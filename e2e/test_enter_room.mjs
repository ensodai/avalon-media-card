import { chromium } from 'playwright';

async function findSidebarCoords() {
    const browser = await chromium.launch({ headless: true });
    const page = await browser.newPage({ viewport: { width: 1280, height: 720 } });

    await page.addInitScript(() => {
        const OrigWebSocket = window.WebSocket;
        window.WebSocket = function(url, protocols) {
            if (url && url.includes(':8081/api/rpc')) {
                url = url.replace(':8081/api/rpc', ':8080/api/rpc');
            }
            return new OrigWebSocket(url, protocols);
        };
    });

    await page.goto('http://localhost:8081', { waitUntil: 'networkidle' });
    await page.waitForTimeout(3000);

    // Login
    await page.mouse.click(520, 247);
    await page.keyboard.type('admin');
    await page.waitForTimeout(200);
    await page.mouse.click(520, 320);
    await page.keyboard.type('admin');
    await page.waitForTimeout(200);
    await page.mouse.click(640, 460);
    await page.waitForTimeout(4000);

    // На Главном экране сайдбар слева (ширина ~60-200px)
    // В сайдбаре пункты сверху вниз:
    // 1. Главная (y ~120)
    // 2. Фильмы (y ~170)
    // 3. Сериалы (y ~220)
    // 4. Тренды (y ~270)
    // 5. Поиск (y ~320)
    // 6. Совместный просмотр (y ~370)
    console.log('Кликаем по разделу "Совместный просмотр" (x: 40, y: 370)...');
    await page.mouse.click(40, 370);
    await page.waitForTimeout(2500);
    await page.screenshot({ path: 'screen_watch_rooms.png' });
    console.log('Скриншот экрана комнат сохранен в screen_watch_rooms.png');

    // На экране комнат первая комната в списке ("Смотрим хищника") по центру (x: 500, y: 280)
    console.log('Кликаем по комнате (x: 500, y: 280)...');
    await page.mouse.click(500, 280);
    await page.waitForTimeout(2500);
    await page.screenshot({ path: 'screen_room_lobby.png' });
    console.log('Скриншот лобби комнаты сохранен в screen_room_lobby.png');

    const videoPresent = await page.evaluate(() => !!document.querySelector('video'));
    console.log('Наличие тега video после входа:', videoPresent);

    await browser.close();
}

findSidebarCoords().catch(console.error);
