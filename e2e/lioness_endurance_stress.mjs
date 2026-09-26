import { chromium, firefox } from 'playwright';
import fs from 'fs';
import path from 'path';

const APP_URL = 'http://localhost:8081';
const SERVER_URL = 'http://localhost:8080';
const LIONESS_PIN = '241889';

// Запуск участника в браузере и переход в комнату «Львица»
async function prepareParticipant(browser, username, password, isHost = false) {
    console.log(`▶️ [${username}] Запуск контекста браузера (${isHost ? 'ХОСТ (Chromium)' : 'ГОСТЬ (Firefox)'})...`);
    const context = await browser.newContext({ viewport: { width: 1440, height: 900 } });

    // Проксирование всех API и стрим-запросов на порт 8080 Ktor сервера
    await context.route('**/api/**', route => {
        const orig = route.request().url();
        const rep = orig.replace(':8081', ':8080');
        if (orig !== rep) {
            route.continue({ url: rep });
        } else {
            route.continue();
        }
    });

    const page = await context.newPage();

    const logs = [];
    page.on('console', msg => {
        const text = msg.text();
        const time = new Date().toISOString().substring(11, 19);
        logs.push(`[${time}] ${text}`);
        if (text.includes('TrueSync') || text.includes('drift') || text.includes('rate=') || 
            text.includes('Seek') || text.includes('PAUSE') || text.includes('PLAY') || 
            text.includes('BUFFER') || text.includes('Anchor') || text.includes('HLS') ||
            text.includes('ReportMediaReady') || text.includes('Preroll') ||
            text.includes('pipeline') || text.includes('ready segments') || text.includes('FRAG_LOADING')) {
            console.log(`[${username} ${time}] ${text}`);
        }
    });

    const waitForMsg = (predicate, timeoutMs = 25000) => new Promise((resolve, reject) => {
        const timer = setTimeout(() => reject(new Error(`[${username}] Timeout waiting for console event`)), timeoutMs);
        const interval = setInterval(() => {
            if (logs.some(predicate)) {
                clearTimeout(timer);
                clearInterval(interval);
                resolve();
            }
        }, 100);
    });

    console.log(`🌐 [${username}] Вход на страницу приложения...`);
    await page.goto(APP_URL, { waitUntil: 'networkidle' });
    await page.waitForTimeout(3000);

    // Авторизация
    console.log(`🔑 [${username}] Ввод учетных данных и адреса сервера (${SERVER_URL})...`);
    await page.mouse.click(600, 337);
    await page.keyboard.type(username);
    await page.waitForTimeout(100);

    await page.mouse.click(600, 410);
    await page.keyboard.type(password);
    await page.waitForTimeout(100);

    // Ввод Server URL в поле ввода (Y=480)
    await page.mouse.click(600, 480);
    await page.keyboard.press('Control+A');
    await page.keyboard.press('Backspace');
    await page.keyboard.type(SERVER_URL);
    await page.waitForTimeout(200);

    // Нажатие кнопки «Войти»
    await page.mouse.click(720, 550);
    await waitForMsg(t => t.includes('streamSidebar collected') || t.includes('Global Manifest Loaded'), 20000);
    console.log(`✅ [${username}] Успешная авторизация!`);
    await page.waitForTimeout(2000);

    // Переход в «Смотрим вместе»
    console.log(`📂 [${username}] Переход в «Смотрим вместе» (60, 481)...`);
    await page.mouse.click(60, 481);
    await waitForMsg(t => t.includes('getUserRooms'), 15000);
    await page.waitForTimeout(2500);

    // Вход в карточку комнаты «Львица» (Card 1)
    console.log(`🎬 [${username}] Вход в комнату «Львица» (1316, 465)...`);
    await page.mouse.click(1316, 465);
    await page.waitForTimeout(3000);
    console.log(`🎉 [${username}] В лобби «Львица»!`);

    return { context, page, logs, username, isHost };
}

