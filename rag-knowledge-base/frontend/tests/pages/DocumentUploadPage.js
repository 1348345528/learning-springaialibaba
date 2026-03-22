/**
 * Page Object Model for Document Upload Page
 * Encapsulates all interactions with the RAG document upload and chunking interface
 */
class DocumentUploadPage {
  /**
   * @param {import('@playwright/test').Page} page
   */
  constructor(page) {
    this.page = page;

    // Navigation
    this.pageUrl = '/';

    // Upload area locators
    this.uploadArea = page.locator('.ant-upload-drag');
    this.uploadInput = page.locator('input[type="file"]');
    this.fileInfoAlert = page.locator('.ant-alert-info');
    this.uploadProgress = page.locator('.ant-progress');

    // Step indicator
    this.steps = page.locator('.ant-steps');
    this.currentStep = page.locator('.ant-steps-item-process');

    // Strategy selector locators
    this.strategyCards = page.locator('.strategy-card');
    this.selectedStrategyCard = page.locator('.strategy-card-selected');

    // Strategy names mapping
    this.strategies = {
      recursive: { name: 'recursive', label: '递归分块' },
      true_semantic: { name: 'true_semantic', label: '语义分块' },
      hierarchical: { name: 'hierarchical', label: '分层分块' },
      fixed_length: { name: 'fixed_length', label: '固定长度分块' },
      hybrid: { name: 'hybrid', label: '混合分块' },
      custom_rule: { name: 'custom_rule', label: '自定义规则分块' },
    };

    // Recursive config locators
    this.chunkSizeSlider = page.locator('.ant-slider').first();
    this.overlapSlider = page.locator('.ant-slider').nth(1);
    this.minChunkSizeInput = page.locator('input[type="number"]').first();
    this.keepSeparatorSwitch = page.locator('.ant-switch').first();
    this.advancedConfigCollapse = page.locator('.ant-collapse-header').first();

    // Semantic config locators
    this.similarityThresholdSlider = page.locator('.ant-slider').first();
    this.dynamicThresholdSwitch = page.locator('.ant-switch').first();
    this.breakpointMethodRadio = page.locator('.ant-radio-group').first();

    // Hierarchical config locators
    this.parentChunkSizeSlider = page.locator('.ant-slider').first();
    this.childChunkSizeSlider = page.locator('.ant-slider').nth(1);
    this.childSplitStrategyRadio = page.locator('.ant-radio-group');

    // Action buttons
    this.previewButton = page.getByRole('button', { name: /预览分块/ });
    this.uploadButton = page.getByRole('button', { name: /上传并处理/ });
    this.resetButton = page.getByRole('button', { name: /重置/ });

    // Preview area locators
    this.chunkPreviewArea = page.locator('.ant-card').filter({ hasText: '分块预览' });
    this.chunkCards = page.locator('[class*="ChunkCard"], .ant-card').filter({ has: page.locator('.ant-tag') });
    this.chunkCountTag = page.locator('.ant-tag').filter({ hasText: /个分块/ });
    this.expandAllButton = page.getByRole('button', { name: /全部展开|全部收起/ });
    this.emptyPreview = page.locator('.ant-empty');

    // Statistics locators
    this.statisticsCard = page.locator('.ant-card').filter({ hasText: /统计/ });
    this.totalChunksStat = page.locator('[data-testid="total-chunks"], .ant-statistic-content-value').first();

    // Success/Error alerts
    this.successAlert = page.locator('.ant-alert-success');
    this.errorAlert = page.locator('.ant-alert-error');
    this.errorMessage = page.locator('.ant-alert-description');

    // Loading spinner
    this.loadingSpinner = page.locator('.ant-spin-spinning');
  }

  /**
   * Navigate to the document upload page
   */
  async navigate() {
    await this.page.goto(this.pageUrl);
    await this.page.waitForLoadState('networkidle');
  }

  /**
   * Upload a file using the file input
   * @param {string} filePath - Path to the file to upload
   */
  async uploadFile(filePath) {
    await this.uploadInput.setInputFiles(filePath);
    // Wait for file info alert to appear
    await this.fileInfoAlert.waitFor({ state: 'visible', timeout: 5000 });
  }

  /**
   * Upload a file by dragging (simulated)
   * @param {string} filePath - Path to the file to upload
   */
  async uploadFileByDrag(filePath) {
    // Playwright doesn't support native drag and drop for files easily
    // Use the input method as fallback
    await this.uploadFile(filePath);
  }

