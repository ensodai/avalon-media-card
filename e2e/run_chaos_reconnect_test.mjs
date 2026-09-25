import { chromium, firefox } from 'playwright';
import { spawn } from 'child_process';
import fs from 'fs';
import path from 'path';

const APP_URL = 'http://localhost:8081';

function setupWebSocketRedirect(page) {
    return page.addInitScript(() => {
        const OrigWebSocket = window.WebSocket;
        window.WebSocket = function(url, protocols) {
            if (url && url.includes(':8081/api/rpc')) {
                url = url.replace(':8081/api/rpc', ':8080/api/rpc');
            }
            return new OrigWebSocket(url, protocols);
        };
    });
}

async function prepareParticipant(browser, username, password) {
    console.log(`▶️ [${username}] Вход в лобби...`);
    const page = await browser.newPage({ viewport: { width: 1280, height: 720 } });
    await setupWebSocketRedirect(page);

    const logs = [];
    page.on('console', msg => {
        const text = msg.text();
        logs.push(`[${new Date().toISOString()}] ${text}`);
        if (text.includes('TrueSync') || text.includes('drift') || text.includes('rate=') || text.includes('Seek') || text.includes('PAUSE') || text.includes('PLAY') || text.includes('RPC') || text.includes('Hard Seek')) {
            console.log(`[${username} LOG] ${text}`);
        }
    });

    const waitForMsg = (predicate, timeoutMs = 20000) => new Promise((resolve, reject) => {
        const timer = setTimeout(() => reject(new Error(`[${username}] Timeout waiting for console event`)), timeoutMs);
        const interval = setInterval(() => {
            if (logs.some(predicate)) {
                clearTimeout(timer);
                clearInterval(interval);
                resolve();
            }
        }, 100);
    });

    await page.goto(APP_URL, { waitUntil: 'networkidle' });
    await page.waitForTimeout(3000);

    // Авторизация
    console.log(`🔑 [${username}] Ввод логина и пароля...`);
    await page.mouse.click(520, 247);
    await page.keyboard.press('Control+A');
    await page.keyboard.press('Backspace');
    await page.keyboard.type(username);
    await page.waitForTimeout(150);

    await page.mouse.click(520, 320);
    await page.keyboard.press('Control+A');
    await page.keyboard.press('Backspace');
    await page.keyboard.type(password);
    await page.waitForTimeout(150);

    console.log(`🚪 [${username}] Отправка формы логина...`);
    await page.mouse.click(640, 460); // Войти
    await waitForMsg(t => t.includes('streamSidebar collected') || t.includes('Global Manifest Loaded'), 25000);
    console.log(`✅ [${username}] Успешный вход в аккаунт!`);
    await page.waitForTimeout(2500);

    // Переход в "Смотрим вместе" (x: 38, y: 481)
    console.log(`📂 [${username}] Переход в раздел "Смотрим вместе"...`);
    await page.mouse.click(38, 481);
    await waitForMsg(t => t.includes('getUserRooms'), 15000);
    console.log(`📋 [${username}] Список комнат загружен.`);
    await page.waitForTimeout(2500);

    // Вход в комнату "Смотрим хищника" (x: 1160, y: 459)
    console.log(`🎬 [${username}] Вход в комнату "Смотрим хищника"...`);
    await page.mouse.click(1160, 459);
    await waitForMsg(t => t.includes('joinRoomById'), 15000);
    console.log(`🎉 [${username}] Успешно присоединился к комнате "Смотрим хищника"!`);
    await page.waitForTimeout(2500);

    return { page, logs };
}

