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
        if (text.includes('TrueSync') || text.includes('drift') || text.includes('rate=') || text.includes('Seek') || text.includes('PAUSE') || text.includes('PLAY')) {
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

    const projectRoot = path.resolve(path.dirname(new URL(import.meta.url).pathname), '..');
    const screenshotPath = path.join(projectRoot, 'логи', `${username}_in_room.png`);
    await page.screenshot({ path: screenshotPath });
    console.log(`📸 [${username}] Скриншот сохранен: ${screenshotPath}`);

    return { page, logs };
}

async function main() {
    console.log('========================================================================');
    console.log('🚀 ПОЛНЫЙ ПАРАЛЛЕЛЬНЫЙ E2E СТРЕСС-ТЕСТ: CHROMIUM + FIREFOX + 10 СЦЕНАРИЕВ');
    console.log('========================================================================\n');

    const chromeBrowser = await chromium.launch({ headless: true });
    const ffBrowser = await firefox.launch({ headless: true });

    try {
        const chrome = await prepareParticipant(chromeBrowser, 'admin', 'admin');
        const ff = await prepareParticipant(ffBrowser, 'tortuga1', '123456');

        console.log('\nОба браузера в лобби комнаты. Запускаем стресс-тест в фоне и начинаем телеметрию...\n');

        const checkVideo = async (p) => p.evaluate(() => {
            const v = document.querySelector('video');
            return v ? {
                currentTime: v.currentTime,
                paused: v.paused,
                rate: v.playbackRate,
                buffered: v.buffered.length > 0 ? v.buffered.end(v.buffered.length - 1) : 0
            } : null;
        });

        const projectRoot = path.resolve(path.dirname(new URL(import.meta.url).pathname), '..');

        // Запуск Gradle тестов в фоновом процессе
        const testProc = spawn('./gradlew', [':client:jvmTest', '--rerun', '--tests', 'org.ensodai.avalonmediacard.TrueSyncStressTestSuite'], {
            cwd: projectRoot,
            stdio: 'pipe'
        });

        testProc.stdout.on('data', (d) => {
            const str = d.toString();
            const lines = str.split('\n');
            for (const line of lines) {
                const trimmed = line.trim();
                if (trimmed.includes('▶️ Running') || trimmed.includes('PASSED') || trimmed.includes('FAILED') || trimmed.includes('TRUESYNC') || trimmed.includes('Result:') || trimmed.startsWith('|')) {
                    console.log(`📌 [TEST SUITE] ${trimmed}`);
                }
            }
        });

        // Мониторинг состояния плееров каждую 1 секунду пока идет стресс-тест
        const metricsHistory = [];
        let tick = 0;
        const intervalId = setInterval(async () => {
            tick++;
            try {
                const cState = await checkVideo(chrome.page);
                const fState = await checkVideo(ff.page);
                if (cState && fState) {
                    const deltaMs = (cState.currentTime - fState.currentTime) * 1000;
                    metricsHistory.push({ tick, cState, fState, deltaMs });
                    console.log(`⏱️ [${tick}s] Chrome: ${cState.currentTime.toFixed(3)}s (paused=${cState.paused}, ${cState.rate}x) | FF: ${fState.currentTime.toFixed(3)}s (paused=${fState.paused}, ${fState.rate}x) | 🎯 ДЕЛЬТА: ${deltaMs.toFixed(1)}ms`);
                } else if (cState || fState) {
                    console.log(`⏱️ [${tick}s] Chrome: ${cState ? cState.currentTime.toFixed(2) + 's' : 'null'} | FF: ${fState ? fState.currentTime.toFixed(2) + 's' : 'null'}`);
                }
            } catch (_) {}
        }, 1000);

        // Ждем завершения тестового процесса
        await new Promise((resolve) => {
            testProc.on('close', (code) => {
                clearInterval(intervalId);
                console.log(`\n🏁 Тестовый процесс завершился с кодом: ${code}`);
                resolve();
            });
        });

        // Сохранение логов консоли браузеров
        const logsDir = path.join(projectRoot, 'логи');
        fs.writeFileSync(path.join(logsDir, 'e2e_playwright_chrome.txt'), chrome.logs.join('\n'));
        fs.writeFileSync(path.join(logsDir, 'e2e_playwright_firefox.txt'), ff.logs.join('\n'));

        // Сводный отчет
        console.log('\n========================================================================');
        console.log('📊 ИТОГОВАЯ ТЕЛЕМЕТРИЯ СИНХРОНИЗАЦИИ МЕЖДУ CHROMIUM И FIREFOX');
        console.log('========================================================================');
        let telemetryMd = `# Отчет телеметрии синхронизации видеоплееров (TrueSync)\n\n`;
        telemetryMd += `- **Chromium**: аккаунт \`admin\` (Хост)\n`;
        telemetryMd += `- **Firefox**: аккаунт \`tortuga1\` (Зритель)\n`;
        telemetryMd += `- **Комната**: Смотрим хищника (\`51ecc142-499e-463a-aec1-7851bbdfa7c2\`)\n\n`;

        if (metricsHistory.length > 0) {
            const deltas = metricsHistory.map(m => Math.abs(m.deltaMs));
            const maxDelta = Math.max(...deltas);
            const avgDelta = deltas.reduce((a, b) => a + b, 0) / deltas.length;
            console.log(`Всего замеров: ${metricsHistory.length}`);
            console.log(`Средний рассинхрон: ${avgDelta.toFixed(1)} мс`);
            console.log(`Максимальный рассинхрон: ${maxDelta.toFixed(1)} мс`);

            telemetryMd += `### Статистика синхронизации\n`;
            telemetryMd += `- Всего замеров: **${metricsHistory.length}**\n`;
            telemetryMd += `- Средний рассинхрон: **${avgDelta.toFixed(1)} мс**\n`;
            telemetryMd += `- Максимальный рассинхрон: **${maxDelta.toFixed(1)} мс**\n\n`;

            telemetryMd += `| Время (с) | Chrome (сек) | Chrome Статус | Firefox (сек) | Firefox Статус | Дельта (мс) |\n`;
            telemetryMd += `|:---------:|:------------:|:-------------:|:-------------:|:--------------:|:-----------:|\n`;
            for (const m of metricsHistory) {
                const cPaused = m.cState.paused ? 'PAUSED' : 'PLAY';
                const fPaused = m.fState.paused ? 'PAUSED' : 'PLAY';
                telemetryMd += `| ${m.tick}s | ${m.cState.currentTime.toFixed(3)}s | ${cPaused} (${m.cState.rate}x) | ${m.fState.currentTime.toFixed(3)}s | ${fPaused} (${m.fState.rate}x) | ${m.deltaMs.toFixed(1)}ms |\n`;
            }
        } else {
            console.log('Замеры с тегов <video> зафиксированы в логах консоли.');
            telemetryMd += `Замеры с тегов video зафиксированы в логах консоли.\n`;
        }

        fs.writeFileSync(path.join(logsDir, 'E2E_Video_Sync_Telemetry.md'), telemetryMd);
        console.log('\n💾 Все детальные логи браузеров сохранены:');
        console.log(' - логи/e2e_playwright_chrome.txt');
        console.log(' - логи/e2e_playwright_firefox.txt');
        console.log(' - логи/E2E_Video_Sync_Telemetry.md');

    } finally {
        await chromeBrowser.close();
        await ffBrowser.close();
    }
}

main().catch(err => {
    console.error('Ошибка в тесте:', err);
    process.exit(1);
});
