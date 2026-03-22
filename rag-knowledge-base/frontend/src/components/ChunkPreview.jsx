import React, { useState } from 'react';
import PropTypes from 'prop-types';
import {
  Card,
  List,
  Tag,
  Typography,
  Space,
  Button,
  Empty,
  Tooltip,
  Collapse,
  Badge,
} from 'antd';
import {
  ExpandOutlined,
  CompressOutlined,
  CopyOutlined,
  FileTextOutlined,
  CheckOutlined,
} from '@ant-design/icons';

const { Text, Paragraph } = Typography;
const { Panel } = Collapse;

// 截断文本显示
const truncateText = (text, maxLength = 150) => {
  if (!text) return '';
  if (text.length <= maxLength) return text;
  return text.substring(0, maxLength) + '...';
};

// 获取分块类型颜色
const getChunkTypeColor = (type) => {
  const colors = {
    parent: 'blue',
    child: 'green',
    standalone: 'default',
  };
  return colors[type] || 'default';
};

// 单个分块卡片
const ChunkCard = ({ chunk, index, defaultExpanded = false }) => {
  const [expanded, setExpanded] = useState(defaultExpanded);
  const [copied, setCopied] = useState(false);

  const content = chunk.content || chunk.text || '';
  const charCount = content.length;
  const isExpanded = expanded || charCount <= 150;

  // 复制内容
  const handleCopy = async (e) => {
    e.stopPropagation();
    try {
      await navigator.clipboard.writeText(content);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (err) {
      console.error('复制失败:', err);
    }
  };

  return (
    <Card
      size="small"
      hoverable
      style={{
        marginBottom: 12,
        borderLeft: `3px solid ${chunk.type === 'parent' ? '#1890ff' : chunk.type === 'child' ? '#52c41a' : '#d9d9d9'}`,
      }}
      styles={{
        body: { padding: '12px 16px' },
      }}
    >
      {/* 头部信息 */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: 8,
        }}
      >
        <Space>
          <Badge
            count={index + 1}
            style={{ backgroundColor: '#1890ff' }}
            overflowCount={9999}
          />
          {chunk.type && (
            <Tag color={getChunkTypeColor(chunk.type)}>
              {chunk.type === 'parent' ? '父块' : chunk.type === 'child' ? '子块' : '独立块'}
            </Tag>
          )}
          <Tag color="default">{charCount} 字符</Tag>
        </Space>
        <Space>
          <Tooltip title={copied ? '已复制' : '复制内容'}>
            <Button
              type="text"
              size="small"
              icon={copied ? <CheckOutlined style={{ color: '#52c41a' }} /> : <CopyOutlined />}
              onClick={handleCopy}
            />
          </Tooltip>
          {charCount > 150 && (
            <Tooltip title={expanded ? '收起' : '展开'}>
              <Button
                type="text"
                size="small"
                icon={expanded ? <CompressOutlined /> : <ExpandOutlined />}
                onClick={() => setExpanded(!expanded)}
              />
            </Tooltip>
          )}
        </Space>
      </div>

      {/* 内容区域 */}
      <div
        style={{
          backgroundColor: '#fafafa',
          padding: 12,
          borderRadius: 4,
          maxHeight: isExpanded ? 'none' : '80px',
          overflow: 'hidden',
          position: 'relative',
        }}
      >
        <Paragraph
          style={{
            margin: 0,
            whiteSpace: 'pre-wrap',
            wordBreak: 'break-word',
            fontSize: 13,
            lineHeight: 1.6,
          }}
        >
          {isExpanded ? content : truncateText(content)}
        </Paragraph>
        {!isExpanded && (
          <div
            style={{
              position: 'absolute',
              bottom: 0,
              left: 0,
              right: 0,
              height: 30,
              background: 'linear-gradient(transparent, #fafafa)',
            }}
          />
        )}
      </div>

      {/* 元数据 */}
      {chunk.metadata && Object.keys(chunk.metadata).length > 0 && (
        <div style={{ marginTop: 8 }}>
          <Collapse ghost>
            <Panel header={<Text type="secondary" style={{ fontSize: 12 }}>元数据</Text>} key="metadata">
              <pre style={{ margin: 0, fontSize: 11, backgroundColor: '#f5f5f5', padding: 8, borderRadius: 4 }}>
                {JSON.stringify(chunk.metadata, null, 2)}
              </pre>
            </Panel>
          </Collapse>
        </div>
      )}
    </Card>
  );
};

ChunkCard.propTypes = {
  chunk: PropTypes.shape({
    content: PropTypes.string,
    text: PropTypes.string,
    type: PropTypes.string,
    metadata: PropTypes.object,
  }).isRequired,
  index: PropTypes.number.isRequired,
  defaultExpanded: PropTypes.bool,
};

const ChunkPreview = ({ chunks = [], loading = false, error = null, defaultExpanded = false }) => {
  const [expandAll, setExpandAll] = useState(false);

  // 空状态
  if (!chunks || chunks.length === 0) {
    return (
      <Empty
        image={Empty.PRESENTED_IMAGE_SIMPLE}
        description="暂无分块预览"
        style={{ padding: '40px 0' }}
      >
        <Text type="secondary">
          请先上传文件并点击"预览分块"按钮查看分块效果
        </Text>
      </Empty>
    );
  }

  // 错误状态
  if (error) {
    return (
      <Card style={{ borderColor: '#ff4d4f' }}>
        <Text type="danger">{error}</Text>
      </Card>
    );
  }

  return (
    <div>
      {/* 工具栏 */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: 16,
        }}
      >
        <Space>
          <FileTextOutlined />
          <Text strong>分块预览</Text>
          <Tag color="blue">{chunks.length} 个分块</Tag>
        </Space>
        <Button
          type="text"
          icon={expandAll ? <CompressOutlined /> : <ExpandOutlined />}
          onClick={() => setExpandAll(!expandAll)}
        >
          {expandAll ? '全部收起' : '全部展开'}
        </Button>
      </div>

      {/* 分块列表 */}
      <List
        dataSource={chunks}
        renderItem={(chunk, index) => (
          <ChunkCard
            key={chunk.id || index}
            chunk={chunk}
            index={index}
            defaultExpanded={expandAll || defaultExpanded}
          />
        )}
        style={{ maxHeight: '600px', overflow: 'auto' }}
      />
    </div>
  );
};

ChunkPreview.propTypes = {
  chunks: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
      content: PropTypes.string,
      text: PropTypes.string,
      type: PropTypes.string,
      metadata: PropTypes.object,
    })
  ),
  loading: PropTypes.bool,
  error: PropTypes.string,
  defaultExpanded: PropTypes.bool,
};

export default ChunkPreview;