  /**
   * Select a chunking strategy
   * @param {string} strategyKey - Key of the strategy (recursive, true_semantic, hierarchical, etc.)
   */
  async selectStrategy(strategyKey) {
    const strategy = this.strategies[strategyKey];
    if (!strategy) {
      throw new Error(`Unknown strategy: ${strategyKey}`);
    }

    // Click on the strategy card containing the strategy name
    const strategyCard = this.strategyCards.filter({ hasText: strategy.label });
    await strategyCard.click();

    // Wait for the card to be selected (border color change)
    await this.page.waitForTimeout(300);
  }

  /**
   * Get the currently selected strategy
   * @returns {Promise<string>} - The key of the selected strategy
   */
  async getSelectedStrategy() {
    const selectedCard = this.selectedStrategyCard;
    const text = await selectedCard.textContent();

    // Match strategy by label
    for (const [key, value] of Object.entries(this.strategies)) {
      if (text.includes(value.label)) {
        return key;
      }
    }
    return null;
  }

  /**
   * Configure recursive chunking parameters
   * @param {Object} config - Configuration object
   * @param {number} config.chunkSize - Chunk size in characters
   * @param {number} config.overlap - Overlap size in characters
   * @param {number} config.minChunkSize - Minimum chunk size
   * @param {boolean} config.keepSeparator - Whether to keep separators
   */
  async configureRecursive(config) {
    // Ensure recursive strategy is selected
    await this.selectStrategy('recursive');

    // Wait for config to load
    await this.page.waitForTimeout(300);

    // Set chunk size using slider (if needed)
    if (config.chunkSize !== undefined) {
      // Find the slider track and calculate position
      const slider = this.page.locator('.ant-slider').first();
      await slider.click();
    }

    // Set min chunk size using input
    if (config.minChunkSize !== undefined) {
      const input = this.page.locator('input[type="number"]').first();
      await input.fill(String(config.minChunkSize));
    }

    // Toggle keep separator switch
    if (config.keepSeparator !== undefined) {
      const switchEl = this.page.locator('.ant-switch').first();
      const isChecked = await switchEl.getAttribute('aria-checked');
      if ((isChecked === 'true') !== config.keepSeparator) {
        await switchEl.click();
      }
    }
  }

  /**
   * Configure semantic chunking parameters
   * @param {Object} config - Configuration object
   * @param {number} config.similarityThreshold - Similarity threshold (0-1)
   * @param {boolean} config.useDynamicThreshold - Whether to use dynamic threshold
   * @param {string} config.breakpointMethod - Breakpoint detection method
   */
  async configureSemantic(config) {
    // Ensure semantic strategy is selected
    await this.selectStrategy('true_semantic');

    // Wait for config to load
    await this.page.waitForTimeout(300);

    // Toggle dynamic threshold
    if (config.useDynamicThreshold !== undefined) {
      const switchEl = this.page.locator('.ant-switch').first();
      const isChecked = await switchEl.getAttribute('aria-checked');
      if ((isChecked === 'true') !== config.useDynamicThreshold) {
        await switchEl.click();
      }
    }

    // Select breakpoint method
    if (config.breakpointMethod !== undefined) {
      const methodLabels = {
        PERCENTILE: '百分位法',
        GRADIENT: '梯度法',
        FIXED_THRESHOLD: '固定阈值法',
      };
      const label = methodLabels[config.breakpointMethod];
      if (label) {
        const radio = this.page.getByText(label, { exact: false }).first();
        await radio.click();
      }
    }
  }

  /**
   * Configure hierarchical chunking parameters
   * @param {Object} config - Configuration object
   * @param {number} config.parentChunkSize - Parent chunk size
   * @param {number} config.childChunkSize - Child chunk size
   * @param {string} config.childSplitStrategy - Child split strategy
   */
  async configureHierarchical(config) {
    // Ensure hierarchical strategy is selected
    await this.selectStrategy('hierarchical');

    // Wait for config to load
    await this.page.waitForTimeout(300);

    // Select child split strategy
    if (config.childSplitStrategy !== undefined) {
      const strategyLabels = {
        RECURSIVE: '递归分割',
        SENTENCE: '句子分割',
        FIXED: '固定长度',
      };
      const label = strategyLabels[config.childSplitStrategy];
      if (label) {
        const radio = this.page.getByText(label, { exact: false }).first();
        await radio.click();
      }
    }
  }

  /**
   * Click the preview button to generate chunk preview
   */
  async previewChunks() {
    await this.previewButton.click();

    // Wait for loading to complete
    await this.loadingSpinner.waitFor({ state: 'hidden', timeout: 30000 });
  }