async function main() {
    console.log('========================================================================');
    console.log('🔥 СТРЕСС-ТЕСТ СЕТЕВОГО ХАОСА И ГОРЯЧЕГО РЕКОННЕКТА (E2E CHAOS TEST)');
    console.log('========================================================================\n');

    const projectRoot = path.resolve(path.dirname(new URL(import.meta.url).pathname), '..');
    const logsDir = path.join(projectRoot, 'логи');
    if (!fs.existsSync(logsDir)) fs.mkdirSync(logsDir, { recursive: true });

    // Очистка старых сигналов
    const signalFiles = ['signal_offline.req', 'signal_offline.ack', 'signal_reconnect.req', 'signal_reconnect.ack'];
    for (const f of signalFiles) {
        const p = path.join(logsDir, f);
        if (fs.existsSync(p)) fs.unlinkSync(p);
    }

    const chromeBrowser = await chromium.launch({ headless: true });
    const ffBrowser = await firefox.launch({ headless: true });

    try {
        const chrome = await prepareParticipant(chromeBrowser, 'admin', 'admin');
        const ff = await prepareParticipant(ffBrowser, 'tortuga1', '123456');

        console.log('\n✅ Оба участника в комнате. Запуск сценария хаоса в фоне...\n');

        const checkVideo = async (p) => p.evaluate(() => {
            const v = document.querySelector('video');
            return v ? {
                currentTime: v.currentTime,
                paused: v.paused,
                rate: v.playbackRate,
                buffered: v.buffered.length > 0 ? v.buffered.end(v.buffered.length - 1) : 0
            } : null;
        });

        // Запуск тестового раннера в JVM
        const testProc = spawn('./gradlew', [':client:jvmTest', '--rerun', '--tests', 'org.ensodai.avalonmediacard.ChaosReconnectTestSuite'], {
            cwd: projectRoot,
            stdio: 'pipe'
        });

        testProc.stdout.on('data', (d) => {
            const str = d.toString();
            const lines = str.split('\n');
            for (const line of lines) {
                const trimmed = line.trim();
                if (trimmed.includes('STARTING') || trimmed.includes('Step') || trimmed.includes('PASSED') || trimmed.includes('FAILED') || trimmed.includes('finished')) {
                    console.log(`📌 [CHAOS SUITE] ${trimmed}`);
                }
            }
        });

        const metricsHistory = [];
        let currentPhase = '1. INITIAL PLAYBACK';
        let tick = 0;

        // Быстрый опрос сигналов раз в 100 мс
        const signalInterval = setInterval(async () => {
            const offlineReq = path.join(logsDir, 'signal_offline.req');
            if (fs.existsSync(offlineReq)) {
                try {
                    fs.unlinkSync(offlineReq);
                    currentPhase = '2. OFFLINE DROP (ISOLATION)';
                    console.log('\n💥💥💥 [CHAOS TRIGGER] DISCONNECTING FIREFOX NETWORK (OFFLINE)! 💥💥💥\n');
                    await ff.page.context().setOffline(true);
                    fs.writeFileSync(path.join(logsDir, 'signal_offline.ack'), 'ACK');
                    await ff.page.screenshot({ path: path.join(logsDir, 'chaos_during_offline_ff.png') });
                } catch (e) {
                    console.error('Error triggering offline:', e);
                }
            }

            const reconnectReq = path.join(logsDir, 'signal_reconnect.req');
            if (fs.existsSync(reconnectReq)) {
                try {
                    fs.unlinkSync(reconnectReq);
                    currentPhase = '3. HOT RECONNECT & RECOVERY';
                    console.log('\n🌐🌐🌐 [CHAOS TRIGGER] RESTORING FIREFOX NETWORK (RECONNECT)! 🌐🌐🌐\n');
                    await ff.page.context().setOffline(false);
                    fs.writeFileSync(path.join(logsDir, 'signal_reconnect.ack'), 'ACK');
                } catch (e) {
                    console.error('Error triggering reconnect:', e);
                }
            }
        }, 100);

        // Цикл мониторинга телеметрии каждую 1 секунду
        const telemetryInterval = setInterval(async () => {
            tick++;
            try {
                const cState = await checkVideo(chrome.page);
                const fState = await checkVideo(ff.page);
                if (cState && fState) {
                    const deltaMs = (cState.currentTime - fState.currentTime) * 1000;
                    metricsHistory.push({ tick, phase: currentPhase, cState, fState, deltaMs });
                    console.log(`⏱️ [${tick}s | ${currentPhase}] Chrome: ${cState.currentTime.toFixed(2)}s | FF: ${fState.currentTime.toFixed(2)}s | 🎯 ДЕЛЬТА: ${deltaMs.toFixed(1)}ms`);
                } else if (cState || fState) {
                    console.log(`⏱️ [${tick}s | ${currentPhase}] Chrome: ${cState ? cState.currentTime.toFixed(2) + 's' : 'null'} | FF: ${fState ? fState.currentTime.toFixed(2) + 's' : 'null'}`);
                }
            } catch (_) {}
        }, 1000);

        // Ждем завершения теста
        await new Promise((resolve) => {
            testProc.on('close', (code) => {
                clearInterval(signalInterval);
                clearInterval(telemetryInterval);
                console.log(`\n🏁 Chaos test процесс завершился с кодом: ${code}`);
                resolve();
            });
        });

        // Скриншоты после завершения
        await chrome.page.screenshot({ path: path.join(logsDir, 'chaos_chrome_final.png') });
        await ff.page.screenshot({ path: path.join(logsDir, 'chaos_firefox_final.png') });

        // Сохранение логов консоли
        fs.writeFileSync(path.join(logsDir, 'chaos_chrome_console.txt'), chrome.logs.join('\n'));
        fs.writeFileSync(path.join(logsDir, 'chaos_firefox_console.txt'), ff.logs.join('\n'));

        // Формирование markdown-отчета телеметрии
        console.log('\n========================================================================');
        console.log('📊 ИТОГОВЫЙ ОТЧЕТ СТРЕСС-ТЕСТА СЕТЕВОГО ХАОСА И РЕКОННЕКТА');
        console.log('========================================================================');

        let reportMd = `# Отчет стресс-теста сетевого хаоса и горячего реконнекта (TrueSync Chaos Test)\n\n`;
        reportMd += `- **Хост**: Chromium (\`admin\`)\n`;
        reportMd += `- **Зритель**: Firefox (\`tortuga1\`)\n`;
        reportMd += `- **Комната**: Смотрим хищника (\`51ecc142-499e-463a-aec1-7851bbdfa7c2\`)\n`;
        reportMd += `- **Дата запуска**: ${new Date().toISOString()}\n\n`;

        reportMd += `### Сценарий теста:\n`;
        reportMd += `1. **Синхронный старт**: запуск видеопотока в обоих браузерах, калибровка SNTP.\n`;
        reportMd += `2. **Сетевой блэкаут (Offline Drop)**: полный обрыв сети у Firefox (\`context.setOffline(true)\`).\n`;
        reportMd += `3. **Действия хоста в изоляции**: Хост продолжает воспроизведение и совершает Seek на +35 секунд.\n`;
        reportMd += `4. **Горячий реконнект (Hot Reconnect)**: восстановление сети у Firefox (\`context.setOffline(false)\`), повторный захват потока событий комнаты, SNTP-калибровка и мгновенный догон хоста через Hard Seek.\n`;
        reportMd += `5. **Стабилизация**: возврат рассинхрона в рабочий коридор без заиканий.\n\n`;

        if (metricsHistory.length > 0) {
            reportMd += `### Хронологическая телеметрия воспроизведения:\n\n`;
            reportMd += `| Время (с) | Фаза теста | Chrome (сек) | Firefox (сек) | Статус Chrome | Статус FF | Рассинхрон (мс) |\n`;
            reportMd += `|:---------:|:----------:|:------------:|:-------------:|:-------------:|:---------:|:---------------:|\n`;
            for (const m of metricsHistory) {
                const cPaused = m.cState.paused ? 'PAUSED' : 'PLAY';
                const fPaused = m.fState.paused ? 'PAUSED' : 'PLAY';
                reportMd += `| ${m.tick}s | ${m.phase} | ${m.cState.currentTime.toFixed(3)}s | ${m.fState.currentTime.toFixed(3)}s | ${cPaused} (${m.cState.rate}x) | ${fPaused} (${m.fState.rate}x) | ${m.deltaMs.toFixed(1)}ms |\n`;
            }

            const recoveryEntries = metricsHistory.filter(m => m.phase.includes('HOT RECONNECT'));
            if (recoveryEntries.length > 0) {
                const lastRecovery = recoveryEntries[recoveryEntries.length - 1];
                reportMd += `\n### Результат восстановления:\n`;
                reportMd += `- Итоговый рассинхрон после реконнекта: **${Math.abs(lastRecovery.deltaMs).toFixed(1)} мс**\n`;
                reportMd += `- Успешность догона: **${Math.abs(lastRecovery.deltaMs) < 150 ? 'УСПЕШНО (В коридоре нормы)' : 'РАССИНХРОН'}**\n`;
            }
        }

        fs.writeFileSync(path.join(logsDir, 'Chaos_Reconnect_Telemetry.md'), reportMd);
        console.log('\n💾 Отчет сохранен в логи/Chaos_Reconnect_Telemetry.md');

    } finally {
        await chromeBrowser.close();
        await ffBrowser.close();
    }
}

main().catch(err => {
    console.error('Ошибка в тесте хаоса:', err);
    process.exit(1);
});
