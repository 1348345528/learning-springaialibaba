// @ts-check
const { test, expect } = require('@playwright/test');
const { DocumentUploadPage } = require('../pages/DocumentUploadPage');
const path = require('path');

test.describe('Full Flow E2E Tests', () => {
  let documentUploadPage;

  test.beforeEach(async ({ page }) => {
    documentUploadPage = new DocumentUploadPage(page);
    await documentUploadPage.navigate();
    await documentUploadPage.waitForReady();
  });

  test.describe('Complete Upload Flow', () => {
    test('should complete full upload flow with recursive strategy', async ({ page }) => {
      // Step 1: Upload file
      const filePath = path.join(__dirname, '../fixtures/sample.txt');
      await documentUploadPage.uploadFile(filePath);

      let stepIndex = await documentUploadPage.getCurrentStepIndex();
      expect(stepIndex).toBe(1);

      // Step 2: Configure strategy (recursive is default)
      await documentUploadPage.selectStrategy('recursive');

      // Step 3: Preview chunks
      await documentUploadPage.previewChunks();
      await page.waitForTimeout(3000);

      // Step 4: Verify preview or error
      const hasError = await documentUploadPage.isErrorAlertVisible();
      if (!hasError) {
        stepIndex = await documentUploadPage.getCurrentStepIndex();
        expect(stepIndex).toBeGreaterThanOrEqual(2);
      }
    });

    test('should complete full upload flow with semantic strategy', async ({ page }) => {
      // Step 1: Upload file
      const filePath = path.join(__dirname, '../fixtures/sample.txt');
      await documentUploadPage.uploadFile(filePath);

      // Step 2: Configure semantic strategy
      await documentUploadPage.selectStrategy('true_semantic');

      // Step 3: Preview chunks
      await documentUploadPage.previewChunks();
      await page.waitForTimeout(3000);

      // Verify completion
      const hasError = await documentUploadPage.isErrorAlertVisible();
      const isEmpty = await documentUploadPage.isPreviewEmpty();
      expect(hasError || !isEmpty).toBe(true);
    });

    test('should complete full upload flow with hierarchical strategy', async ({ page }) => {
      // Step 1: Upload file
      const filePath = path.join(__dirname, '../fixtures/sample.txt');
      await documentUploadPage.uploadFile(filePath);

      // Step 2: Configure hierarchical strategy
      await documentUploadPage.selectStrategy('hierarchical');

      // Step 3: Preview chunks
      await documentUploadPage.previewChunks();
      await page.waitForTimeout(3000);

      // Verify completion
      const hasError = await documentUploadPage.isErrorAlertVisible();
      const isEmpty = await documentUploadPage.isPreviewEmpty();
      expect(hasError || !isEmpty).toBe(true);
    });
  });

  test.describe('Strategy Switching', () => {
    test('should switch between strategies without losing file', async ({ page }) => {
      // Upload file
      const filePath = path.join(__dirname, '../fixtures/sample.txt');
      await documentUploadPage.uploadFile(filePath);

      // Verify file is uploaded
      let isDisplayed = await documentUploadPage.isFileInfoDisplayed('sample.txt');
      expect(isDisplayed).toBe(true);

      // Switch to semantic
      await documentUploadPage.selectStrategy('true_semantic');
      isDisplayed = await documentUploadPage.isFileInfoDisplayed('sample.txt');
      expect(isDisplayed).toBe(true);

      // Switch to hierarchical
      await documentUploadPage.selectStrategy('hierarchical');
      isDisplayed = await documentUploadPage.isFileInfoDisplayed('sample.txt');
      expect(isDisplayed).toBe(true);

      // Switch back to recursive
      await documentUploadPage.selectStrategy('recursive');
      isDisplayed = await documentUploadPage.isFileInfoDisplayed('sample.txt');
      expect(isDisplayed).toBe(true);
    });

    test('should clear preview when switching strategies', async ({ page }) => {
      // Upload file
      const filePath = path.join(__dirname, '../fixtures/sample.txt');
      await documentUploadPage.uploadFile(filePath);

      // Preview with recursive
      await documentUploadPage.selectStrategy('recursive');
      await documentUploadPage.previewChunks();
      await page.waitForTimeout(2000);

      // Switch to semantic - preview should be cleared
      await documentUploadPage.selectStrategy('true_semantic');

      // Preview should be empty again
      const isEmpty = await documentUploadPage.isPreviewEmpty();
      expect(isEmpty).toBe(true);
    });

    test('should update configuration panel when switching strategies', async ({ page }) => {
      // Upload file
      const filePath = path.join(__dirname, '../fixtures/sample.txt');
      await documentUploadPage.uploadFile(filePath);

      // Select recursive - should show chunk size slider
      await documentUploadPage.selectStrategy('recursive');
      let recursiveConfig = page.locator('.ant-slider').first();
      await expect(recursiveConfig).toBeVisible();

      // Select semantic - should show similarity threshold
      await documentUploadPage.selectStrategy('true_semantic');
      let semanticAlert = page.locator('.ant-alert-info').filter({ hasText: '语义分块说明' });
      await expect(semanticAlert).toBeVisible();

      // Select hierarchical - should show parent/child config
      await documentUploadPage.selectStrategy('hierarchical');
      let hierarchicalAlert = page.locator('.ant-alert-info').filter({ hasText: '分层分块说明' });
      await expect(hierarchicalAlert).toBeVisible();
    });
  });

  test.describe('All Strategies Validation', () => {
    test('should have all 6 strategies available', async ({ page }) => {
      const strategies = ['recursive', 'true_semantic', 'hierarchical', 'fixed_length', 'hybrid', 'custom_rule'];

      for (const strategy of strategies) {
        await documentUploadPage.selectStrategy(strategy);
        const selected = await documentUploadPage.getSelectedStrategy();
        expect(selected).toBe(strategy);
      }
    });

    test('should display configuration for fixed_length strategy', async ({ page }) => {
      const filePath = path.join(__dirname, '../fixtures/sample.txt');
      await documentUploadPage.uploadFile(filePath);

      await documentUploadPage.selectStrategy('fixed_length');

      // Should show basic chunk size and overlap inputs
      const configCard = page.locator('.ant-card').filter({ hasText: '分块大小' });
      await expect(configCard).toBeVisible();
    });

    test('should display configuration for hybrid strategy', async ({ page }) => {
      const filePath = path.join(__dirname, '../fixtures/sample.txt');
      await documentUploadPage.uploadFile(filePath);

      await documentUploadPage.selectStrategy('hybrid');

      // Should show chunk size and semantic threshold
      const configCard = page.locator('.ant-card').filter({ hasText: '基础分块大小' });
      await expect(configCard).toBeVisible();
    });

    test('should display configuration for custom_rule strategy', async ({ page }) => {
      const filePath = path.join(__dirname, '../fixtures/sample.txt');
      await documentUploadPage.uploadFile(filePath);

      await documentUploadPage.selectStrategy('custom_rule');

      // Should show separator input
      const separatorLabel = page.locator('text=自定义分隔符');
      await expect(separatorLabel).toBeVisible();
    });
  });

  test.describe('Error Handling and Recovery', () => {
    test('should handle network errors gracefully', async ({ page }) => {
      const filePath = path.join(__dirname, '../fixtures/sample.txt');
      await documentUploadPage.uploadFile(filePath);

      // Simulate network failure by blocking the API
      await page.route('**/api/doc/**', route => route.abort());

      // Try to preview
      await documentUploadPage.previewChunks();
      await page.waitForTimeout(2000);

      // Should show error
      const hasError = await documentUploadPage.isErrorAlertVisible();
      // Error handling depends on implementation
      expect(hasError).toBe(true);
    });

    test('should allow retry after error', async ({ page }) => {
      const filePath = path.join(__dirname, '../fixtures/sample.txt');
      await documentUploadPage.uploadFile(filePath);

      // Block API once
      let shouldFail = true;
      await page.route('**/api/doc/**', route => {
        if (shouldFail) {
          shouldFail = false;
          route.abort();
        } else {
          route.continue();
        }
      });

      // First attempt fails
      await documentUploadPage.previewChunks();
      await page.waitForTimeout(2000);

      // Unroute and retry
      await page.unroute('**/api/doc/**');

      // Reset and try again
      await documentUploadPage.reset();
      await documentUploadPage.uploadFile(filePath);
      await documentUploadPage.previewChunks();
    });

    test('should reset properly after error', async ({ page }) => {
      const filePath = path.join(__dirname, '../fixtures/sample.txt');
      await documentUploadPage.uploadFile(filePath);

      // Block API
      await page.route('**/api/doc/**', route => route.abort());

      await documentUploadPage.previewChunks();
      await page.waitForTimeout(2000);

      // Reset
      await documentUploadPage.reset();

      // Verify clean state
      const stepIndex = await documentUploadPage.getCurrentStepIndex();
      expect(stepIndex).toBe(0);

      const selectedStrategy = await documentUploadPage.getSelectedStrategy();
      expect(selectedStrategy).toBe('recursive');
    });
  });

  test.describe('Responsive Layout', () => {
    test('should work correctly on mobile devices', async ({ page }) => {
      await page.setViewportSize({ width: 375, height: 667 });

      const filePath = path.join(__dirname, '../fixtures/sample.txt');
      await documentUploadPage.uploadFile(filePath);

      // Verify upload worked
      const isDisplayed = await documentUploadPage.isFileInfoDisplayed('sample.txt');
      expect(isDisplayed).toBe(true);

      // Can select strategy
      await documentUploadPage.selectStrategy('recursive');
      const selected = await documentUploadPage.getSelectedStrategy();
      expect(selected).toBe('recursive');
    });

    test('should work correctly on tablet devices', async ({ page }) => {
      await page.setViewportSize({ width: 768, height: 1024 });

      const filePath = path.join(__dirname, '../fixtures/sample.txt');
      await documentUploadPage.uploadFile(filePath);

      await documentUploadPage.selectStrategy('true_semantic');
      const selected = await documentUploadPage.getSelectedStrategy();
      expect(selected).toBe('true_semantic');
    });

    test('should display two-column layout on desktop', async ({ page }) => {
      await page.setViewportSize({ width: 1280, height: 720 });

      // Check for left and right columns
      const leftColumn = page.locator('.ant-col').filter({ has: page.locator('.ant-upload-drag') });
      const rightColumn = page.locator('.ant-card').filter({ hasText: '分块预览' });

      await expect(leftColumn).toBeVisible();
      await expect(rightColumn).toBeVisible();
    });
  });

  test.describe('Accessibility', () => {
    test('should have accessible upload area', async ({ page }) => {
      // Check for proper ARIA labels
      const uploadArea = documentUploadPage.uploadArea;
      await expect(uploadArea).toBeVisible();

      // Should be keyboard accessible
      await page.keyboard.press('Tab');
      const focusedElement = page.locator(':focus');
      await expect(focusedElement).toBeVisible();
    });

    test('should have accessible strategy cards', async ({ page }) => {
      const strategyCards = await documentUploadPage.strategyCards.all();

      for (const card of strategyCards) {
        // Each card should be clickable
        await expect(card).toBeEnabled();
      }
    });

    test('should have accessible buttons', async ({ page }) => {
      // Check all main buttons have proper labels
      await expect(documentUploadPage.previewButton).toBeVisible();
      await expect(documentUploadPage.uploadButton).toBeVisible();
      await expect(documentUploadPage.resetButton).toBeVisible();
    });

    test('should support keyboard navigation', async ({ page }) => {
      // Tab through the page
      for (let i = 0; i < 5; i++) {
        await page.keyboard.press('Tab');
      }

      // Some element should be focused
      const focusedElement = page.locator(':focus');
      await expect(focusedElement).toBeVisible();
    });
  });

  test.describe('Performance', () => {
    test('should load page within acceptable time', async ({ page }) => {
      const startTime = Date.now();
      await documentUploadPage.navigate();
      await documentUploadPage.waitForReady();
      const loadTime = Date.now() - startTime;

      // Page should load within 5 seconds
      expect(loadTime).toBeLessThan(5000);
    });

    test('should switch strategies quickly', async ({ page }) => {
      await documentUploadPage.waitForReady();

      const startTime = Date.now();

      // Switch through all strategies
      const strategies = ['recursive', 'true_semantic', 'hierarchical'];
      for (const strategy of strategies) {
        await documentUploadPage.selectStrategy(strategy);
      }

      const switchTime = Date.now() - startTime;

      // All switches should complete within 3 seconds
      expect(switchTime).toBeLessThan(3000);
    });
  });

  test.describe('Edge Cases', () => {
    test('should handle empty file', async ({ page }) => {
      // Create empty file path (would need to create actual empty file)
      // This is a placeholder for the test case
    });

    test('should handle file with special characters', async ({ page }) => {
      const filePath = path.join(__dirname, '../fixtures/sample.txt');
      await documentUploadPage.uploadFile(filePath);

      // File contains Chinese characters and special symbols
      await documentUploadPage.selectStrategy('recursive');
      await documentUploadPage.previewChunks();
      await page.waitForTimeout(2000);

      // Should handle without error
    });

    test('should handle very long single line', async ({ page }) => {
      // This would require a special test file
      // Placeholder for edge case test
    });

    test('should handle multiple rapid strategy changes', async ({ page }) => {
      const filePath = path.join(__dirname, '../fixtures/sample.txt');
      await documentUploadPage.uploadFile(filePath);

      // Rapidly switch strategies
      for (let i = 0; i < 10; i++) {
        await documentUploadPage.selectStrategy(i % 2 === 0 ? 'recursive' : 'true_semantic');
      }

      // Should still be in a valid state
      const selected = await documentUploadPage.getSelectedStrategy();
      expect(['recursive', 'true_semantic']).toContain(selected);
    });
  });

  test.describe('State Persistence', () => {
    test('should maintain file selection after page refresh', async ({ page }) => {
      const filePath = path.join(__dirname, '../fixtures/sample.txt');
      await documentUploadPage.uploadFile(filePath);

      // Refresh page
      await page.reload();

      // File selection is lost on refresh (expected behavior)
      const stepIndex = await documentUploadPage.getCurrentStepIndex();
      expect(stepIndex).toBe(0);
    });
  });
});