  /**
   * Click the upload button to upload and process document
   */
  async uploadAndProcess() {
    await this.uploadButton.click();

    // Wait for upload to complete (progress bar or success message)
    await this.page.waitForTimeout(1000);
  }

  /**
   * Click the reset button to clear all state
   */
  async reset() {
    await this.resetButton.click();
    await this.page.waitForTimeout(300);
  }

  /**
   * Get the number of chunks in the preview
   * @returns {Promise<number>} - Number of chunks
   */
  async getChunkCount() {
    const tag = this.chunkCountTag;
    if (await tag.isVisible()) {
      const text = await tag.textContent();
      const match = text.match(/(\d+)/);
      return match ? parseInt(match[1], 10) : 0;
    }
    return 0;
  }

  /**
   * Check if preview is empty
   * @returns {Promise<boolean>}
   */
  async isPreviewEmpty() {
    return await this.emptyPreview.isVisible();
  }

  /**
   * Get chunk content by index
   * @param {number} index - Zero-based index of the chunk
   * @returns {Promise<string>} - Chunk content
   */
  async getChunkContent(index) {
    const chunks = await this.page.locator('.ant-card').filter({ has: this.page.locator('.ant-badge') }).all();
    if (index < chunks.length) {
      return await chunks[index].textContent();
    }
    return null;
  }

  /**
   * Get all chunk contents
   * @returns {Promise<string[]>} - Array of chunk contents
   */
  async getAllChunkContents() {
    const chunks = await this.page.locator('.ant-card').filter({ has: this.page.locator('.ant-badge') }).all();
    const contents = [];
    for (const chunk of chunks) {
      contents.push(await chunk.textContent());
    }
    return contents;
  }

  /**
   * Expand all chunks in preview
   */
  async expandAllChunks() {
    const button = this.expandAllButton;
    const text = await button.textContent();
    if (text.includes('全部展开')) {
      await button.click();
    }
  }

  /**
   * Collapse all chunks in preview
   */
  async collapseAllChunks() {
    const button = this.expandAllButton;
    const text = await button.textContent();
    if (text.includes('全部收起')) {
      await button.click();
    }
  }

  /**
   * Check if success alert is visible
   * @returns {Promise<boolean>}
   */
  async isSuccessAlertVisible() {
    return await this.successAlert.isVisible();
  }

  /**
   * Check if error alert is visible
   * @returns {Promise<boolean>}
   */
  async isErrorAlertVisible() {
    return await this.errorAlert.isVisible();
  }

  /**
   * Get error message text
   * @returns {Promise<string>}
   */
  async getErrorMessage() {
    if (await this.errorMessage.isVisible()) {
      return await this.errorMessage.textContent();
    }
    return null;
  }

  /**
   * Get current step index (0-based)
   * @returns {Promise<number>}
   */
  async getCurrentStepIndex() {
    const stepItems = await this.page.locator('.ant-steps-item').all();
    for (let i = 0; i < stepItems.length; i++) {
      const classes = await stepItems[i].getAttribute('class');
      if (classes.includes('ant-steps-item-process')) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Wait for page to be ready
   */
  async waitForReady() {
    await this.page.waitForLoadState('networkidle');
    await this.uploadArea.waitFor({ state: 'visible' });
  }

  /**
   * Take a screenshot for debugging
   * @param {string} name - Screenshot name
   */
  async takeScreenshot(name) {
    await this.page.screenshot({ path: `test-results/screenshots/${name}.png`, fullPage: true });
  }

  /**
   * Check if file info alert shows correct file
   * @param {string} fileName - Expected file name
   * @returns {Promise<boolean>}
   */
  async isFileInfoDisplayed(fileName) {
    const alert = this.fileInfoAlert;
    if (await alert.isVisible()) {
      const text = await alert.textContent();
      return text.includes(fileName);
    }
    return false;
  }

  /**
   * Remove uploaded file
   */
  async removeUploadedFile() {
    const removeIcon = this.page.locator('.ant-upload-list-item-card-actions-remove');
    if (await removeIcon.isVisible()) {
      await removeIcon.click();
    }
  }

  /**
   * Get upload button disabled state
   * @returns {Promise<boolean>}
   */
  async isUploadButtonDisabled() {
    return await this.uploadButton.isDisabled();
  }

  /**
   * Get preview button disabled state
   * @returns {Promise<boolean>}
   */
  async isPreviewButtonDisabled() {
    return await this.previewButton.isDisabled();
  }
}

module.exports = { DocumentUploadPage };
