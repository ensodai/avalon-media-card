import { chromium } from 'playwright';

async function testVideo() {
    console.log('🚀 Starting video stream verification test...');
    const browser = await chromium.launch({
        headless: true,
        args: [
            '--no-sandbox',
            '--autoplay-policy=no-user-gesture-required',
            '--disable-web-security'
        ]
    });
    const context = await browser.newContext({ viewport: { width: 1440, height: 900 } });
    
    // Route any 8081 /api requests to 8080
    await context.route('**/api/**', route => {
        const orig = route.request().url();
        const rep = orig.replace(':8081', ':8080');
        if (orig !== rep) {
            console.log(`[Proxy] ${orig} -> ${rep}`);
            route.continue({ url: rep });
        } else {
            route.continue();
        }
    });

    const page = await context.newPage();
    page.on('console', msg => console.log(`[Browser] ${msg.text()}`));
    page.on('pageerror', err => console.error(`[Page Error] ${err.message}`));

    console.log('🌐 Opening page...');
    await page.goto('http://localhost:8081', { waitUntil: 'networkidle' });
    await page.waitForTimeout(3000);
    await page.screenshot({ path: 'screen_step1_open.png' });

    // Login
    console.log('🔑 Entering credentials and Server URL (http://localhost:8080)...');
    await page.mouse.click(600, 337);
    await page.keyboard.type('admin');
    await page.waitForTimeout(100);

    await page.mouse.click(600, 410);
    await page.keyboard.type('admin');
    await page.waitForTimeout(100);

    // Fill Server URL field
    await page.mouse.click(600, 480);
    await page.keyboard.press('Control+A');
    await page.keyboard.press('Backspace');
    await page.keyboard.type('http://localhost:8080');
    await page.waitForTimeout(200);
    await page.screenshot({ path: 'screen_step2_login_filled.png' });

    // Click Login button
    await page.mouse.click(720, 550);
    await page.waitForTimeout(4000);
    await page.screenshot({ path: 'screen_step3_after_login.png' });

    // Go to "Смотрим вместе"
    console.log('📂 Navigating to Watch Together...');
    await page.mouse.click(60, 481);
    await page.waitForTimeout(3000);
    await page.screenshot({ path: 'screen_step4_watch_rooms.png' });

    // Open "Львица"
    console.log('🎬 Entering «Львица»...');
    await page.mouse.click(1316, 465);
    await page.waitForTimeout(3000);
    await page.screenshot({ path: 'screen_step5_lobby.png' });

    // Click Start Playback
    console.log('▶️ Clicking «Начать просмотр для всех»...');
    await page.mouse.click(644, 804);
    await page.waitForTimeout(6000);
    await page.screenshot({ path: 'screen_step6_player.png' });

    // Check video telemetry
    for (let i = 0; i < 6; i++) {
        const telem = await page.evaluate(() => {
            const v = document.querySelector('video');
            if (!v) return null;
            let bufEnd = 0;
            try {
                if (v.buffered && v.buffered.length > 0) bufEnd = v.buffered.end(v.buffered.length - 1);
            } catch (_) {}
            return {
                currentTime: v.currentTime,
                paused: v.paused,
                playbackRate: v.playbackRate,
                bufferedEnd: bufEnd,
                readyState: v.readyState,
                networkState: v.networkState,
                duration: v.duration
            };
        });
        console.log(`📊 Video Telemetry (sample ${i+1}):`, telem);
        await page.waitForTimeout(3000);
    }

    await browser.close();
    console.log('🏁 Verification finished.');
}

testVideo().catch(err => {
    console.error('❌ Test failed with error:', err);
    process.exit(1);
});
