import { chromium } from 'playwright';

async function testAutomatedLogin() {
    console.log('🚀 Starting automated Playwright login test...');
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

    const logs = [];
    page.on('console', msg => {
        const text = msg.text();
        logs.push(text);
        if (text.includes('login') || text.includes('RPC') || text.includes('Token') || text.includes('TrueSync')) {
            console.log(`[Browser Console] ${text}`);
        }
    });

    await page.goto('http://localhost:8081', { waitUntil: 'networkidle', timeout: 15000 });
    await page.waitForTimeout(3000);

    // Type login admin
    console.log('Typing username...');
    await page.mouse.click(520, 247);
    await page.keyboard.type('admin');
    await page.waitForTimeout(300);

    // Type password admin
    console.log('Typing password...');
    await page.mouse.click(520, 320);
    await page.keyboard.type('admin');
    await page.waitForTimeout(300);

    // Click Login
    console.log('Clicking login button...');
    await page.mouse.click(640, 460);

    console.log('Waiting for authentication response...');
    await page.waitForTimeout(5000);

    await page.screenshot({ path: 'automated_login_result.png' });
    console.log('Saved automated_login_result.png');

    const authSuccess = logs.some(l => l.includes('Channel successfully authenticated') || l.includes('Token found locally') || l.includes('role=ADMIN'));
    console.log('Auth success status:', authSuccess);

    await browser.close();
}

testAutomatedLogin().catch(err => {
    console.error('Error:', err);
    process.exit(1);
});
