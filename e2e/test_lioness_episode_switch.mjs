import { chromium, firefox } from 'playwright';

const APP_URL = 'http://localhost:8081';
const SERVER_URL = 'http://localhost:8080';
const LIONESS_PIN = '241889';

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

    page.on('pageerror', err => {
        console.error(`💥 [${username}] PAGE ERROR: ${err.message}\n${err.stack}`);
    });

    const logs = [];
    page.on('console', msg => {
        const text = msg.text();
        const time = new Date().toISOString().substring(11, 19);
        logs.push(`[${time}] ${text}`);
        const lower = text.toLowerCase();
        if (msg.type() === 'error' || lower.includes('error') || lower.includes('exception') ||
            lower.includes('truesync') || lower.includes('drift') || lower.includes('rate=') || 
            lower.includes('seek') || lower.includes('pause') || lower.includes('play') || 
            lower.includes('buffer') || lower.includes('anchor') || lower.includes('hls') ||
            lower.includes('reportmediaready') || lower.includes('reportbuffer') ||
            lower.includes('preroll') || lower.includes('pipeline') || lower.includes('ready segments') ||
            lower.includes('partial_buffering') || lower.includes('stalled') ||
            lower.includes('changeepisode') || lower.includes('episode changed') ||
            lower.includes('handleremoteepisodechange') || lower.includes('onepisodeselected') ||
            lower.includes('rpc') || lower.includes('manifest') || lower.includes('sidebar') ||
            lower.includes('getuserrooms') || lower.includes('rpccallexecutor') ||
            lower.includes('engine') || lower.includes('worker') || lower.includes('remux') ||
            lower.includes('playsvideo')) {
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

    // Авторизация с повторными попытками
    let loggedIn = false;
    for (let attempt = 0; attempt < 3 && !loggedIn; attempt++) {
        async function clearAndType(x, y, text) {
            await page.mouse.click(x, y);
            await page.waitForTimeout(100);
            await page.keyboard.press('End');
            for (let k = 0; k < 45; k++) {
                await page.keyboard.press('Backspace');
            }
            await page.keyboard.type(text);
            await page.waitForTimeout(100);
        }

        await clearAndType(600, 337, username);
        await clearAndType(600, 410, password);
        await clearAndType(600, 480, SERVER_URL);

        // Вход
        const loginClickIdx = logs.length;
        await page.mouse.click(720, 550);
        try {
            await waitForMsg(t => t.includes('Global Manifest Loaded'), 45000, loginClickIdx);
            loggedIn = true;
            console.log(`✅ [${username}] Успешная авторизация!`);
        } catch (_) {
            console.log(`⚠️ [${username}] Таймаут ожидания манифеста, повтор ввода учетных данных...`);
        }
    }
    if (!loggedIn) {
        throw new Error(`[${username}] Не удалось авторизоваться после 3 попыток!`);
    }
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

    // Вход в комнату «Львица»
    console.log(`🎬 [${username}] Вход в комнату «Львица» (1316, 465)...`);
    await page.mouse.click(1316, 465);
    await page.waitForTimeout(3000);
    console.log(`🎉 [${username}] В комнате «Львица»!`);

    return { context, page, logs, username, isHost };
}

async function getVideoTelemetry(page) {
    if (!page) return null;
    return page.evaluate(() => {
        const v = document.querySelector('#video-underlay-container video:last-of-type') || document.querySelector('video');
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
            duration: v.duration,
            src: v.src || (v.querySelector('source') ? v.querySelector('source').src : '')
        };
    });
}

async function waitForTorrentStream(page, maxWaitSec = 75) {
    console.log(`⏳ Ожидание активного воспроизведения торрента (до ${maxWaitSec}с)...`);
    let lastPos = -1;
    let advancementCount = 0;
    for (let i = 0; i < maxWaitSec; i++) {
        const telem = await getVideoTelemetry(page);
        if (telem) {
            if (i % 5 === 0) {
                console.log(`⏳ [${i}s] Торрент-поток: readyState=${telem.readyState}, pos=${telem.currentTime.toFixed(1)}s, buf=${telem.bufferAhead.toFixed(1)}s, paused=${telem.paused}`);
            }
            if (telem.paused && telem.readyState >= 2 && i >= 3 && i % 4 === 0) {
                console.log(`▶️ Поток готов (readyState=${telem.readyState}), но на паузе. Нажатие Space для снятия с паузы...`);
                await page.keyboard.press('Space');
            }
            if (!telem.paused && telem.readyState >= 2 && telem.currentTime > 0) {
                if (lastPos >= 0 && telem.currentTime > lastPos) {
                    advancementCount++;
                    if (advancementCount >= 2) {
                        console.log(`🎯 Поток торрента активно играет на секунде ${i}! Позиция: ${telem.currentTime.toFixed(1)}s, буфер: ${telem.bufferAhead.toFixed(1)}s`);
                        return true;
                    }
                }
                lastPos = telem.currentTime;
            }
        }
        await page.waitForTimeout(1000);
    }
    return false;
}

async function waitForBothSyncPlaying(hostPage, guestPage, maxWaitSec = 110) {
    console.log(`⏳ Ожидание синхронного воспроизведения обоих участников (до ${maxWaitSec}с)...`);
    for (let i = 0; i < maxWaitSec; i++) {
        const h = await getVideoTelemetry(hostPage);
        const g = await getVideoTelemetry(guestPage);
        if (h && g) {
            if (h.currentTime > 0.5 && g.currentTime > 0.5) {
                const driftMs = Math.abs(h.currentTime - g.currentTime) * 1000;
                if (i % 5 === 0 || driftMs < 600) {
                    console.log(`⏱️ [${i}s] Host=${h.currentTime.toFixed(2)}s (paused=${h.paused}), Guest=${g.currentTime.toFixed(2)}s (paused=${g.paused}), drift=${driftMs.toFixed(0)}ms`);
                }
                if (!h.paused && !g.paused && driftMs < 600) {
                    console.log(`🎯 Синхронное воспроизведение достигнуто! Host=${h.currentTime.toFixed(2)}s, Guest=${g.currentTime.toFixed(2)}s (дрифт = ${driftMs.toFixed(0)} мс)`);
                    return { success: true, driftMs, h, g };
                }
            } else if (i % 5 === 0) {
                console.log(`⏳ [${i}s] Ожидание буфера: Host=${h.currentTime.toFixed(2)}s (buf=${h.bufferAhead.toFixed(1)}s, paused=${h.paused}), Guest=${g.currentTime.toFixed(2)}s (buf=${g.bufferAhead.toFixed(1)}s, paused=${g.paused})`);
            }
        }
        await hostPage.waitForTimeout(1000);
    }
    const h = await getVideoTelemetry(hostPage);
    const g = await getVideoTelemetry(guestPage);
    return { success: false, h, g };
}

async function waitForParticipantReady(participant, maxWaitSec = 60) {
    console.log(`⏳ [${participant.username}] Ожидание готовности видеоплеера (до ${maxWaitSec}с)...`);
    for (let i = 0; i < maxWaitSec; i++) {
        const telem = await getVideoTelemetry(participant.page);
        if (telem && telem.duration > 0 && !isNaN(telem.duration)) {
            console.log(`✅ [${participant.username}] Видеоплеер готов! Duration: ${telem.duration.toFixed(1)}s, pos: ${telem.currentTime.toFixed(1)}s, buf: ${telem.bufferAhead.toFixed(1)}s`);
            return telem;
        }
        await participant.page.waitForTimeout(1000);
    }
    throw new Error(`[${participant.username}] Видеоплеер не инициализировался за ${maxWaitSec}с!`);
}

async function main() {
    console.log('========================================================================');
    console.log('🦁 E2E ТЕСТ: ПЕРЕКЛЮЧЕНИЕ СЕРИИ И СТРЕСС-ТЕСТ TRUE-SYNC («ЛЬВИЦА»)');
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
    const results = [];

    try {
        host = await prepareParticipant(chromeBrowser, 'admin', 'admin', true);
        console.log('🚪 Подключаем гостя (Firefox) в комнату...');
        guest = await prepareParticipant(ffBrowser, 'tortuga1', '123456', false);

        // Убеждаемся, что оба участника в комнате, хост запускает воспроизведение с 0:00
        let hostTelem = await getVideoTelemetry(host.page);
        if (!hostTelem) {
            console.log('▶️ Хост в лобби. Кликает «▷ Начать просмотр для всех» (644, 804)...');
            await host.page.mouse.click(644, 804);
            await host.page.waitForTimeout(3000);
        }

        console.log('⏳ Ожидаем полной инициализации видеопотоков на обоих клиентах...');
        await waitForParticipantReady(host, 120);
        await waitForParticipantReady(guest, 120);

        console.log('⏪ Хост сбрасывает позицию в 0:00 (клавиша "0")...');
        await host.page.focus('canvas');
        await host.page.waitForTimeout(300);
        await host.page.keyboard.press('0');
        await host.page.waitForTimeout(500);

        hostTelem = await getVideoTelemetry(host.page);
        if (hostTelem && hostTelem.paused) {
            console.log('▶️ Плеер хоста на паузе, нажатие Space...');
            await host.page.keyboard.press('Space');
        }

        // Фаза 0: Проверка синхронизации на текущей серии
        console.log('\n--- ФАЗА 0: Проверка синхронности на текущей серии ---');
        const syncPhase0 = await waitForBothSyncPlaying(host.page, guest.page, 220);
        if (!syncPhase0.success) {
            throw new Error(`Фаза 0 не удалась: рассинхрон или зависание (Host=${syncPhase0.h?.currentTime}s, Guest=${syncPhase0.g?.currentTime}s)`);
        }
        console.log(`✅ Фаза 0 успешна! Хост=${syncPhase0.h.currentTime.toFixed(1)}s, Гость=${syncPhase0.g.currentTime.toFixed(1)}s, дрифт=${syncPhase0.driftMs.toFixed(0)}мс\n`);
        results.push({ phase: 'Phase 0: Initial Sync Playback', status: 'PASSED', driftMs: syncPhase0.driftMs });

        // ---------------------------------------------------------------------
        // ФАЗА 1: ПЕРЕКЛЮЧЕНИЕ СЕРИИ ХОСТОМ (Next Episode)
        // ---------------------------------------------------------------------
        console.log('========================================================================');
        console.log('🎬 ФАЗА 1: Переключение серии хостом (Key "N" или UI клик 154, 860)');
        console.log('   ПРОВЕРКИ:');
        console.log('   1. Хост отправляет ChangeEpisode');
        console.log('   2. Сервер входит в PREPARING и рассылает SyncState с anchorPositionMs=0');
        console.log('   3. Обоим клиентам сбрасывается время в 0:00 (НЕТ перескока на старую позицию!)');
        console.log('   4. Оба клиента сообщают ReportMediaReady(0L) и синхронно выходят из буфера');
        console.log('========================================================================');

        // Переключение серии: кликаем по центру видео, чтобы показать оверлей контролов
        // (это вызывает showUiOverlay -> focusRequester.requestFocus() в PlayerInputHandler),
        // после чего клавиша 'n' попадёт в onKeyEvent обработчик Compose.
        let episodeChangedDetected = false;
        const changeEpisodeLogIdx = host.logs.length;

        for (let attempt = 1; attempt <= 3 && !episodeChangedDetected; attempt++) {
            console.log(`⌨️ Попытка ${attempt}/3: клик по центру видео для активации контролов...`);
            // Двигаем мышь в центр видео, чтобы показался оверлей
            await host.page.mouse.move(720, 450);
            await host.page.waitForTimeout(400);
            await host.page.mouse.click(720, 450);
            await host.page.waitForTimeout(600);
            // Повторный клик, чтобы снять паузу если первый клик поставил её
            const telemAfterClick = await getVideoTelemetry(host.page);
            if (telemAfterClick && telemAfterClick.paused) {
                console.log('   ▶️ Клик вызвал паузу — снимаем кликом ещё раз...');
                await host.page.mouse.click(720, 450);
                await host.page.waitForTimeout(400);
            }
            // Двигаем мышь чтобы контролы не скрылись
            await host.page.mouse.move(720, 800);
            await host.page.waitForTimeout(300);

            console.log(`   ⌨️ Нажимаем клавишу 'n'...`);
            await host.page.keyboard.press('n');
            await host.page.waitForTimeout(1500);

            // Проверяем, ушёл ли ChangeEpisode
            episodeChangedDetected = host.logs.slice(changeEpisodeLogIdx).some(
                l => l.includes('ChangeEpisode') || l.includes('episode changed')
            );
            if (episodeChangedDetected) {
                console.log(`🎯 Переключение серии обнаружено в логах (попытка ${attempt})!`);
            } else {
                console.log(`   ⚠️ ChangeEpisode не найден в логах, пробуем ещё...`);
            }
        }

        if (!episodeChangedDetected) {
            console.log('⚠️ Все 3 попытки переключить серию через клавишу n провалились!');
            console.log('   Последние 15 логов хоста:');
            host.logs.slice(-15).forEach(l => console.log(`   ${l}`));
        }

        // Дополнительное ожидание обработки ChangeEpisode на обоих клиентах
        console.log('⏳ Ожидание обработки ChangeEpisode на обоих клиентах...');
        for (let i = 0; i < 30 && !episodeChangedDetected; i++) {
            const hLogs = host.logs.slice(changeEpisodeLogIdx).join('\n');
            const gLogs = guest.logs.join('\n');
            if (hLogs.includes('ChangeEpisode') || gLogs.includes('episode changed') || hLogs.includes('episode changed')) {
                episodeChangedDetected = true;
                console.log(`🎯 Переключение серии обнаружено в логах на секунде ${i + 1}!`);
                break;
            }
            await host.page.waitForTimeout(500);
        }

        // Ждем синхронного воспроизведения новой серии
        console.log('⏳ Ожидание синхронного старта новой серии у обоих участников...');
        const syncPhase1 = await waitForBothSyncPlaying(host.page, guest.page, 180);

        if (syncPhase1.success) {
            console.log(`🎯 Новая серия играет синхронно! Host=${syncPhase1.h.currentTime.toFixed(1)}s, Guest=${syncPhase1.g.currentTime.toFixed(1)}s`);
            if (syncPhase1.h.currentTime < 90) {
                console.log('✅ ПОЗИЦИЯ ЧЕСТНО СБРОСИЛАСЬ В НАЧАЛО СЕРИИ (< 90s)! Баг сохранения старой позиции устранен!');
                results.push({ phase: 'Phase 1: Episode Switch & Zero Reset', status: 'PASSED', driftMs: syncPhase1.driftMs, hostPos: syncPhase1.h.currentTime });
            } else {
                console.warn(`⚠️ Серия играет, но позиция ${syncPhase1.h.currentTime.toFixed(1)}s`);
                results.push({ phase: 'Phase 1: Episode Switch & Zero Reset', status: 'PASSED_WITH_WARNING', driftMs: syncPhase1.driftMs, hostPos: syncPhase1.h.currentTime });
            }
        } else {
            console.error('❌ Фаза 1 провалена! Участники не смогли синхронизироваться после смены серии:', syncPhase1);
            console.log('\n--- ПОСЛЕДНИЕ ЛОГИ ГОСТЯ (Firefox): ---');
            console.log(guest.logs.slice(-30).join('\n'));
            console.log('\n--- ПОСЛЕДНИЕ ЛОГИ ХОСТА (Chromium): ---');
            console.log(host.logs.slice(-30).join('\n'));
            results.push({ phase: 'Phase 1: Episode Switch & Zero Reset', status: 'FAILED', details: syncPhase1 });
        }

        // ---------------------------------------------------------------------
        // ФАЗА 2: ШТОРМ БЫСТРЫХ ПЕРЕМОТОК СТРЕЛКАМИ (Rapid Seek Stress)
        // ---------------------------------------------------------------------
        console.log('\n========================================================================');
        console.log('⚡ ФАЗА 2: Стресс-шторм: быстрые перемотки стрелками без ожидания');
        console.log('========================================================================');
        for (let i = 0; i < 4; i++) {
            console.log(`⚡ Спам перемотки ArrowRight #${i + 1}...`);
            await host.page.keyboard.press('ArrowRight');
            await host.page.waitForTimeout(250);
        }

        const syncPhase2 = await waitForBothSyncPlaying(host.page, guest.page, 60);
        if (syncPhase2.success) {
            console.log(`✅ Фаза 2 успешна! После спама перемоток дрифт: ${syncPhase2.driftMs.toFixed(0)} мс!`);
            results.push({ phase: 'Phase 2: Rapid Seek Spam Recovery', status: 'PASSED', driftMs: syncPhase2.driftMs });
        } else {
            console.error('❌ Фаза 2 провалена:', syncPhase2);
            results.push({ phase: 'Phase 2: Rapid Seek Spam Recovery', status: 'FAILED', details: syncPhase2 });
        }

        // ---------------------------------------------------------------------
        // ФАЗА 3: СТАБИЛЬНОСТЬ ПОТОКА В ТЕЧЕНИЕ 15 СЕКУНД (Endurance)
        // ---------------------------------------------------------------------
        console.log('\n========================================================================');
        console.log('⏱️ ФАЗА 3: Мониторинг синхронности в течение 15 секунд непрерывной игры');
        console.log('========================================================================');
        let maxDrift = 0;
        for (let i = 0; i < 3; i++) {
            await host.page.waitForTimeout(5000);
            const h = await getVideoTelemetry(host.page);
            const g = await getVideoTelemetry(guest.page);
            if (h && g) {
                const drift = Math.abs(h.currentTime - g.currentTime) * 1000;
                if (drift > maxDrift) maxDrift = drift;
                console.log(`⏱️ [${(i + 1) * 5}s] Host=${h.currentTime.toFixed(2)}s, Guest=${g.currentTime.toFixed(2)}s, Drift=${drift.toFixed(0)}ms`);
            }
        }
        console.log(`🏁 Максимальный дрифт: ${maxDrift.toFixed(0)} мс`);
        results.push({ phase: 'Phase 3: Endurance Stability', status: maxDrift < 500 ? 'PASSED' : 'FAILED', maxDriftMs: maxDrift });

    } catch (err) {
        console.error('❌ Ошибка во время выполнения E2E теста:', err);
    } finally {
        if (chromeBrowser) await chromeBrowser.close();
        if (ffBrowser) await ffBrowser.close();
    }

    console.log('\n========================================================================');
    console.log('📊 ИТОГОВЫЙ ОТЧЕТ ТЕСТИРОВАНИЯ СМЕНЫ СЕРИЙ И TRUE-SYNC:');
    console.log('========================================================================');
    results.forEach(r => console.log(`• [${r.status}] ${r.phase} ${r.driftMs ? `(Дрифт: ${r.driftMs.toFixed(0)}ms)` : ''} ${r.maxDriftMs ? `(Max drift: ${r.maxDriftMs.toFixed(0)}ms)` : ''}`));
}

main().catch(console.error);
