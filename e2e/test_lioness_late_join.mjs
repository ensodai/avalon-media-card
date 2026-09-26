import { chromium, firefox } from 'playwright';

const APP_URL = 'http://localhost:8081';
const SERVER_URL = 'http://localhost:8080';

async function prepareParticipant(browser, username, password, isHost = false) {
    console.log(`▶️ [${username}] Запуск контекста браузера (${isHost ? 'ХОСТ (Chromium)' : 'ГОСТЬ (Firefox)'})...`);
    const context = await browser.newContext({ viewport: { width: 1440, height: 900 } });

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
            text.includes('ReportMediaReady') || text.includes('ReportBuffer') ||
            text.includes('Preroll') || text.includes('pipeline') || text.includes('ready segments') ||
            text.includes('sn=') || text.includes('stalled') || text.includes('FRAG') ||
            text.includes('SourceBuffer') || text.includes('canplay') ||
            text.includes('getUserRooms') || text.includes('RpcCallExecutor')) {
            console.log(`[${username} ${time}] ${text}`);
        }
    });

    const waitForMsg = (predicate, timeoutMs = 45000, startIdx = 0) => new Promise((resolve, reject) => {
        const timer = setTimeout(() => reject(new Error(`[${username}] Timeout waiting for console event`)), timeoutMs);
        const interval = setInterval(() => {
            if (logs.slice(startIdx).some(predicate)) {
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
    const loginClickIdx = logs.length;
    await page.mouse.click(720, 550);
    await waitForMsg(t => t.includes('Global Manifest Loaded'), 45000, loginClickIdx);
    console.log(`✅ [${username}] Успешная авторизация!`);
    await page.waitForTimeout(2500);

    // Переход в «Смотрим вместе»
    console.log(`📂 [${username}] Переход в «Смотрим вместе» (60, 481)...`);
    const roomsClickIdx = logs.length;
    await page.mouse.click(60, 481);
    try {
        await waitForMsg(t => t.includes('getUserRooms'), 8000, roomsClickIdx);
    } catch (_) {
        console.log(`📂 [${username}] Повторный клик в «Смотрим вместе»...`);
        const retryIdx = logs.length;
        await page.mouse.click(60, 481);
        await waitForMsg(t => t.includes('getUserRooms'), 30000, retryIdx);
    }
    await page.waitForTimeout(2500);

    // Вход в карточку комнаты «Львица» (Card 1)
    console.log(`🎬 [${username}] Вход в комнату «Львица» (1316, 465)...`);
    await page.mouse.click(1316, 465);
    await page.waitForTimeout(3000);
    console.log(`🎉 [${username}] В комнате «Львица»!`);

    return { context, page, logs, username, isHost };
}

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

async function waitForTorrentStream(page, maxWaitSec = 60) {
    console.log(`⏳ Ожидание активного воспроизведения торрента (до ${maxWaitSec}с)...`);
    let lastPos = -1;
    let advancementCount = 0;
    for (let i = 0; i < maxWaitSec; i++) {
        const telem = await getVideoTelemetry(page);
        if (telem && !telem.paused && telem.readyState >= 2 && telem.currentTime > 0) {
            if (lastPos >= 0 && telem.currentTime > lastPos) {
                advancementCount++;
                if (advancementCount >= 2) {
                    console.log(`🎯 Поток торрента активно играет на секунде ${i}! Позиция: ${telem.currentTime.toFixed(1)}s, буфер: ${telem.bufferAhead.toFixed(1)}s`);
                    return true;
                }
            }
            lastPos = telem.currentTime;
        }
        await page.waitForTimeout(1000);
    }
    return false;
}

async function waitForBothSyncPlaying(hostPage, guestPage, maxWaitSec = 130) {
    console.log(`⏳ Ожидание синхронного воспроизведения обоих участников (до ${maxWaitSec}с)...`);
    for (let i = 0; i < maxWaitSec; i++) {
        const h = await getVideoTelemetry(hostPage);
        const g = await getVideoTelemetry(guestPage);
        if (h && g && !h.paused && !g.paused && h.currentTime > 0.5 && g.currentTime > 0.5) {
            const driftMs = Math.abs(h.currentTime - g.currentTime) * 1000;
            if (driftMs < 500) {
                console.log(`🎯 Синхронное воспроизведение достигнуто! Host=${h.currentTime.toFixed(2)}s, Guest=${g.currentTime.toFixed(2)}s (дрифт = ${driftMs.toFixed(0)} мс)`);
                return { success: true, driftMs, h, g };
            }
        }
        if (i % 5 === 0 && h && g) {
            console.log(`📊 [Прогресс ${i}с] Host: pos=${h.currentTime.toFixed(1)}s, buf=${h.bufferedEnd.toFixed(1)}s, ready=${h.readyState}, paused=${h.paused} | Guest: pos=${g.currentTime.toFixed(1)}s, buf=${g.bufferedEnd.toFixed(1)}s, ready=${g.readyState}, paused=${g.paused}`);
        }
        await hostPage.waitForTimeout(1000);
    }
    const h = await getVideoTelemetry(hostPage);
    const g = await getVideoTelemetry(guestPage);
    return { success: false, h, g };
}

async function runIsolatedTest1() {
    console.log('========================================================================');
    console.log('💥 ИЗОЛИРОВАННЫЙ ТЕСТ 1: Поздний гость (Late Join) в комнату «Львица»');
    console.log('   Сценарий: Хост играет -> Подключается Firefox -> Синхронизация БЕЗ КЛИКОВ');
    console.log('========================================================================\n');

    const chromeBrowser = await chromium.launch({
        headless: true,
        args: [
            '--no-sandbox',
            '--disable-gpu',
            '--use-gl=swiftshader',
            '--disable-dev-shm-usage',
            '--disable-accelerated-video-decode',
            '--disable-accelerated-video-encode',
            '--autoplay-policy=no-user-gesture-required',
            '--disable-web-security'
        ]
    });
    const ffBrowser = await firefox.launch({
        headless: true,
        firefoxUserPrefs: {
            'media.autoplay.default': 0,
            'media.autoplay.blocking_policy': 0,
            'layers.acceleration.disabled': true,
            'media.hardware-video-decoding.enabled': false,
            'gfx.webrender.all': false,
            'gfx.webrender.software': true
        }
    });

    let host = null;
    let guest = null;

    try {
        host = await prepareParticipant(chromeBrowser, 'admin', 'admin', true);

        const initialTelem = await getVideoTelemetry(host.page);
        if (!initialTelem) {
            console.log('▶️ Хост в лобби. Кликает «▷ Начать просмотр для всех» (644, 804)...');
            await host.page.mouse.click(644, 804);
        } else {
            console.log(`▶️ Плеер хоста уже открыт на позиции ${initialTelem.currentTime.toFixed(1)}s (paused=${initialTelem.paused})`);
            if (initialTelem.paused) {
                console.log('▶️ Плеер хоста на паузе, отправляем клик по центру для снятия с паузы...');
                await host.page.mouse.click(720, 450);
            }
        }
        const hostStarted = await waitForTorrentStream(host.page, 55);
        if (!hostStarted) {
            throw new Error('Хост не смог начать воспроизведение в течение 55 секунд!');
        }

        console.log('⏱️ Хост спокойно играет 10 секунд...');
        await host.page.waitForTimeout(10000);
        const hostBeforeJoin = await getVideoTelemetry(host.page);
        console.log(`Хост перед подключением гостя: ${hostBeforeJoin.currentTime.toFixed(1)}s (paused=${hostBeforeJoin.paused})`);

        console.log('🚪 ГОСТЬ (Firefox) подключается в комнату с живым потоком...');
        guest = await prepareParticipant(ffBrowser, 'tortuga1', '123456', false);

        console.log('⏳ Проверка: комната должна САМА подхватить гостя и играть синхронно...');
        const syncRes = await waitForBothSyncPlaying(host.page, guest.page, 130);

        if (syncRes.success) {
            console.log(`\n🎉 ТЕСТ 1 УСПЕШНО ПРОЙДЕН! Оба участника играют синхронно (дрифт ${syncRes.driftMs.toFixed(0)} мс)! Дедлок полностью устранен!`);
            process.exit(0);
        } else {
            console.error('\n❌ ТЕСТ 1 ПРОВАЛЕН! Телеметрия:', JSON.stringify(syncRes, null, 2));
            process.exit(1);
        }
    } catch (err) {
        console.error('💥 Ошибка в тесте:', err);
        process.exit(1);
    } finally {
        if (host) await host.context.close().catch(() => {});
        if (guest) await guest.context.close().catch(() => {});
        await chromeBrowser.close().catch(() => {});
        await ffBrowser.close().catch(() => {});
    }
}

runIsolatedTest1();