// Замер состояния DOM <video>
async function getVideoTelemetry(page) {
    if (!page) return null;
    return page.evaluate(() => {
        const v = document.querySelector('video');
        if (!v) return null;
        let bufEnd = 0;
        let bufStart = 0;
        try {
            if (v.buffered && v.buffered.length > 0) {
                bufStart = v.buffered.start(0);
                bufEnd = v.buffered.end(v.buffered.length - 1);
            }
        } catch (_) {}
        const bufferAhead = Math.max(0, bufEnd - v.currentTime);
        return {
            currentTime: v.currentTime,
            paused: v.paused,
            playbackRate: v.playbackRate,
            bufferedStart: bufStart,
            bufferedEnd: bufEnd,
            bufferAhead: bufferAhead,
            readyState: v.readyState,
            networkState: v.networkState,
            duration: v.duration
        };
    });
}

// Ожидание старта воспроизведения с торрента
async function waitForTorrentStream(page, maxWaitSec = 60) {
    console.log(`⏳ Ожидание первого кадра и буфера торрента (до ${maxWaitSec}с)...`);
    for (let i = 0; i < maxWaitSec; i++) {
        const telem = await getVideoTelemetry(page);
        if (telem && (telem.currentTime > 0.1 || telem.readyState >= 2 || telem.bufferedEnd > 1.0)) {
            console.log(`🎯 Поток торрента успешно подхвачен на секунде ${i}! Телеметрия:`, telem);
            return true;
        }
        await page.waitForTimeout(1000);
    }
    console.warn('⚠️ Таймаут первичного старта торрента, продолжаем с текущим состоянием.');
    return false;
}

// -----------------------------------------------------------------------------
// ПРОДОЛЖИТЕЛЬНЫЙ ЭКСПЕРИМЕНТАЛЬНЫЙ СТРЕСС-ТЕСТ (30 МИНУТ)
// -----------------------------------------------------------------------------

