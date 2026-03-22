import React, { useState, useCallback } from 'react';
import {
  Card,
  Upload,
  Button,
  Select,
  Input,
  message,
  Progress,
  Space,
  Typography,
  Divider,
  Alert,
  Tabs,
  Spin,
  Row,
  Col,
  Steps,
  Tag,
} from 'antd';
import {
  InboxOutlined,
  FileTextOutlined,
  CheckCircleOutlined,
  EyeOutlined,
  CloudUploadOutlined,
  SettingOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import { documentApi, chunkApi } from '../services/api';

// 组件导入
import ChunkStrategySelector from '../components/ChunkStrategySelector';
import RecursiveChunkConfig from '../components/ChunkConfig/RecursiveChunkConfig';
import SemanticChunkConfig from '../components/ChunkConfig/SemanticChunkConfig';
import HierarchicalChunkConfig from '../components/ChunkConfig/HierarchicalChunkConfig';
import ChunkPreview from '../components/ChunkPreview';
import ChunkStatistics from '../components/ChunkStatistics';

const { Dragger } = Upload;
const { Text, Title, Paragraph } = Typography;
const { TextArea } = Input;

// 嵌入模型选项
const EMBED_MODELS = [
  { value: 'Qwen/Qwen3-Embedding-8B', label: 'Qwen3-Embedding-8B (4096维)' },
];

// 默认配置
const DEFAULT_CONFIGS = {
  recursive: {
    chunkSize: 500,
    overlap: 50,
    minChunkSize: 50,
    keepSeparator: true,
    separators: ['\n\n', '\n', '。', '！', '？', '；', '，', ' ', ''],
  },
  true_semantic: {
    similarityThreshold: 0.45,
    useDynamicThreshold: true,
    percentileThreshold: 0.8,
    breakpointMethod: 'PERCENTILE',
    minChunkSize: 100,
    maxChunkSize: 2000,
  },
  hierarchical: {
    parentChunkSize: 2000,
    parentOverlap: 200,
    childChunkSize: 200,
    childOverlap: 20,
    childSplitStrategy: 'RECURSIVE',
  },
  fixed_length: {
    chunkSize: 500,
    overlap: 50,
  },
  hybrid: {
    chunkSize: 500,
    overlap: 50,
    semanticThreshold: 0.5,
  },
  custom_rule: {
    separators: ['\n\n', '\n'],
    chunkSize: 500,
  },
};

const DocumentUpload = () => {
  // 状态管理
  const [currentStep, setCurrentStep] = useState(0);
  const [fileList, setFileList] = useState([]);
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);

  // 分块配置
  const [strategy, setStrategy] = useState('recursive');
  const [config, setConfig] = useState(DEFAULT_CONFIGS.recursive);
  const [embedModel, setEmbedModel] = useState('Qwen/Qwen3-Embedding-8B');
  const [customPrompt, setCustomPrompt] = useState('');

  // 预览相关
  const [previewChunks, setPreviewChunks] = useState([]);
  const [previewLoading, setPreviewLoading] = useState(false);
  const [previewError, setPreviewError] = useState(null);

  // 上传结果
  const [uploadResult, setUploadResult] = useState(null);

  // 上传前校验
  const beforeUpload = (file) => {
    const allowedTypes = [
      'text/plain',
      'text/markdown',
      'application/pdf',
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    ];
    const isAllowed =
      allowedTypes.some((type) => file.type.includes(type.split('/')[1])) ||
      file.name.endsWith('.md') ||
      file.name.endsWith('.txt') ||
      file.name.endsWith('.pdf') ||
      file.name.endsWith('.docx');

    if (!isAllowed) {
      message.error('仅支持 .txt, .md, .pdf, .docx 格式的文件');
      return false;
    }

    const isLt50M = file.size / 1024 / 1024 <= 50;
    if (!isLt50M) {
      message.error('文件大小不能超过 50MB');
      return false;
    }

    setFileList([file]);
    setUploadResult(null);
    setPreviewChunks([]);
    setPreviewError(null);
    setCurrentStep(1);
    return false;
  };

  // 处理策略变更
  const handleStrategyChange = useCallback((newStrategy) => {
    setStrategy(newStrategy);
    setConfig(DEFAULT_CONFIGS[newStrategy] || {});
    setPreviewChunks([]);
    setPreviewError(null);
  }, []);

  // 处理配置变更
  const handleConfigChange = useCallback((newConfig) => {
    setConfig(newConfig);
    setPreviewChunks([]);
    setPreviewError(null);
  }, []);

  // 预览分块
  const handlePreview = async () => {
    if (fileList.length === 0) {
      message.warning('请先选择文件');
      return;
    }

    setPreviewLoading(true);
    setPreviewError(null);
    setPreviewChunks([]);

    try {
      // 读取文件内容
      const file = fileList[0].originFileObj || fileList[0];
      const content = await readFileContent(file);

      // 构建 JSON 请求体
      const requestData = {
        content,
        strategy,
        ...config,
      };

      const result = await chunkApi.preview(requestData);

      if (result && result.chunks) {
        setPreviewChunks(result.chunks);
        setCurrentStep(2);
        message.success(`预览成功，共 ${result.chunks.length} 个分块`);
      } else {
        throw new Error('预览结果格式错误');
      }
    } catch (error) {
      console.error('预览失败:', error);
      setPreviewError(error.message || '预览失败，请重试');
      message.error(error.message || '预览失败，请重试');
    } finally {
      setPreviewLoading(false);
    }
  };

  // 读取文件内容
  const readFileContent = (file) => {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = (e) => resolve(e.target.result);
      reader.onerror = (e) => reject(new Error('读取文件失败'));
      reader.readAsText(file);
    });
  };

  // 处理上传
  const handleUpload = () => {
    if (fileList.length === 0) {
      message.warning('请先选择文件');
      return;
    }

    setUploading(true);
    setUploadProgress(0);
    setUploadResult(null);

    const formData = new FormData();
    formData.append('file', fileList[0]);
    formData.append('strategy', strategy);
    formData.append('config', JSON.stringify(config));
    formData.append('embedModel', embedModel);
    if (customPrompt) {
      formData.append('customPrompt', customPrompt);
    }

    const xhr = new XMLHttpRequest();
    xhr.open('POST', '/api/doc/upload');

    xhr.upload.onprogress = (event) => {
      if (event.lengthComputable) {
        const percent = Math.round((event.loaded / event.total) * 100);
        setUploadProgress(percent);
      }
    };

    xhr.onload = () => {
      setUploading(false);
      if (xhr.status >= 200 && xhr.status < 300) {
        try {
          const result = JSON.parse(xhr.responseText);
          setUploadResult(result);
          setCurrentStep(3);
          message.success('文档上传成功');
          setFileList([]);
        } catch (e) {
          message.error('解析响应失败');
        }
      } else {
        message.error(xhr.responseText || '上传失败');
      }
      setTimeout(() => setUploadProgress(0), 1000);
    };

    xhr.onerror = () => {
      setUploading(false);
      message.error('网络请求失败');
      setTimeout(() => setUploadProgress(0), 1000);
    };

    xhr.send(formData);
  };

  // 重置
  const handleReset = () => {
    setFileList([]);
    setUploadResult(null);
    setUploadProgress(0);
    setPreviewChunks([]);
    setPreviewError(null);
    setCurrentStep(0);
    setStrategy('recursive');
    setConfig(DEFAULT_CONFIGS.recursive);
  };

  // 渲染策略配置组件
  const renderStrategyConfig = () => {
    const commonProps = {
      config,
      onChange: handleConfigChange,
      disabled: uploading,
    };

    switch (strategy) {
      case 'recursive':
        return <RecursiveChunkConfig {...commonProps} />;
      case 'true_semantic':
        return <SemanticChunkConfig {...commonProps} />;
      case 'hierarchical':
        return <HierarchicalChunkConfig {...commonProps} />;
      case 'fixed_length':
        return (
          <Card size="small">
            <Space direction="vertical" style={{ width: '100%' }}>
              <div>
                <Text strong>分块大小</Text>
                <Input
                  type="number"
                  min={100}
                  max={4000}
                  value={config.chunkSize || 500}
                  onChange={(e) =>
                    handleConfigChange({ ...config, chunkSize: parseInt(e.target.value) || 500 })
                  }
                  style={{ width: '100%', marginTop: 8 }}
                  addonAfter="字符"
                />
              </div>
              <div>
                <Text strong>重叠大小</Text>
                <Input
                  type="number"
                  min={0}
                  max={config.chunkSize || 500}
                  value={config.overlap || 50}
                  onChange={(e) =>
                    handleConfigChange({ ...config, overlap: parseInt(e.target.value) || 0 })
                  }
                  style={{ width: '100%', marginTop: 8 }}
                  addonAfter="字符"
                />
              </div>
            </Space>
          </Card>
        );
      case 'hybrid':
        return (
          <Card size="small">
            <Space direction="vertical" style={{ width: '100%' }}>
              <div>
                <Text strong>基础分块大小</Text>
                <Input
                  type="number"
                  min={100}
                  max={4000}
                  value={config.chunkSize || 500}
                  onChange={(e) =>
                    handleConfigChange({ ...config, chunkSize: parseInt(e.target.value) || 500 })
                  }
                  style={{ width: '100%', marginTop: 8 }}
                  addonAfter="字符"
                />
              </div>
              <div>
                <Text strong>语义阈值</Text>
                <Input
                  type="number"
                  min={0}
                  max={1}
                  step={0.05}
                  value={config.semanticThreshold || 0.5}
                  onChange={(e) =>
                    handleConfigChange({
                      ...config,
                      semanticThreshold: parseFloat(e.target.value) || 0.5,
                    })
                  }
                  style={{ width: '100%', marginTop: 8 }}
                />
              </div>
            </Space>
          </Card>
        );
      case 'custom_rule':
        return (
          <Card size="small">
            <Space direction="vertical" style={{ width: '100%' }}>
              <div>
                <Text strong>自定义分隔符（每行一个）</Text>
                <TextArea
                  value={(config.separators || []).join('\n')}
                  onChange={(e) =>
                    handleConfigChange({
                      ...config,
                      separators: e.target.value.split('\n').filter(Boolean),
                    })
                  }
                  placeholder={'\\n\\n\n\\n\n。'}
                  rows={4}
                  style={{ marginTop: 8 }}
                />
              </div>
              <div>
                <Text strong>最大分块大小</Text>
                <Input
                  type="number"
                  min={100}
                  max={4000}
                  value={config.chunkSize || 500}
                  onChange={(e) =>
                    handleConfigChange({ ...config, chunkSize: parseInt(e.target.value) || 500 })
                  }
                  style={{ width: '100%', marginTop: 8 }}
                  addonAfter="字符"
                />
              </div>
            </Space>
          </Card>
        );
      default:
        return null;
    }
  };

  return (
    <div style={{ padding: '24px', maxWidth: 1200, margin: '0 auto' }}>
      <Title level={3}>文档上传</Title>
      <Paragraph type="secondary">
        上传文档并配置分块和向量化参数，支持多种智能分块策略
      </Paragraph>

      <Divider />

      {/* 步骤指示器 */}
      <Steps
        current={currentStep}
        items={[
          { title: '上传文件', status: currentStep >= 0 ? 'process' : 'wait' },
          { title: '配置策略', status: currentStep >= 1 ? 'process' : 'wait' },
          { title: '预览分块', status: currentStep >= 2 ? 'process' : 'wait' },
          { title: '完成', status: currentStep >= 3 ? 'finish' : 'wait' },
        ]}
        style={{ marginBottom: 24 }}
      />

      <Row gutter={24}>
        {/* 左侧：上传和配置 */}
        <Col xs={24} lg={12}>
          {/* 上传区域 */}
          <Card style={{ marginBottom: 24 }}>
            <div style={{ marginBottom: 12 }}>
              <Text strong>1. 选择文件</Text>
            </div>
            <Dragger
              name="file"
              beforeUpload={beforeUpload}
              fileList={fileList}
              onRemove={() => {
                setFileList([]);
                setUploadResult(null);
                setPreviewChunks([]);
                setCurrentStep(0);
              }}
              accept=".txt,.md,.pdf,.docx"
              disabled={uploading}
            >
              <p className="ant-upload-drag-icon">
                <InboxOutlined />
              </p>
              <p className="ant-upload-text">点击或拖拽文件到此区域上传</p>
              <p className="ant-upload-hint">
                支持单个文件上传，仅支持 .txt, .md, .pdf, .docx 格式，最大 50MB
              </p>
            </Dragger>

            {uploadProgress > 0 && (
              <Progress
                percent={uploadProgress}
                status={uploadProgress === 100 ? 'success' : 'active'}
                style={{ marginTop: 16 }}
              />
            )}

            {fileList.length > 0 && (
              <Alert
                message={`已选择: ${fileList[0].name} (${(fileList[0].size / 1024).toFixed(2)} KB)`}
                type="info"
                showIcon
                style={{ marginTop: 12 }}
              />
            )}
          </Card>

          {/* 策略选择区域 */}
          <Card style={{ marginBottom: 24 }}>
            <div style={{ marginBottom: 12 }}>
              <Text strong>2. 选择分块策略</Text>
            </div>
            <ChunkStrategySelector
              value={strategy}
              onChange={handleStrategyChange}
              disabled={uploading}
            />
          </Card>

          {/* 策略配置区域 */}
          <Card
            title={
              <Space>
                <SettingOutlined />
                <Text strong>3. 配置参数</Text>
              </Space>
            }
            style={{ marginBottom: 24 }}
          >
            {renderStrategyConfig()}
          </Card>

          {/* 嵌入模型和提示词配置 */}
          <Card style={{ marginBottom: 24 }}>
            <div style={{ marginBottom: 12 }}>
              <Text strong>4. 其他配置</Text>
            </div>
            <Space direction="vertical" style={{ width: '100%' }} size="middle">
              {/* 嵌入模型 */}
              <div>
                <Text strong>嵌入模型</Text>
                <Select
                  value={embedModel}
                  onChange={setEmbedModel}
                  style={{ width: '100%', marginTop: 8 }}
                  options={EMBED_MODELS}
                  disabled={uploading}
                />
              </div>

              {/* 自定义提示词 */}
              <div>
                <Text strong>自定义提示词（可选）</Text>
                <TextArea
                  value={customPrompt}
                  onChange={(e) => setCustomPrompt(e.target.value)}
                  placeholder="为文档指定自定义的系统提示词..."
                  rows={3}
                  style={{ marginTop: 8 }}
                  disabled={uploading}
                />
              </div>
            </Space>
          </Card>

          {/* 操作按钮 */}
          <Space style={{ marginBottom: 24 }} wrap>
            <Button
              type="default"
              icon={<EyeOutlined />}
              onClick={handlePreview}
              loading={previewLoading}
              disabled={fileList.length === 0 || uploading}
              size="large"
            >
              预览分块
            </Button>
            <Button
              type="primary"
              icon={<CloudUploadOutlined />}
              onClick={handleUpload}
              loading={uploading}
              disabled={fileList.length === 0}
              size="large"
            >
              上传并处理
            </Button>
            <Button
              icon={<ReloadOutlined />}
              onClick={handleReset}
              disabled={uploading}
              size="large"
            >
              重置
            </Button>
          </Space>
        </Col>

        {/* 右侧：预览和统计 */}
        <Col xs={24} lg={12}>
          {/* 统计信息 */}
          <ChunkStatistics chunks={previewChunks} loading={previewLoading} />

          {/* 分块预览 */}
          <Card
            title={
              <Space>
                <EyeOutlined />
                <Text strong>分块预览</Text>
              </Space>
            }
            extra={
              previewChunks.length > 0 && (
                <Tag color="blue">{previewChunks.length} 个分块</Tag>
              )
            }
            style={{ marginBottom: 24 }}
          >
            <Spin spinning={previewLoading} tip="正在分析文档并生成分块预览...">
              {previewError ? (
                <Alert
                  message="预览失败"
                  description={previewError}
                  type="error"
                  showIcon
                />
              ) : (
                <ChunkPreview
                  chunks={previewChunks}
                  loading={previewLoading}
                  error={previewError}
                />
              )}
            </Spin>
          </Card>
        </Col>
      </Row>

      {/* 上传结果 */}
      {uploadResult && (
        <Alert
          icon={<CheckCircleOutlined style={{ color: '#52c41a' }} />}
          type="success"
          showIcon
          message="上传成功"
          description={
            <div style={{ marginTop: 8 }}>
              <p>文档ID: {uploadResult.documentId}</p>
              <p>生成知识块数: {uploadResult.chunkCount}</p>
              <p>状态: {uploadResult.status}</p>
              <p>使用策略: {strategy}</p>
            </div>
          }
          style={{ marginTop: 24 }}
        />
      )}
    </div>
  );
};

export default DocumentUpload;
