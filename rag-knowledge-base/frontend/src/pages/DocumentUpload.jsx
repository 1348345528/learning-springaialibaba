import React, { useState } from 'react';
import {
  Card,
  Upload,
  Button,
  Select,
  Input,
  InputNumber,
  message,
  Progress,
  Space,
  Typography,
  Divider,
  Alert,
} from 'antd';
import {
  InboxOutlined,
  FileTextOutlined,
  CheckCircleOutlined,
} from '@ant-design/icons';
import { documentApi } from '../services/api';

const { Dragger } = Upload;
const { Text, Title } = Typography;

const { TextArea } = Input;

// 分块策略选项 - 必须与后端 ChunkStrategy 实现类匹配
const CHUNK_STRATEGIES = [
  { value: 'fixed_length', label: '固定长度分块', description: '按固定字符数分块' },
  { value: 'semantic', label: '语义分块', description: '按语义段落分块' },
  { value: 'hybrid', label: '混合分块', description: '结合固定长度和语义分块' },
  { value: 'custom_rule', label: '自定义规则分块', description: '按自定义分隔符分块' },
];

// 嵌入模型选项 - 与后端配置匹配
const EMBED_MODELS = [
  { value: 'Qwen/Qwen3-Embedding-8B', label: 'Qwen3-Embedding-8B (4096维)' },
];

const DocumentUpload = () => {
  const [fileList, setFileList] = useState([]);
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [strategy, setStrategy] = useState('fixed_length');
  const [chunkSize, setChunkSize] = useState(500);
  const [chunkOverlap, setChunkOverlap] = useState(50);
  const [embedModel, setEmbedModel] = useState('Qwen/Qwen3-Embedding-8B');
  const [customPrompt, setCustomPrompt] = useState('');
  const [uploadResult, setUploadResult] = useState(null);

  // 上传前校验
  const beforeUpload = (file) => {
    const allowedTypes = [
      'text/plain',
      'text/markdown',
      'application/pdf',
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    ];
    const isAllowed = allowedTypes.some((type) => file.type.includes(type.split('/')[1])) ||
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
    return false;
  };

  // 处理上传 - 使用 XMLHttpRequest 实现真实进度
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
    formData.append('chunkSize', chunkSize);
    formData.append('chunkOverlap', chunkOverlap);
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
  };

  return (
    <div style={{ padding: '24px', maxWidth: 900, margin: '0 auto' }}>
      <Title level={3}>文档上传</Title>
      <Text type="secondary">上传文档并配置分块和向量化参数</Text>

      <Divider />

      {/* 上传区域 */}
      <Card style={{ marginBottom: 24 }}>
        <Dragger
          name="file"
          beforeUpload={beforeUpload}
          fileList={fileList}
          onRemove={() => {
            setFileList([]);
            setUploadResult(null);
          }}
          accept=".txt,.md,.pdf,.docx"
          disabled={uploading}
        >
          <p className="ant-upload-drag-icon">
            <InboxOutlined />
          </p>
          <p className="ant-upload-text">点击或拖拽文件到此区域上传</p>
          <p className="ant-upload-hint">
            支持单个文件上传，仅支持 .txt, .md, .pdf, .docx 格式
          </p>
        </Dragger>

        {uploadProgress > 0 && (
          <Progress
            percent={uploadProgress}
            status={uploadProgress === 100 ? 'success' : 'active'}
            style={{ marginTop: 16 }}
          />
        )}
      </Card>

      {/* 配置区域 */}
      <Card title="分块配置" style={{ marginBottom: 24 }}>
        <Space direction="vertical" style={{ width: '100%' }} size="middle">
          {/* 分块策略 */}
          <div>
            <Text strong>分块策略</Text>
            <Select
              value={strategy}
              onChange={setStrategy}
              style={{ width: '100%', marginTop: 8 }}
              options={CHUNK_STRATEGIES}
            />
          </div>

          {/* 分块大小和重叠 */}
          <Space style={{ width: '100%' }}>
            <div style={{ flex: 1 }}>
              <Text strong>分块大小（字符数）</Text>
              <InputNumber
                min={100}
                max={2000}
                value={chunkSize}
                onChange={setChunkSize}
                style={{ width: '100%', marginTop: 8 }}
              />
            </div>
            <div style={{ flex: 1 }}>
              <Text strong>分块重叠（字符数）</Text>
              <InputNumber
                min={0}
                max={chunkSize}
                value={chunkOverlap}
                onChange={setChunkOverlap}
                style={{ width: '100%', marginTop: 8 }}
              />
            </div>
          </Space>

          {/* 嵌入模型 */}
          <div>
            <Text strong>嵌入模型</Text>
            <Select
              value={embedModel}
              onChange={setEmbedModel}
              style={{ width: '100%', marginTop: 8 }}
              options={EMBED_MODELS}
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
            />
          </div>
        </Space>
      </Card>

      {/* 操作按钮 */}
      <Space style={{ marginBottom: 24 }}>
        <Button
          type="primary"
          icon={<FileTextOutlined />}
          onClick={handleUpload}
          loading={uploading}
          disabled={fileList.length === 0}
          size="large"
        >
          上传并处理
        </Button>
        <Button onClick={handleReset} disabled={uploading}>
          重置
        </Button>
      </Space>

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
            </div>
          }
        />
      )}
    </div>
  );
};

export default DocumentUpload;
