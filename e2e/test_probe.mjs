import { chromium, firefox } from 'playwright';

async function testBrowserE2E() {
    console.log('🚀 Launching Chromium with WebSocket port redirect to 8080...');
    const browser = await chromium.launch({ headless: true });
    const page = await browser.newPage();

    // Перехват сокета: перенаправляем 8081 -> 8080
    await page.addInitScript(() => {
        const OrigWebSocket = window.WebSocket;
        window.WebSocket = function(url, protocols) {
            console.log('[BROWSER_HOOK] WebSocket connecting to: ' + url);
            if (url && url.includes(':8081/api/rpc')) {
                url = url.replace(':8081/api/rpc', ':8080/api/rpc');
                console.log('[BROWSER_HOOK] Redirected WebSocket to: ' + url);
            }
            return new OrigWebSocket(url, protocols);
        };
    });

    const logs = [];
    page.on('console', msg => {
        const text = msg.text();
        logs.push(text);
        if (text.includes('TrueSync') || text.includes('RPC') || text.includes('WebSocket') || text.includes('HOOK')) {
            console.log(`[Chrome Log] ${text}`);
        }
    });

    console.log('Navigating to http://localhost:8081 ...');
    await page.goto('http://localhost:8081', { waitUntil: 'networkidle', timeout: 15000 });

    console.log('Waiting 5 seconds for Wasm and RPC connection...');
    await page.waitForTimeout(5000);

    const hasConnected = logs.some(l => l.includes('RPC Connection ESTABLISHED') || l.includes('Connected as guest') || l.includes('BROWSER_HOOK'));
    console.log('Has RPC connected?', hasConnected);

    await browser.close();
}

testBrowserE2E().catch(err => {
    console.error('Test failed:', err);
    process.exit(1);
});
