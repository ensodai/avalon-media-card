import { chromium, firefox } from 'playwright';
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

async function loginAndEnterLobby(browser, username, password) {
    const page = await browser.newPage({ viewport: { width: 1280, height: 720 } });
    await setupWebSocketRedirect(page);

    const logs = [];
    page.on('console', msg => {
        const text = msg.text();
        logs.push(`[${new Date().toISOString()}] ${text}`);
        if (text.includes('TrueSync') || text.includes('TransitionToPlayer') || text.includes('drift') || text.includes('rate=')) {
            console.log(`[${username}] ${text}`);
        }
    });

    console.log(`[${username}] Открытие страницы...`);
    await page.goto(APP_URL, { waitUntil: 'networkidle' });
    await page.waitForTimeout(3000);

    // Авторизация
    console.log(`[${username}] Ввод логина и пароля...`);
    await page.mouse.click(520, 247);
    await page.keyboard.type(username);
    await page.waitForTimeout(150);
    await page.mouse.click(520, 320);
    await page.keyboard.type(password);
    await page.waitForTimeout(150);
    await page.mouse.click(640, 460); // Войти
    await page.waitForTimeout(3500);

    // Переход в Совместный просмотр
    console.log(`[${username}] Переход в Совместный просмотр (x: 40, y: 370)...`);
    await page.mouse.click(40, 370);
    await page.waitForTimeout(2000);

    // Клик по комнате в списке
    console.log(`[${username}] Выбор комнаты (x: 500, y: 280)...`);
    await page.mouse.click(500, 280);
    await page.waitForTimeout(2500);

    return { page, logs };
}

async function run() {
    console.log('🚀 ЗАПУСК ПОЛНОГО E2E ТЕСТА В ДВУХ БРАУЗЕРАХ');

    const chromeBrowser = await chromium.launch({ headless: true });
    const ffBrowser = await firefox.launch({ headless: true });

    const chrome = await loginAndEnterLobby(chromeBrowser, 'admin', 'admin');
    const ff = await loginAndEnterLobby(ffBrowser, 'tortuga1', 'tortuga1');

    console.log('\nОба браузера зашли в лобби комнаты. Ожидаем 3 секунды...');
    await new Promise(r => setTimeout(r, 3000));

    // Проверяем наличие видеотега до старта
    const cVidBefore = await chrome.page.evaluate(() => !!document.querySelector('video'));
    const fVidBefore = await ff.page.evaluate(() => !!document.querySelector('video'));
    console.log(`Видео до старта: Chrome=${cVidBefore}, FF=${fVidBefore}`);

    // Теперь в Хроме кликаем кнопку старта («Начать просмотр» в лобби)
    // Либо ждем перехода
    console.log('Кликаем "Начать просмотр" в Хроме или проверяем реакцию на старт...');
    // В лобби кнопка старта находится внизу по центру (x: 640, y: 650)
    await chrome.page.mouse.click(640, 650);
    await new Promise(r => setTimeout(r, 5000));

    // Проверяем наличие видеотега после старта
    const cVidAfter = await chrome.page.evaluate(() => {
        const v = document.querySelector('video');
        return v ? { currentTime: v.currentTime, paused: v.paused, rate: v.playbackRate } : null;
    });
    const fVidAfter = await ff.page.evaluate(() => {
        const v = document.querySelector('video');
        return v ? { currentTime: v.currentTime, paused: v.paused, rate: v.playbackRate } : null;
    });

    console.log('Состояние плееров после старта:');
    console.log('Chrome:', cVidAfter);
    console.log('Firefox:', fVidAfter);

    // Сбор логов
    const logsDir = path.resolve('../логи');
    fs.writeFileSync(path.join(logsDir, 'e2e_chrome_console.txt'), chrome.logs.join('\n'));
    fs.writeFileSync(path.join(logsDir, 'e2e_firefox_console.txt'), ff.logs.join('\n'));
    console.log('Логи записаны в логи/e2e_chrome_console.txt и e2e_firefox_console.txt');

    await chromeBrowser.close();
    await ffBrowser.close();
}

run().catch(console.error);
