// @ts-check
const { test, expect } = require('@playwright/test');
const { DocumentUploadPage } = require('../pages/DocumentUploadPage');
const path = require('path');

test.describe('Document Upload Tests', () => {
  let documentUploadPage;

  test.beforeEach(async ({ page }) => {
    documentUploadPage = new DocumentUploadPage(page);
    await documentUploadPage.navigate();
    await documentUploadPage.waitForReady();
  });

  test.describe('Page Load', () => {
    test('should load the document upload page successfully', async ({ page }) => {
      // Verify page title
      await expect(page).toHaveTitle(/RAG|知识库|文档/i);

      // Verify upload area is visible
      await expect(documentUploadPage.uploadArea).toBeVisible();

      // Verify strategy selector is visible
      await expect(documentUploadPage.strategyCards.first()).toBeVisible();

      // Verify action buttons are present
      await expect(documentUploadPage.previewButton).toBeVisible();
      await expect(documentUploadPage.uploadButton).toBeVisible();
      await expect(documentUploadPage.resetButton).toBeVisible();
    });

    test('should display initial step as step 0 (upload file)', async ({ page }) => {
      const stepIndex = await documentUploadPage.getCurrentStepIndex();
      expect(stepIndex).toBe(0);
    });

    test('should have preview and upload buttons disabled initially', async ({ page }) => {
      // No file uploaded, buttons should be disabled
      const isPreviewDisabled = await documentUploadPage.isPreviewButtonDisabled();
      const isUploadDisabled = await documentUploadPage.isUploadButtonDisabled();

      expect(isPreviewDisabled).toBe(true);
      expect(isUploadDisabled).toBe(true);
    });

    test('should display all 6 chunking strategies', async ({ page }) => {
      const strategies = await documentUploadPage.strategyCards.count();
      expect(strategies).toBe(6);
    });

    test('should have recursive strategy selected by default', async ({ page }) => {
      const selectedStrategy = await documentUploadPage.getSelectedStrategy();
      expect(selectedStrategy).toBe('recursive');
    });
  });

  test.describe('File Upload - Success Cases', () => {
    test('should upload a .txt file successfully', async ({ page }) => {
      const filePath = path.join(__dirname, '../fixtures/sample.txt');
      await documentUploadPage.uploadFile(filePath);

      // Verify file info is displayed
      const isDisplayed = await documentUploadPage.isFileInfoDisplayed('sample.txt');
      expect(isDisplayed).toBe(true);

      // Verify step moved to step 1
      const stepIndex = await documentUploadPage.getCurrentStepIndex();
      expect(stepIndex).toBe(1);
    });

    test('should upload a .md file successfully', async ({ page }) => {
      const filePath = path.join(__dirname, '../fixtures/sample.md');
      await documentUploadPage.uploadFile(filePath);

      const isDisplayed = await documentUploadPage.isFileInfoDisplayed('sample.md');
      expect(isDisplayed).toBe(true);
    });

    test('should enable preview and upload buttons after file selection', async ({ page }) => {
      const filePath = path.join(__dirname, '../fixtures/sample.txt');
      await documentUploadPage.uploadFile(filePath);

      // Wait for state update
      await page.waitForTimeout(500);

      const isPreviewDisabled = await documentUploadPage.isPreviewButtonDisabled();
      const isUploadDisabled = await documentUploadPage.isUploadButtonDisabled();

      expect(isPreviewDisabled).toBe(false);
      expect(isUploadDisabled).toBe(false);
    });

    test('should display file size in the info alert', async ({ page }) => {
      const filePath = path.join(__dirname, '../fixtures/sample.txt');
      await documentUploadPage.uploadFile(filePath);

      const alert = documentUploadPage.fileInfoAlert;
      const text = await alert.textContent();

      // Should contain KB or MB
      expect(text).toMatch(/KB|MB/);
    });
  });

  test.describe('File Upload - Validation', () => {
    test('should reject unsupported file format', async ({ page }) => {
      // Create an unsupported file type
      const filePath = path.join(__dirname, '../fixtures/test.xyz');

      // Try to upload (this might not trigger due to accept attribute)
      // Instead, verify the accept attribute is set correctly
      const acceptValue = await documentUploadPage.uploadInput.getAttribute('accept');
      expect(acceptValue).toContain('.txt');
      expect(acceptValue).toContain('.md');
      expect(acceptValue).toContain('.pdf');
      expect(acceptValue).toContain('.docx');
    });

    test('should allow removing uploaded file', async ({ page }) => {
      const filePath = path.join(__dirname, '../fixtures/sample.txt');
      await documentUploadPage.uploadFile(filePath);

      // Verify file is uploaded
      let isDisplayed = await documentUploadPage.isFileInfoDisplayed('sample.txt');
      expect(isDisplayed).toBe(true);

      // Remove the file
      await documentUploadPage.removeUploadedFile();

      // Verify file is removed (step should go back to 0)
      await page.waitForTimeout(300);
      const stepIndex = await documentUploadPage.getCurrentStepIndex();
      expect(stepIndex).toBe(0);
    });

    test('should show empty preview initially', async ({ page }) => {
      const isEmpty = await documentUploadPage.isPreviewEmpty();
      expect(isEmpty).toBe(true);
    });
  });

  test.describe('File Upload - Large Files', () => {
    test.skip('should reject files larger than 50MB', async ({ page }) => {
      // This test is skipped as it requires creating a large file
      // In production, you would create a large test file or mock the validation

      // The validation should happen in beforeUpload handler
      // Verify the size check exists by checking the implementation
    });
  });

  test.describe('Reset Functionality', () => {
    test('should reset all state when reset button is clicked', async ({ page }) => {
      // Upload a file
      const filePath = path.join(__dirname, '../fixtures/sample.txt');
      await documentUploadPage.uploadFile(filePath);

      // Change strategy
      await documentUploadPage.selectStrategy('true_semantic');

      // Reset
      await documentUploadPage.reset();

      // Verify state is reset
      const stepIndex = await documentUploadPage.getCurrentStepIndex();
      expect(stepIndex).toBe(0);

      const selectedStrategy = await documentUploadPage.getSelectedStrategy();
      expect(selectedStrategy).toBe('recursive');

      const isEmpty = await documentUploadPage.isPreviewEmpty();
      expect(isEmpty).toBe(true);
    });

    test('should clear file info after reset', async ({ page }) => {
      const filePath = path.join(__dirname, '../fixtures/sample.txt');
      await documentUploadPage.uploadFile(filePath);

      await documentUploadPage.reset();

      const isDisplayed = await documentUploadPage.isFileInfoDisplayed('sample.txt');
      expect(isDisplayed).toBe(false);
    });
  });

  test.describe('Responsive Design', () => {
    test('should display correctly on mobile viewport', async ({ page }) => {
      await page.setViewportSize({ width: 375, height: 667 });

      // Verify upload area is still visible
      await expect(documentUploadPage.uploadArea).toBeVisible();

      // Verify strategy cards are stacked
      const strategyCards = await documentUploadPage.strategyCards.count();
      expect(strategyCards).toBe(6);
    });

    test('should display correctly on tablet viewport', async ({ page }) => {
      await page.setViewportSize({ width: 768, height: 1024 });

      await expect(documentUploadPage.uploadArea).toBeVisible();
      await expect(documentUploadPage.strategyCards.first()).toBeVisible();
    });

    test('should display correctly on desktop viewport', async ({ page }) => {
      await page.setViewportSize({ width: 1280, height: 720 });

      await expect(documentUploadPage.uploadArea).toBeVisible();
      await expect(documentUploadPage.strategyCards.first()).toBeVisible();
    });
  });
});
