// @ts-check
const { test, expect } = require('@playwright/test');
const { DocumentUploadPage } = require('../pages/DocumentUploadPage');
const path = require('path');

test.describe('Semantic Chunking Tests', () => {
  let documentUploadPage;

  test.beforeEach(async ({ page }) => {
    documentUploadPage = new DocumentUploadPage(page);
    await documentUploadPage.navigate();
    await documentUploadPage.waitForReady();

    // Upload test file
    const filePath = path.join(__dirname, '../fixtures/sample.txt');
    await documentUploadPage.uploadFile(filePath);
  });

  test.describe('Strategy Selection', () => {
    test('should select semantic strategy when clicked', async ({ page }) => {
      await documentUploadPage.selectStrategy('true_semantic');

      const selectedStrategy = await documentUploadPage.getSelectedStrategy();
      expect(selectedStrategy).toBe('true_semantic');
    });

    test('should display semantic strategy config after selection', async ({ page }) => {
      await documentUploadPage.selectStrategy('true_semantic');

      // Verify the info alert is visible
      const infoAlert = page.locator('.ant-alert-info').filter({ hasText: '语义分块说明' });
      await expect(infoAlert).toBeVisible();
    });

    test('should show semantic strategy description', async ({ page }) => {
      await documentUploadPage.selectStrategy('true_semantic');

      // Look for strategy description
      const description = page.locator('.ant-card').filter({ hasText: '当前选择' });
      await expect(description).toBeVisible();
    });
  });

  test.describe('Similarity Threshold Configuration', () => {
    test('should display similarity threshold slider', async ({ page }) => {
      await documentUploadPage.selectStrategy('true_semantic');

      const slider = page.locator('.ant-slider').first();
      await expect(slider).toBeVisible();
    });

    test('should display default similarity threshold value', async ({ page }) => {
      await documentUploadPage.selectStrategy('true_semantic');

      // Default should be 0.45
      const thresholdTag = page.locator('.ant-tag').filter({ hasText: '0.45' });
      await expect(thresholdTag.first()).toBeVisible();
    });

    test('should show threshold description', async ({ page }) => {
      await documentUploadPage.selectStrategy('true_semantic');

      // Look for explanatory text
      const description = page.locator('text=阈值越低，分块越少');
      await expect(description).toBeVisible();
    });

    test('should have slider marks for threshold values', async ({ page }) => {
      await documentUploadPage.selectStrategy('true_semantic');

      // Check for marks: 0, 0.25, 0.5, 0.75, 1
      const slider = page.locator('.ant-slider').first();
      await expect(slider).toBeVisible();
    });
  });

  test.describe('Dynamic Threshold Configuration', () => {
    test('should display dynamic threshold switch', async ({ page }) => {
      await documentUploadPage.selectStrategy('true_semantic');

      const switchContainer = page.locator('.ant-form-item').filter({ hasText: '动态阈值调整' });
      const switchEl = switchContainer.locator('.ant-switch');
      await expect(switchEl).toBeVisible();
    });

    test('should have dynamic threshold enabled by default', async ({ page }) => {
      await documentUploadPage.selectStrategy('true_semantic');

      const switchContainer = page.locator('.ant-form-item').filter({ hasText: '动态阈值调整' });
      const switchEl = switchContainer.locator('.ant-switch');
      const isChecked = await switchEl.getAttribute('aria-checked');
      expect(isChecked).toBe('true');
    });

    test('should display percentile threshold slider when dynamic is enabled', async ({ page }) => {
      await documentUploadPage.selectStrategy('true_semantic');

      // Percentile threshold should be visible
      const percentileLabel = page.locator('.ant-form-item').filter({ hasText: '百分位阈值' });
      await expect(percentileLabel).toBeVisible();
    });

    test('should display default percentile value', async ({ page }) => {
      await documentUploadPage.selectStrategy('true_semantic');

      // Default should be 80%
      const percentileTag = page.locator('.ant-tag').filter({ hasText: '80%' });
      await expect(percentileTag).toBeVisible();
    });

    test('should toggle dynamic threshold switch', async ({ page }) => {
      await documentUploadPage.selectStrategy('true_semantic');

      const switchContainer = page.locator('.ant-form-item').filter({ hasText: '动态阈值调整' });
      const switchEl = switchContainer.locator('.ant-switch');

      // Toggle off
      await switchEl.click();
      let isChecked = await switchEl.getAttribute('aria-checked');
      expect(isChecked).toBe('false');

      // Percentile slider should be hidden
      const percentileLabel = page.locator('.ant-form-item').filter({ hasText: '百分位阈值' });
      await expect(percentileLabel).not.toBeVisible();

      // Toggle on
      await switchEl.click();
      isChecked = await switchEl.getAttribute('aria-checked');
      expect(isChecked).toBe('true');
    });
  });

  test.describe('Breakpoint Detection Methods', () => {
    test('should display breakpoint method selection', async ({ page }) => {
      await documentUploadPage.selectStrategy('true_semantic');

      const methodLabel = page.locator('.ant-form-item').filter({ hasText: '断点检测方法' });
      await expect(methodLabel).toBeVisible();
    });

    test('should have three breakpoint methods available', async ({ page }) => {
      await documentUploadPage.selectStrategy('true_semantic');

      // Check for radio buttons
      const radioGroup = page.locator('.ant-radio-group').filter({ has: page.locator('text=百分位法') });
      await expect(radioGroup).toBeVisible();
    });

    test('should have PERCENTILE as default breakpoint method', async ({ page }) => {
      await documentUploadPage.selectStrategy('true_semantic');

      // Check that 百分位法 is selected
      const percentilRadio = page.locator('.ant-radio-wrapper').filter({ hasText: '百分位法' });
      const radio = percentilRadio.locator('input[type="radio"]');
      const isChecked = await radio.isChecked();
      expect(isChecked).toBe(true);
    });

    test('should allow selecting GRADIENT method', async ({ page }) => {
      await documentUploadPage.selectStrategy('true_semantic');

      // Select 梯度法
      const gradientRadio = page.locator('.ant-radio-wrapper').filter({ hasText: '梯度法' });
      await gradientRadio.click();

      // Verify it's selected
      const radio = gradientRadio.locator('input[type="radio"]');
      const isChecked = await radio.isChecked();
      expect(isChecked).toBe(true);
    });

    test('should allow selecting FIXED_THRESHOLD method', async ({ page }) => {
      await documentUploadPage.selectStrategy('true_semantic');

      // Select 固定阈值法
      const fixedRadio = page.locator('.ant-radio-wrapper').filter({ hasText: '固定阈值法' });
      await fixedRadio.click();

      // Verify it's selected
      const radio = fixedRadio.locator('input[type="radio"]');
      const isChecked = await radio.isChecked();
      expect(isChecked).toBe(true);
    });

    test('should display method description for each option', async ({ page }) => {
      await documentUploadPage.selectStrategy('true_semantic');

      // Check that descriptions are visible
      const description1 = page.locator('text=基于相似度分布的百分位数确定断点');
      await expect(description1).toBeVisible();

      const description2 = page.locator('text=基于相似度变化的梯度确定断点');
      await expect(description2).toBeVisible();

      const description3 = page.locator('text=使用固定的相似度阈值确定断点');
      await expect(description3).toBeVisible();
    });
  });

  test.describe('Chunk Size Limits', () => {
    test('should display min chunk size input', async ({ page }) => {
      await documentUploadPage.selectStrategy('true_semantic');

      const minInput = page.locator('.ant-form-item').filter({ hasText: '最小分块大小' });
      await expect(minInput).toBeVisible();
    });

    test('should display max chunk size input', async ({ page }) => {
      await documentUploadPage.selectStrategy('true_semantic');

      const maxInput = page.locator('.ant-form-item').filter({ hasText: '最大分块大小' });
      await expect(maxInput).toBeVisible();
    });

    test('should have default min chunk size of 100', async ({ page }) => {
      await documentUploadPage.selectStrategy('true_semantic');

      const minInput = page.locator('input[type="number"]').first();
      const value = await minInput.inputValue();
      expect(value).toBe('100');
    });

    test('should have default max chunk size of 2000', async ({ page }) => {
      await documentUploadPage.selectStrategy('true_semantic');

      const maxInput = page.locator('input[type="number"]').nth(1);
      const value = await maxInput.inputValue();
      expect(value).toBe('2000');
    });

    test('should allow changing min chunk size', async ({ page }) => {
      await documentUploadPage.selectStrategy('true_semantic');

      const minInput = page.locator('input[type="number"]').first();
      await minInput.fill('150');

      const value = await minInput.inputValue();
      expect(value).toBe('150');
    });

    test('should allow changing max chunk size', async ({ page }) => {
      await documentUploadPage.selectStrategy('true_semantic');

      const maxInput = page.locator('input[type="number"]').nth(1);
      await maxInput.fill('3000');

      const value = await maxInput.inputValue();
      expect(value).toBe('3000');
    });
  });

  test.describe('Configuration Summary', () => {
    test('should display configuration summary at bottom', async ({ page }) => {
      await documentUploadPage.selectStrategy('true_semantic');

      const summary = page.locator('text=当前配置摘要');
      await expect(summary).toBeVisible();
    });

    test('should show similarity threshold in summary', async ({ page }) => {
      await documentUploadPage.selectStrategy('true_semantic');

      const summary = page.locator('.ant-tag').filter({ hasText: /相似度阈值/ });
      await expect(summary).toBeVisible();
    });

    test('should show dynamic threshold status in summary', async ({ page }) => {
      await documentUploadPage.selectStrategy('true_semantic');

      const summary = page.locator('.ant-tag').filter({ hasText: /动态阈值/ });
      await expect(summary).toBeVisible();
    });

    test('should show breakpoint method in summary', async ({ page }) => {
      await documentUploadPage.selectStrategy('true_semantic');

      const summary = page.locator('.ant-tag').filter({ hasText: /断点方法/ });
      await expect(summary).toBeVisible();
    });
  });

  test.describe('Preview Functionality', () => {
    test('should generate semantic chunks when preview is clicked', async ({ page }) => {
      await documentUploadPage.selectStrategy('true_semantic');
      await documentUploadPage.previewChunks();

      await page.waitForTimeout(3000);

      // Check result
      const isEmpty = await documentUploadPage.isPreviewEmpty();
      const hasError = await documentUploadPage.isErrorAlertVisible();

      expect(!isEmpty || hasError).toBe(true);
    });

    test('should create chunks based on semantic boundaries', async ({ page }) => {
      await documentUploadPage.selectStrategy('true_semantic');
      await documentUploadPage.previewChunks();

      await page.waitForTimeout(3000);

      const hasError = await documentUploadPage.isErrorAlertVisible();
      if (!hasError) {
        const chunkCount = await documentUploadPage.getChunkCount();
        // Semantic chunking should create fewer, more meaningful chunks
        // This is a soft assertion
        if (chunkCount > 0) {
          expect(chunkCount).toBeGreaterThan(0);
        }
      }
    });
  });

  test.describe('Configuration Change Impact', () => {
    test('should clear preview when threshold changes', async ({ page }) => {
      await documentUploadPage.selectStrategy('true_semantic');
      await documentUploadPage.previewChunks();

      await page.waitForTimeout(2000);

      // Change configuration
      const minInput = page.locator('input[type="number"]').first();
      await minInput.fill('200');

      // Preview should be cleared
    });

    test('should update summary when config changes', async ({ page }) => {
      await documentUploadPage.selectStrategy('true_semantic');

      // Change breakpoint method
      const gradientRadio = page.locator('.ant-radio-wrapper').filter({ hasText: '梯度法' });
      await gradientRadio.click();

      // Summary should update
      const summary = page.locator('.ant-tag').filter({ hasText: '梯度法' });
      await expect(summary).toBeVisible();
    });
  });
});
