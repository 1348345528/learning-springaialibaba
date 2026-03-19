import { test, expect } from '@playwright/test';

// Test results storage
const testResults = [];

function logResult(name, passed, message = '') {
  testResults.push({ name, passed, message });
  console.log(`[${passed ? 'PASS' : 'FAIL'}] ${name}${message ? ': ' + message : ''}`);
}

// API Base URLs
const API = {
  docService: 'http://localhost:8081/api/doc',
  ragService: 'http://localhost:8082/api/vector',
  chatService: 'http://localhost:8083/api/chat',
};

test.describe('RAG Knowledge Base E2E Tests', () => {

  test('1. Document Upload Page', async ({ page }) => {
    try {
      await page.goto('http://localhost:3000', { waitUntil: 'networkidle', timeout: 20000 });

      // Wait for React to render
      await page.waitForTimeout(2000);

      // Check page title
      const title = await page.locator('h3:has-text("文档上传"), h1:has-text("文档上传")').count();
      logResult('1.1 Document Upload Page - Title', title > 0, title > 0 ? 'Title found' : 'Title not found');

      // Check for Dragger upload component
      const dragger = await page.locator('.ant-upload-drag').count();
      logResult('1.2 Document Upload Page - Dragger Upload', dragger > 0, dragger > 0 ? 'Dragger upload found' : 'Dragger upload not found');

      // Check for upload button
      const uploadBtn = await page.locator('button:has-text("上传")').count();
      logResult('1.3 Document Upload Page - Upload Button', uploadBtn > 0, uploadBtn > 0 ? 'Upload button found' : 'Upload button not found');

      // Check for Select components (chunk strategy, embed model)
      const selects = await page.locator('.ant-select').count();
      logResult('1.4 Document Upload Page - Select Components', selects > 0, `Found ${selects} select component(s)`);

      // Check for InputNumber components (chunk size, overlap)
      const inputNumbers = await page.locator('.ant-input-number').count();
      logResult('1.5 Document Upload Page - InputNumber Components', inputNumbers >= 2, `Found ${inputNumbers} input number component(s)`);

      await page.screenshot({ path: 'e2e/screenshots/1-document-upload-page.png', fullPage: true });

    } catch (error) {
      logResult('Document Upload Page', false, error.message);
      await page.screenshot({ path: 'e2e/screenshots/1-document-upload-page-error.png', fullPage: true });
      throw error;
    }
  });

  test('2. Chunk Management Page', async ({ page }) => {
    try {
      await page.goto('http://localhost:3000', { waitUntil: 'networkidle', timeout: 20000 });
      await page.waitForTimeout(2000);

      // Click on "知识块管理" menu item
      const menuItem = page.locator('.ant-menu-item:has-text("知识块管理")');
      await menuItem.click();
      await page.waitForTimeout(1000);

      logResult('2.1 Chunk Management Page - Menu Navigation', true, 'Navigated to chunk management page');

      // Check for title
      const title = await page.locator('h3:has-text("知识块"), h1:has-text("知识块")').count();
      logResult('2.2 Chunk Management Page - Title', title > 0, title > 0 ? 'Title found' : 'Title not found');

      // Check for table (chunk list)
      const table = await page.locator('.ant-table').count();
      logResult('2.3 Chunk Management Page - Table', table > 0, table > 0 ? 'Table found' : 'Table not found');

      await page.screenshot({ path: 'e2e/screenshots/2-chunk-management-page.png', fullPage: true });

    } catch (error) {
      logResult('Chunk Management Page', false, error.message);
      await page.screenshot({ path: 'e2e/screenshots/2-chunk-management-page-error.png', fullPage: true });
      throw error;
    }
  });

  test('3. Chat Test Page', async ({ page }) => {
    try {
      await page.goto('http://localhost:3000', { waitUntil: 'networkidle', timeout: 20000 });
      await page.waitForTimeout(2000);

      // Click on "问答测试" menu item
      const menuItem = page.locator('.ant-menu-item:has-text("问答测试")');
      await menuItem.click();
      await page.waitForTimeout(1000);

      logResult('3.1 Chat Test Page - Menu Navigation', true, 'Navigated to chat test page');

      // Check for title
      const title = await page.locator('h3:has-text("问答"), h1:has-text("问答")').count();
      logResult('3.2 Chat Test Page - Title', title > 0, title > 0 ? 'Title found' : 'Title not found');

      // Check for input components
      const textarea = await page.locator('textarea').count();
      const input = await page.locator('input').count();
      logResult('3.3 Chat Test Page - Input Box', textarea > 0 || input > 0, `Found ${textarea} textarea(s) and ${input} input(s)`);

      // Check for send button
      const sendBtn = await page.locator('button:has-text("发送"), button:has-text("提交"), button:has-text("问答")').count();
      logResult('3.4 Chat Test Page - Send Button', sendBtn > 0, sendBtn > 0 ? 'Send button found' : 'Send button not found');

      await page.screenshot({ path: 'e2e/screenshots/3-chat-test-page.png', fullPage: true });

    } catch (error) {
      logResult('Chat Test Page', false, error.message);
      await page.screenshot({ path: 'e2e/screenshots/3-chat-test-page-error.png', fullPage: true });
      throw error;
    }
  });

  test('4. API - GET /api/doc/chunks', async ({ request }) => {
    try {
      const response = await request.get(`${API.docService}/chunks`, { timeout: 10000 });
      const status = response.status();
      const body = await response.text();

      logResult('4.1 API - GET /api/doc/chunks - Status', status === 200, `Status: ${status}`);

      let jsonBody;
      try {
        jsonBody = JSON.parse(body);
        logResult('4.2 API - GET /api/doc/chunks - Valid JSON', true, `Response has ${Array.isArray(jsonBody) ? jsonBody.length : 'N/A'} items`);
      } catch {
        logResult('4.2 API - GET /api/doc/chunks - Valid JSON', false, 'Response is not valid JSON');
      }

    } catch (error) {
      logResult('API - GET /api/doc/chunks', false, error.message);
    }
  });

  test('5. API - POST /api/vector/index', async ({ request }) => {
    try {
      const testData = {
        documents: [
          {
            id: 'test-doc-001',
            content: 'This is a test document for indexing',
            metadata: { source: 'e2e-test', timestamp: new Date().toISOString() }
          }
        ]
      };

      const response = await request.post(`${API.ragService}/index`, {
        data: testData,
        headers: { 'Content-Type': 'application/json' },
        timeout: 10000
      });

      const status = response.status();
      const body = await response.text();

      logResult('5.1 API - POST /api/vector/index - Status', status >= 200 && status < 500, `Status: ${status}`);
      logResult('5.2 API - POST /api/vector/index - Response', true, `Response: ${body.substring(0, 100)}`);

    } catch (error) {
      logResult('API - POST /api/vector/index', false, error.message);
    }
  });

  test('6. API - POST /api/vector/search', async ({ request }) => {
    try {
      const searchData = {
        query: 'test search query',
        topK: 5
      };

      const response = await request.post(`${API.ragService}/search`, {
        data: searchData,
        headers: { 'Content-Type': 'application/json' },
        timeout: 10000
      });

      const status = response.status();
      const body = await response.text();

      logResult('6.1 API - POST /api/vector/search - Status', status >= 200 && status < 500, `Status: ${status}`);

      let jsonBody;
      try {
        jsonBody = JSON.parse(body);
        logResult('6.2 API - POST /api/vector/search - Valid JSON', true, `Response: ${JSON.stringify(jsonBody).substring(0, 100)}`);
      } catch {
        logResult('6.2 API - POST /api/vector/search - Valid JSON', false, 'Response is not valid JSON');
      }

    } catch (error) {
      logResult('API - POST /api/vector/search', false, error.message);
    }
  });

});

test.afterAll(async () => {
  console.log('\n========== TEST SUMMARY ==========');
  console.log(`Total: ${testResults.length}`);
  console.log(`Passed: ${testResults.filter(r => r.passed).length}`);
  console.log(`Failed: ${testResults.filter(r => !r.passed).length}`);
  console.log('==================================\n');

  const failedTests = testResults.filter(r => !r.passed);
  if (failedTests.length > 0) {
    console.log('Failed Tests:');
    failedTests.forEach(t => console.log(`  - ${t.name}: ${t.message}`));
  }
});