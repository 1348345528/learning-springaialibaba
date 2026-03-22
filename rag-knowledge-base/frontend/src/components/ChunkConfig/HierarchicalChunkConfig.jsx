import React from 'react';
import PropTypes from 'prop-types';
import {
  Form,
  Slider,
  InputNumber,
  Radio,
  Row,
  Col,
  Space,
  Typography,
  Tag,
  Divider,
  Card,
  Alert,
} from 'antd';
import {
  ApartmentOutlined,
  FolderOutlined,
  FileOutlined,
} from '@ant-design/icons';

const { Text, Title } = Typography;

// 子块分割策略
const CHILD_SPLIT_STRATEGIES = [
  {
    value: 'RECURSIVE',
    label: '递归分割',
    description: '使用递归分隔符进行分割',
  },
  {
    value: 'SENTENCE',
    label: '句子分割',
    description: '按句子边界进行分割',
  },
  {
    value: 'FIXED',
    label: '固定长度',
    description: '按固定字符数分割',
  },
];

const HierarchicalChunkConfig = ({ config, onChange, disabled }) => {
  // 处理配置变更
  const handleChange = (key, value) => {
    onChange({
      ...config,
      [key]: value,
    });
  };

  const parentSize = config.parentChunkSize || 2000;
  const childSize = config.childChunkSize || 200;

  return (
    <div>
      {/* 说明提示 */}
      <Alert
        message="分层分块说明"
        description="创建父块和子块的双层结构。父块提供上下文，子块用于精确检索。适合需要多粒度检索的复杂文档。"
        type="info"
        showIcon
        style={{ marginBottom: 24 }}
      />

      {/* 父子块示意图 */}
      <Card
        size="small"
        style={{ marginBottom: 24, backgroundColor: '#fafafa' }}
      >
        <div style={{ textAlign: 'center', padding: '12px 0' }}>
          {/* 父块 */}
          <div
            style={{
              border: '2px solid #1890ff',
              borderRadius: 8,
              padding: 16,
              marginBottom: 8,
              backgroundColor: '#e6f4ff',
            }}
          >
            <Space>
              <FolderOutlined style={{ color: '#1890ff', fontSize: 16 }} />
              <Text strong style={{ color: '#1890ff' }}>
                父块 (Parent Chunk)
              </Text>
              <Tag color="blue">{parentSize} 字符</Tag>
            </Space>
            <div style={{ marginTop: 8 }}>
              <Text type="secondary" style={{ fontSize: 12 }}>
                提供上下文，用于大粒度检索
              </Text>
            </div>

            {/* 子块容器 */}
            <div
              style={{
                display: 'flex',
                gap: 8,
                marginTop: 12,
                flexWrap: 'wrap',
                justifyContent: 'center',
              }}
            >
              {[1, 2, 3, 4].map((i) => (
                <div
                  key={i}
                  style={{
                    border: '1px solid #52c41a',
                    borderRadius: 4,
                    padding: '8px 12px',
                    backgroundColor: '#f6ffed',
                    minWidth: 80,
                  }}
                >
                  <Space size={4}>
                    <FileOutlined style={{ color: '#52c41a', fontSize: 12 }} />
                    <Text style={{ fontSize: 11, color: '#52c41a' }}>
                      子块 {i}
                    </Text>
                  </Space>
                  <div>
                    <Text type="secondary" style={{ fontSize: 10 }}>
                      {childSize}字符
                    </Text>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <Text type="secondary" style={{ fontSize: 12 }}>
            每个父块包含多个子块，检索时先定位父块再精确定位子块
          </Text>
        </div>
      </Card>

      <Form layout="vertical" disabled={disabled}>
        {/* 父块配置 */}
        <Card
          size="small"
          title={
            <Space>
              <FolderOutlined style={{ color: '#1890ff' }} />
              <Text strong>父块配置</Text>
            </Space>
          }
          style={{ marginBottom: 16 }}
        >
          {/* 父块大小 */}
          <Form.Item
            label={
              <Space>
                <Text>父块大小</Text>
                <Tag color="blue">{parentSize} 字符</Tag>
              </Space>
            }
            tooltip="父块的目标字符数，决定上下文的粒度"
          >
            <Slider
              min={1000}
              max={8000}
              step={100}
              value={parentSize}
              onChange={(value) => handleChange('parentChunkSize', value)}
              marks={{
                1000: '1000',
                2000: '2000',
                4000: '4000',
                6000: '6000',
                8000: '8000',
              }}
            />
          </Form.Item>

          {/* 父块重叠 */}
          <Form.Item label={<Text>父块重叠</Text>} tooltip="相邻父块之间的重叠字符数">
            <InputNumber
              min={0}
              max={Math.floor(parentSize / 4)}
              value={config.parentOverlap || 200}
              onChange={(value) => handleChange('parentOverlap', value)}
              style={{ width: '100%' }}
              addonAfter="字符"
            />
          </Form.Item>
        </Card>

        {/* 子块配置 */}
        <Card
          size="small"
          title={
            <Space>
              <FileOutlined style={{ color: '#52c41a' }} />
              <Text strong>子块配置</Text>
            </Space>
          }
          style={{ marginBottom: 16 }}
        >
          {/* 子块大小 */}
          <Form.Item
            label={
              <Space>
                <Text>子块大小</Text>
                <Tag color="green">{childSize} 字符</Tag>
              </Space>
            }
            tooltip="子块的目标字符数，决定精确检索的粒度"
          >
            <Slider
              min={100}
              max={1000}
              step={50}
              value={childSize}
              onChange={(value) => handleChange('childChunkSize', value)}
              marks={{
                100: '100',
                200: '200',
                400: '400',
                600: '600',
                800: '800',
                1000: '1000',
              }}
            />
          </Form.Item>

          {/* 子块重叠 */}
          <Form.Item label={<Text>子块重叠</Text>} tooltip="相邻子块之间的重叠字符数">
            <InputNumber
              min={0}
              max={Math.floor(childSize / 4)}
              value={config.childOverlap || 20}
              onChange={(value) => handleChange('childOverlap', value)}
              style={{ width: '100%' }}
              addonAfter="字符"
            />
          </Form.Item>

          {/* 子块分割策略 */}
          <Form.Item label={<Text>子块分割策略</Text>}>
            <Radio.Group
              value={config.childSplitStrategy || 'RECURSIVE'}
              onChange={(e) => handleChange('childSplitStrategy', e.target.value)}
            >
              <Space direction="vertical">
                {CHILD_SPLIT_STRATEGIES.map((strategy) => (
                  <Radio key={strategy.value} value={strategy.value}>
                    <Space>
                      <Text strong>{strategy.label}</Text>
                      <Text type="secondary" style={{ fontSize: 12 }}>
                        {strategy.description}
                      </Text>
                    </Space>
                  </Radio>
                ))}
              </Space>
            </Radio.Group>
          </Form.Item>
        </Card>
      </Form>

      {/* 配置验证提示 */}
      {childSize >= parentSize && (
        <Alert
          message="配置警告"
          description="子块大小不应大于或等于父块大小，这可能导致每个父块只有一个子块"
          type="warning"
          showIcon
          style={{ marginTop: 16 }}
        />
      )}

      {/* 配置摘要 */}
      <Divider />
      <div
        style={{
          padding: 12,
          backgroundColor: '#fafafa',
          borderRadius: 6,
          border: '1px solid #d9d9d9',
        }}
      >
        <Text strong style={{ display: 'block', marginBottom: 8 }}>
          当前配置摘要：
        </Text>
        <Row gutter={16}>
          <Col span={12}>
            <Space direction="vertical" size="small">
              <Text type="secondary">父块配置：</Text>
              <Space size={[8, 8]} wrap>
                <Tag color="blue">大小: {parentSize}</Tag>
                <Tag color="blue">重叠: {config.parentOverlap || 200}</Tag>
              </Space>
            </Space>
          </Col>
          <Col span={12}>
            <Space direction="vertical" size="small">
              <Text type="secondary">子块配置：</Text>
              <Space size={[8, 8]} wrap>
                <Tag color="green">大小: {childSize}</Tag>
                <Tag color="green">重叠: {config.childOverlap || 20}</Tag>
                <Tag color="purple">
                  策略:{' '}
                  {CHILD_SPLIT_STRATEGIES.find(
                    (s) => s.value === (config.childSplitStrategy || 'RECURSIVE')
                  )?.label || '递归分割'}
                </Tag>
              </Space>
            </Space>
          </Col>
        </Row>
      </div>
    </div>
  );
};

HierarchicalChunkConfig.propTypes = {
  config: PropTypes.shape({
    parentChunkSize: PropTypes.number,
    parentOverlap: PropTypes.number,
    childChunkSize: PropTypes.number,
    childOverlap: PropTypes.number,
    childSplitStrategy: PropTypes.oneOf(['RECURSIVE', 'SENTENCE', 'FIXED']),
  }),
  onChange: PropTypes.func.isRequired,
  disabled: PropTypes.bool,
};

HierarchicalChunkConfig.defaultProps = {
  config: {
    parentChunkSize: 2000,
    parentOverlap: 200,
    childChunkSize: 200,
    childOverlap: 20,
    childSplitStrategy: 'RECURSIVE',
  },
  disabled: false,
};

export default HierarchicalChunkConfig;
