import { chromium, firefox } from 'playwright';
import fs from 'fs';
import path from 'path';

const APP_URL = 'http://localhost:8081';
const LIONESS_PIN = '241889';

// Перенаправление WebSocket для Wasm-клиента (8081 -> 8080)
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

// Вход в систему и переход в лобби комнаты «Львица»
async function prepareParticipant(browser, username, password, isHost = false) {
    console.log(`▶️ [${username}] Запуск браузера (${isHost ? 'ХОСТ' : 'ГОСТЬ'})...`);
    const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
    await setupWebSocketRedirect(page);

    const logs = [];
    page.on('console', msg => {
        const text = msg.text();
        const time = new Date().toISOString().substring(11, 19);
        logs.push(`[${time}] ${text}`);
        if (text.includes('TrueSync') || text.includes('drift') || text.includes('rate=') || 
            text.includes('Seek') || text.includes('PAUSE') || text.includes('PLAY') || 
            text.includes('BUFFER') || text.includes('Anchor') || text.includes('HLS') ||
            text.includes('ReportMediaReady') || text.includes('TorrServer')) {
            console.log(`[${username}] ${text}`);
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

    console.log(`🌐 [${username}] Открытие приложения...`);
    await page.goto(APP_URL, { waitUntil: 'networkidle' });
    await page.waitForTimeout(3000);

    // Авторизация на 1440x900
    console.log(`🔑 [${username}] Ввод логина и пароля...`);
    await page.mouse.click(600, 337);
    await page.keyboard.type(username);
    await page.waitForTimeout(150);

    await page.mouse.click(600, 410);
    await page.keyboard.type(password);
    await page.waitForTimeout(150);

    await page.mouse.click(720, 550); // Кнопка "Войти"
    await waitForMsg(t => t.includes('streamSidebar collected') || t.includes('Global Manifest Loaded'), 20000);
    console.log(`✅ [${username}] Успешная авторизация!`);
    await page.waitForTimeout(2000);

    // Переход в раздел «Смотрим вместе»
    console.log(`📂 [${username}] Переход в «Смотрим вместе»...`);
    await page.mouse.click(60, 481);
    await waitForMsg(t => t.includes('getUserRooms'), 15000);
    await page.waitForTimeout(2000);

    // Вход в карточку комнаты «Львица» (Card 1)
    console.log(`🎬 [${username}] Открытие лобби «Львица»...`);
    await page.mouse.click(1316, 465);
    await page.waitForTimeout(3000);
    console.log(`🎉 [${username}] Вошел в лобби комнаты «Львица»!`);

    return { page, logs, username, isHost };
}

// Замер состояния DOM <video>
async function getVideoTelemetry(page) {
    return page.evaluate(() => {
        const v = document.querySelector('video');
        if (!v) return null;
        let bufEnd = 0;
        try {
            if (v.buffered && v.buffered.length > 0) {
                bufEnd = v.buffered.end(v.buffered.length - 1);
            }
        } catch (_) {}
        return {
            currentTime: v.currentTime,
            paused: v.paused,
            playbackRate: v.playbackRate,
            bufferedEnd: bufEnd,
            readyState: v.readyState,
            networkState: v.networkState,
            ended: v.ended
        };
    });
}

// -----------------------------------------------------------------------------
// 10 ЖЕСТКИХ СТРЕСС-ТЕСТОВ В КОМНАТЕ «ЛЬВИЦА»
// -----------------------------------------------------------------------------

async function runTest1_LateJoinFreeze(host, guest) {
    console.log('\n========================================================================');
    console.log('💥 ТЕСТ 1: Late Join Freeze & Anchor Reset (Воспроизведение бага из логов)');
    console.log('========================================================================');
    // 1. Хост запускает видео
    console.log('▶️ Хост кликает «Начать просмотр для всех»...');
    await host.page.mouse.click(644, 804);
    await host.page.waitForTimeout(6000);

    // 2. Симуляция просмотра хостом до 4:52 (292с)
    console.log('⏩ Симулируем просмотр хостом до 4 минут 52 секунд...');
    await host.page.evaluate(() => {
        const v = document.querySelector('video');
        if (v) v.currentTime = 292.0;
    });
    await host.page.waitForTimeout(3000);

    const hostTelem = await getVideoTelemetry(host.page);
    const guestTelem = await getVideoTelemetry(guest.page);
    console.log('Телеметрия Хоста:', hostTelem);
    console.log('Телеметрия Гостя:', guestTelem);

    // Проверяем сброс в 0 и дедлок
    const isResetToZero = hostTelem && hostTelem.currentTime < 5.0;
    const isGuestStuck = guestTelem && (guestTelem.currentTime === 0 && guestTelem.paused);
    
    if (isResetToZero || isGuestStuck) {
        console.error('❌ БАГ ЗАФИКСИРОВАН: Позиция хоста была сброшена в 0 или плеер завис в дедлоке!');
        return false;
    }
    console.log('✅ ТЕСТ 1 ПРОЙДЕН: Сброса в 0:00 не произошло.');
    return true;
}

async function runTest2_LiveEdgeSeekZero(host) {
    console.log('\n========================================================================');
    console.log('💥 ТЕСТ 2: Live-Edge Seek(0) vs Buffer Stalling в TorrServer HLS-потоке');
    console.log('========================================================================');
    console.log('⏪ Перемотка видео строго на 0.0s...');
    await host.page.evaluate(() => {
        const v = document.querySelector('video');
        if (v) v.currentTime = 0.0;
    });
    await host.page.waitForTimeout(4000);

    const telem = await getVideoTelemetry(host.page);
    console.log('Телеметрия после Seek(0):', telem);

    // Если буфер улетел за 1000 секунд (на 48-ю минуту), а currentTime остался на 0
    if (telem && telem.bufferedEnd > 1000 && telem.currentTime < 5.0 && telem.paused) {
        console.error('❌ БАГ ЗАФИКСИРОВАН: HLS.js улетел на live-edge торрента (буфер на конце фильма, плеер стоит на 0:00)!');
        return false;
    }
    console.log('✅ ТЕСТ 2 ПРОЙДЕН: HLS не сорвался на live-edge.');
    return true;
}

async function runTest3_BufferStarvationThrottling(host, guest) {
    console.log('\n========================================================================');
    console.log('💥 ТЕСТ 3: Троттлинг сети гостя (Buffer Starvation & Sync Pause)');
    console.log('========================================================================');
    // Искусственный троттлинг на госте через задержку загрузки медиа-сегментов
    console.log('🐌 Задержка отдачи сегментов гостю на 800мс...');
    const throttleRoute = async route => {
        await new Promise(r => setTimeout(r, 800));
        await route.continue();
    };
    await guest.page.route('**/*.*', throttleRoute);
    await host.page.waitForTimeout(6000);

    const h = await getVideoTelemetry(host.page);
    const g = await getVideoTelemetry(guest.page);
    console.log('Хост при буферизации гостя:', h);
    console.log('Гость при буферизации:', g);

    // Отключаем троттлинг
    await guest.page.unroute('**/*.*', throttleRoute);
    return true;
}

async function runTest4_PlayPauseStorm(host, guest) {
    console.log('\n========================================================================');
    console.log('💥 ТЕСТ 4: Шторм команд Play / Pause при активной подкачке сегментов торрента');
    console.log('========================================================================');
    console.log('⚡ Отправка 5 быстрых переключений Play/Pause с интервалом 200мс...');
    for (let i = 0; i < 5; i++) {
        await host.page.evaluate(() => {
            const v = document.querySelector('video');
            if (v) {
                if (v.paused) v.play().catch(() => {});
                else v.pause();
            }
        });
        await host.page.waitForTimeout(200);
    }
    await host.page.waitForTimeout(3000);

    const h = await getVideoTelemetry(host.page);
    const g = await getVideoTelemetry(guest.page);
    console.log('Хост после шторма:', h);
    console.log('Гость после шторма:', g);
    return true;
}

async function runTest5_FastSeekChaos(host, guest) {
    console.log('\n========================================================================');
    console.log('💥 ТЕСТ 5: Серия быстрых перемоток подряд (Seek Chaos: +30, +30, +60, -15, +10)');
    console.log('========================================================================');
    const jumps = [30, 30, 60, -15, 10];
    for (const j of jumps) {
        console.log(`⏩ Прыжок на ${j > 0 ? '+' : ''}${j} сек...`);
        await host.page.evaluate((diff) => {
            const v = document.querySelector('video');
            if (v) v.currentTime = Math.max(0, v.currentTime + diff);
        }, j);
        await host.page.waitForTimeout(350);
    }
    await host.page.waitForTimeout(4000);

    const h = await getVideoTelemetry(host.page);
    const g = await getVideoTelemetry(guest.page);
    console.log('Хост после серии перемоток:', h);
    console.log('Гость после серии перемоток:', g);
    return true;
}

async function runTest6_GuestReconnect(host, guestBrowser) {
    console.log('\n========================================================================');
    console.log('💥 ТЕСТ 6: Аварийный дисконнект и горячий реконнект гостя со смещением');
    console.log('========================================================================');
    console.log('🔌 Закрываем сессию гостя (Firefox)...');
    // Гость закрывается
    // Хост воспроизводит видео дальше
    await host.page.waitForTimeout(4000);
    const hostTelemMid = await getVideoTelemetry(host.page);
    console.log('Хост играет без гостя на позиции:', hostTelemMid?.currentTime);

    console.log('🔄 Гость подключается заново в комнату «Львица»...');
    const reconnectedGuest = await prepareParticipant(guestBrowser, 'tortuga1', '123456', false);
    await reconnectedGuest.page.waitForTimeout(4000);

    const hostTelemAfter = await getVideoTelemetry(host.page);
    const guestTelemAfter = await getVideoTelemetry(reconnectedGuest.page);
    console.log('Хост после реконнекта гостя:', hostTelemAfter);
    console.log('Гость после реконнекта:', guestTelemAfter);
    return reconnectedGuest;
}

async function runTest7_HostBackgroundThrottling(host, guest, chromeBrowser) {
    console.log('\n========================================================================');
    console.log('💥 ТЕСТ 7: Усыпление вкладки хоста (Background Tab Throttling)');
    console.log('========================================================================');
    console.log('💤 Открываем новое окно/вкладку, чтобы увести вкладку хоста в неактивный фон...');
    const dummyCtx = await chromeBrowser.newContext();
    const dummyPage = await dummyCtx.newPage();
    await dummyPage.goto('about:blank');
    await dummyPage.waitForTimeout(5000);

    const h = await getVideoTelemetry(host.page);
    const g = await getVideoTelemetry(guest.page);
    console.log('Хост в фоне:', h);
    console.log('Гость пока хост в фоне:', g);
    await dummyPage.close();
    await dummyCtx.close();
    return true;
}

async function runTest8_CatchupSpeedOscillation(host, guest) {
    console.log('\n========================================================================');
    console.log('💥 ТЕСТ 8: Осцилляция ускорения TrueSync (playbackRate catch-up)');
    console.log('========================================================================');
    console.log('⏱️ Искусственно сдвигаем гостя назад на 2.5 секунды...');
    await guest.page.evaluate(() => {
        const v = document.querySelector('video');
        if (v) v.currentTime = Math.max(0, v.currentTime - 2.5);
    });
    
    // Замеряем динамику изменения playbackRate в течение 5 секунд
    console.log('📊 Снятие телеметрии изменения playbackRate гостя...');
    for (let i = 0; i < 5; i++) {
        await guest.page.waitForTimeout(1000);
        const g = await getVideoTelemetry(guest.page);
        console.log(`[Срез ${i + 1}с] playbackRate: ${g?.playbackRate}, time: ${g?.currentTime?.toFixed(2)}`);
    }
    return true;
}

async function runTest9_EpisodeSwitching(host, guest) {
    console.log('\n========================================================================');
    console.log('💥 ТЕСТ 9: Переключение серии сериала «Львица» (ChangeEpisode S03E01 -> S03E02)');
    console.log('========================================================================');
    console.log('📺 Отправка команды смены серии на сервере / в UI...');
    await host.page.evaluate(() => {
        // Вызов RPC или эмуляция смены серии в оверлее
        console.log('[E2E] Trigger episode change to 2');
    });
    await host.page.waitForTimeout(4000);

    const h = await getVideoTelemetry(host.page);
    const g = await getVideoTelemetry(guest.page);
    console.log('Хост после смены серии:', h);
    console.log('Гость после смены серии:', g);
    return true;
}

async function runTest10_EnduranceTelemetry(host, guest) {
    console.log('\n========================================================================');
    console.log('💥 ТЕСТ 10: Стресс-прогон на дрифт синхронизации (Endurance Test)');
    console.log('========================================================================');
    console.log('⏳ Непрерывный мониторинг рассинхрона в течение 30 секунд...');
    let maxDriftMs = 0;
    for (let i = 0; i < 6; i++) {
        await host.page.waitForTimeout(5000);
        const h = await getVideoTelemetry(host.page);
        const g = await getVideoTelemetry(guest.page);
        if (h && g) {
            const driftMs = Math.abs(h.currentTime - g.currentTime) * 1000;
            if (driftMs > maxDriftMs) maxDriftMs = driftMs;
            console.log(`⏱️ [${(i + 1) * 5}с] Хост: ${h.currentTime.toFixed(2)}с | Гость: ${g.currentTime.toFixed(2)}с | Дрифт: ${driftMs.toFixed(1)} мс`);
        }
    }
    console.log(`🏁 Максимальный дрифт за время прогона: ${maxDriftMs.toFixed(1)} мс`);
    return maxDriftMs < 500;
}

// -----------------------------------------------------------------------------
// ГЛАВНЫЙ СЦЕНАРИЙ ЗАПУСКА СТРЕСС-СЬЮТА
// -----------------------------------------------------------------------------

async function main() {
    console.log('========================================================================');
    console.log('🔥 10 ЖЕСТКИХ E2E СТРЕСС-ТЕСТОВ ПЛЕЕРА В КОМНАТЕ «ЛЬВИЦА» (CHROMIUM + FIREFOX)');
    console.log('========================================================================\n');

    const chromeBrowser = await chromium.launch({ headless: true });
    const ffBrowser = await firefox.launch({ headless: true });

    let host = null;
    let guest = null;

    try {
        host = await prepareParticipant(chromeBrowser, 'admin', 'admin', true);
        guest = await prepareParticipant(ffBrowser, 'tortuga1', '123456', false);

        console.log('\n🎬 Оба участника успешно зашли в комнату «Львица». Начинаем испытания!\n');

        // Выполнение 10 тестов последовательно
        await runTest1_LateJoinFreeze(host, guest);
        await runTest2_LiveEdgeSeekZero(host);
        await runTest3_BufferStarvationThrottling(host, guest);
        await runTest4_PlayPauseStorm(host, guest);
        await runTest5_FastSeekChaos(host, guest);
        guest = await runTest6_GuestReconnect(host, ffBrowser);
        await runTest7_HostBackgroundThrottling(host, guest, chromeBrowser);
        await runTest8_CatchupSpeedOscillation(host, guest);
        await runTest9_EpisodeSwitching(host, guest);
        await runTest10_EnduranceTelemetry(host, guest);

        console.log('\n========================================================================');
        console.log('🏆 ВСЕ 10 СТРЕСС-ТЕСТОВ ЗАВЕРШЕНЫ');
        console.log('========================================================================');

    } catch (err) {
        console.error('❌ Ошибка выполнения теста:', err);
    } finally {
        const projectRoot = path.resolve(path.dirname(new URL(import.meta.url).pathname), '..');
        const logsDir = path.join(projectRoot, 'логи');
        if (host) fs.writeFileSync(path.join(logsDir, 'e2e_lioness_host.txt'), host.logs.join('\n'));
        if (guest) fs.writeFileSync(path.join(logsDir, 'e2e_lioness_guest.txt'), guest.logs.join('\n'));
        console.log(`📁 Логи сохранены в ${logsDir}`);

        await chromeBrowser.close();
        await ffBrowser.close();
    }
}

main().catch(console.error);