async function runEnduranceTest() {
    console.log('========================================================================');
    console.log('⏱️ ДОЛГОВРЕМЕННЫЙ СТРЕСС-ТЕСТ (30 МИНУТ): БОЕВОЙ СТРИМ И TRUE-SYNC «ЛЬВИЦА»');
    console.log('========================================================================\n');

    const chromeBrowser = await chromium.launch({
        headless: true,
        args: [
            '--no-sandbox',
            '--autoplay-policy=no-user-gesture-required',
            '--disable-web-security'
        ]
    });
    const ffBrowser = await firefox.launch({
        headless: true,
        firefoxUserPrefs: {
            'media.autoplay.default': 0,
            'media.autoplay.blocking_policy': 0
        }
    });

    let host = null;
    let guest = null;

    const projectRoot = path.resolve(path.dirname(new URL(import.meta.url).pathname), '..');
    const logsDir = path.join(projectRoot, 'логи');
    const reportPath = path.join(logsDir, 'endurance_report.json');

    const report = {
        startTime: new Date().toISOString(),
        samples: [],
        events: [],
        issues: []
    };

    try {
        host = await prepareParticipant(chromeBrowser, 'admin', 'admin', true);
        guest = await prepareParticipant(ffBrowser, 'tortuga1', '123456', false);

        console.log('\n🚀 Хост запускает воспроизведение для всей комнаты...');
        await host.page.mouse.click(644, 804); // «▷ Начать просмотр для всех»
        await host.page.waitForTimeout(5000);

        // Ждем старта HLS торрент-потока у хоста
        await waitForTorrentStream(host.page, 50);

        // ---------------------------------------------------------------------
        // ФАЗА 1: Непрерывный естественный просмотр (5 минут)
        // ---------------------------------------------------------------------
        console.log('\n========================================================================');
        console.log('📍 ФАЗА 1: Естественное воспроизведение 5 минут (замеры каждые 10 секунд)');
        console.log('========================================================================');
        
        for (let sec = 0; sec < 300; sec += 10) {
            await host.page.waitForTimeout(10000);
            const h = await getVideoTelemetry(host.page);
            const g = await getVideoTelemetry(guest.page);

            const driftMs = (h && g && !isNaN(h.currentTime) && !isNaN(g.currentTime)) 
                ? Math.abs(h.currentTime - g.currentTime) * 1000 
                : null;

            const sample = {
                phase: 'NaturalPlay_5m',
                elapsedSec: sec + 10,
                host: h,
                guest: g,
                driftMs: driftMs
            };
            report.samples.push(sample);

            console.log(`[ФАЗА 1 - ${sec + 10}с] ` +
                `Хост: ${h?.currentTime?.toFixed(2)}с (buf: ${h?.bufferAhead?.toFixed(1)}с, paused: ${h?.paused}) | ` +
                `Гость: ${g?.currentTime?.toFixed(2)}с (buf: ${g?.bufferAhead?.toFixed(1)}с, rate: ${g?.playbackRate}) | ` +
                `Дрифт: ${driftMs !== null ? driftMs.toFixed(0) + ' мс' : 'N/A'}`);

            if (driftMs && driftMs > 800) {
                const warn = `⚠️ Зафиксирован дрифт > 800мс на ${sec + 10}с: ${driftMs.toFixed(0)} мс`;
                console.warn(warn);
                report.issues.push(warn);
            }
        }

        // ---------------------------------------------------------------------
        // ФАЗА 2: Слабый интернет на стороне гостя (3 минуты троттлинга стрима)
        // ---------------------------------------------------------------------
        console.log('\n========================================================================');
        console.log('📍 ФАЗА 2: Симуляция слабого интернета гостя (задержка чанков 1500мс)');
        console.log('========================================================================');
        
        const throttleRoute = async route => {
            await new Promise(r => setTimeout(r, 1500));
            await route.continue();
        };
        await guest.context.route('**/api/stream-proxy/**', throttleRoute);
        console.log('🐌 Троттлинг сегментов гостя активирован на 3 минуты.');

        for (let sec = 0; sec < 180; sec += 10) {
            await host.page.waitForTimeout(10000);
            const h = await getVideoTelemetry(host.page);
            const g = await getVideoTelemetry(guest.page);
            const driftMs = (h && g) ? Math.abs(h.currentTime - g.currentTime) * 1000 : null;

            console.log(`[ФАЗА 2 - Слабая сеть ${sec + 10}с] ` +
                `Хост: ${h?.currentTime?.toFixed(2)}с (paused: ${h?.paused}) | ` +
                `Гость: ${g?.currentTime?.toFixed(2)}с (paused: ${g?.paused}, buf: ${g?.bufferAhead?.toFixed(1)}с) | ` +
                `Дрифт: ${driftMs !== null ? driftMs.toFixed(0) + ' мс' : 'N/A'}`);
        }

        await guest.context.unroute('**/api/stream-proxy/**', throttleRoute);
        console.log('🚀 Троттлинг снят! Наблюдаем сглаживание и ускорение гостя в течение 60 секунд...');

        for (let sec = 0; sec < 60; sec += 10) {
            await host.page.waitForTimeout(10000);
            const h = await getVideoTelemetry(host.page);
            const g = await getVideoTelemetry(guest.page);
            const driftMs = (h && g) ? Math.abs(h.currentTime - g.currentTime) * 1000 : null;
            console.log(`[ФАЗА 2 - Восстановление ${sec + 10}с] Дрифт: ${driftMs?.toFixed(0)} мс, rate гостя: ${g?.playbackRate}`);
        }

        // ---------------------------------------------------------------------
        // ФАЗА 3: Интерактивная синхронизация (Пауза -> Seek +60s -> Play -> Seek -30s)
        // ---------------------------------------------------------------------
        console.log('\n========================================================================');
        console.log('📍 ФАЗА 3: Серия действий пользователя (Пауза -> Вперед +60с -> Play -> Назад -30с)');
        console.log('========================================================================');

        console.log('⏸️ Хост ставит воспроизведение на паузу...');
        await host.page.evaluate(() => {
            const v = document.querySelector('video');
            if (v) v.pause();
        });
        await host.page.waitForTimeout(3000);
        let h = await getVideoTelemetry(host.page);
        let g = await getVideoTelemetry(guest.page);
        console.log(`Пауза зафиксирована: Хост paused=${h?.paused}, Гость paused=${g?.paused}`);

        console.log('⏩ Хост перематывает на +60 секунд вперед...');
        await host.page.evaluate(() => {
            const v = document.querySelector('video');
            if (v) v.currentTime += 60;
        });
        await host.page.waitForTimeout(5000);
        h = await getVideoTelemetry(host.page);
        g = await getVideoTelemetry(guest.page);
        console.log(`После Seek(+60s): Хост pos=${h?.currentTime?.toFixed(1)}, Гость pos=${g?.currentTime?.toFixed(1)}`);

        console.log('▶️ Хост возобновляет воспроизведение...');
        await host.page.evaluate(() => {
            const v = document.querySelector('video');
            if (v) v.play().catch(() => {});
        });
        await host.page.waitForTimeout(5000);

        console.log('⏪ Хост перематывает на -30 секунд назад...');
        await host.page.evaluate(() => {
            const v = document.querySelector('video');
            if (v) v.currentTime = Math.max(0, v.currentTime - 30);
        });
        await host.page.waitForTimeout(5000);
        h = await getVideoTelemetry(host.page);
        g = await getVideoTelemetry(guest.page);
        console.log(`После Seek(-30s): Хост pos=${h?.currentTime?.toFixed(1)}, Гость pos=${g?.currentTime?.toFixed(1)}`);

        // ---------------------------------------------------------------------
        // ФАЗА 4: Дисконнект гостя и повторное подключение посреди фильма
        // ---------------------------------------------------------------------
        console.log('\n========================================================================');
        console.log('📍 ФАЗА 4: Аварийный дисконнект и повторный вход гостя');
        console.log('========================================================================');

        const previousGuestLogs = [...guest.logs];
        console.log('🔌 Закрываем вкладку гостя (Firefox)...');
        await guest.page.close();
        await guest.context.close();
        console.log('⏳ Хост продолжает воспроизведение один в течение 30 секунд...');
        await host.page.waitForTimeout(30000);
        h = await getVideoTelemetry(host.page);
        console.log(`Хост один на позиции: ${h?.currentTime?.toFixed(1)}с`);

        console.log('🔄 Гость подключается заново в комнату «Львица»...');
        guest = await prepareParticipant(ffBrowser, 'tortuga1', '123456', false);
        guest.logs.unshift(...previousGuestLogs);
        await guest.page.waitForTimeout(4000);
        await waitForTorrentStream(guest.page, 40);

        h = await getVideoTelemetry(host.page);
        g = await getVideoTelemetry(guest.page);
        console.log('Телеметрия после горячего реконнекта:');
        console.log(`Хост: ${h?.currentTime?.toFixed(1)}с, paused=${h?.paused}`);
        console.log(`Гость: ${g?.currentTime?.toFixed(1)}с, paused=${g?.paused}`);

        // ---------------------------------------------------------------------
        // ФАЗА 5: Длительный финальный просмотр на стабильность (15 минут)
        // ---------------------------------------------------------------------
        console.log('\n========================================================================');
        console.log('📍 ФАЗА 5: Длительный финишный просмотр (15 минут, замеры каждые 15 секунд)');
        console.log('========================================================================');

        for (let sec = 0; sec < 900; sec += 15) {
            await host.page.waitForTimeout(15000);
            h = await getVideoTelemetry(host.page);
            g = await getVideoTelemetry(guest.page);
            const driftMs = (h && g && !isNaN(h.currentTime) && !isNaN(g.currentTime)) 
                ? Math.abs(h.currentTime - g.currentTime) * 1000 
                : null;

            console.log(`[ФАЗА 5 - Финал ${(sec + 15) / 60} мин] ` +
                `Хост: ${h?.currentTime?.toFixed(1)}с (buf: ${h?.bufferAhead?.toFixed(1)}с) | ` +
                `Гость: ${g?.currentTime?.toFixed(1)}с (buf: ${g?.bufferAhead?.toFixed(1)}с) | ` +
                `Дрифт: ${driftMs !== null ? driftMs.toFixed(0) + ' мс' : 'N/A'}`);
        }

        report.endTime = new Date().toISOString();
        console.log('\n========================================================================');
        console.log('🏆 ДОЛГОВРЕМЕННЫЙ 30-МИНУТНЫЙ ТЕСТ УСПЕШНО ЗАВЕРШЕН!');
        console.log('========================================================================');

    } catch (err) {
        console.error('❌ Ошибка во время долговременного теста:', err);
        report.error = err.message;
    } finally {
        fs.writeFileSync(reportPath, JSON.stringify(report, null, 2));
        if (host) fs.writeFileSync(path.join(logsDir, 'endurance_host_logs.txt'), host.logs.join('\n'));
        if (guest) fs.writeFileSync(path.join(logsDir, 'endurance_guest_logs.txt'), guest.logs.join('\n'));
        console.log(`📁 Полный отчет сохранен в ${reportPath}`);

        await chromeBrowser.close();
        await ffBrowser.close();
    }
}

runEnduranceTest().catch(console.error);
