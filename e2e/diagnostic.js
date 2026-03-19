import { chromium } from '@playwright/test';

(async () => {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage();

  // Capture console messages and errors
  const consoleLogs = [];
  const errors = [];

  page.on('console', msg => {
    consoleLogs.push(`[${msg.type()}] ${msg.text()}`);
  });

  page.on('pageerror', err => {
    errors.push(err.message);
  });

  console.log('Navigating to http://localhost:3000...');
  await page.goto('http://localhost:3000', { waitUntil: 'networkidle', timeout: 30000 });

  console.log('Waiting for React to render...');
  await page.waitForTimeout(5000);

  // Get page content
  const rootContent = await page.locator('#root').innerHTML();
  console.log('\n=== #root innerHTML ===');
  if (rootContent.length === 0) {
    console.log('EMPTY - React did not render!');
  } else {
    console.log(rootContent.substring(0, 2000));
  }

  // Print console logs
  if (consoleLogs.length > 0) {
    console.log('\n=== Console Logs ===');
    consoleLogs.forEach(log => console.log(log));
  }

  // Print errors
  if (errors.length > 0) {
    console.log('\n=== Page Errors ===');
    errors.forEach(err => console.log(err));
  }

  // Take screenshot
  await page.screenshot({ path: 'e2e/screenshots/diagnostic.png', fullPage: true });
  console.log('\nScreenshot saved to e2e/screenshots/diagnostic.png');

  await browser.close();
})();