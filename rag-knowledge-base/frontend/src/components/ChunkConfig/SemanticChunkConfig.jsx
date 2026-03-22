import React from 'react';
import PropTypes from 'prop-types';
import {
  Form,
  Slider,
  InputNumber,
  Switch,
  Radio,
  Row,
  Col,
  Space,
  Typography,
  Tag,
  Divider,
  Alert,
  Tooltip,
} from 'antd';
import {
  InfoCircleOutlined,
  ThunderboltOutlined,
  AimOutlined,
} from '@ant-design/icons';

const { Text, Paragraph } = Typography;

// 断点方法选项
const BREAKPOINT_METHODS = [
  {
    value: 'PERCENTILE',
    label: '百分位法',
    description: '基于相似度分布的百分位数确定断点',
    icon: <AimOutlined />,
  },
  {
    value: 'GRADIENT',
    label: '梯度法',
    description: '基于相似度变化的梯度确定断点',
    icon: <ThunderboltOutlined />,
  },
  {
    value: 'FIXED_THRESHOLD',
    label: '固定阈值法',
    description: '使用固定的相似度阈值确定断点',
    icon: <InfoCircleOutlined />,
  },
];

const SemanticChunkConfig = ({ config, onChange, disabled }) => {
  // 处理配置变更
  const handleChange = (key, value) => {
    onChange({
      ...config,
      [key]: value,
    });
  };

  // 是否使用动态阈值
  const useDynamicThreshold = config.useDynamicThreshold !== false;

  return (
    <div>
      {/* 说明提示 */}
      <Alert
        message="语义分块说明"
        description="基于文本语义相似度进行智能分块，相邻句子语义差异超过阈值时创建新分块。适合需要保持语义连贯性的长文本。"
        type="info"
        showIcon
        style={{ marginBottom: 24 }}
      />

      <Form layout="vertical" disabled={disabled}>
        {/* 相似度阈值 */}
        <Form.Item
          label={
            <Space>
              <Text strong>相似度阈值</Text>
              <Tag color="blue">{(config.similarityThreshold || 0.45).toFixed(2)}</Tag>
            </Space>
          }
          tooltip="当相邻文本块的语义相似度低于此阈值时，将创建新的分块"
        >
          <Slider
            min={0}
            max={1}
            step={0.01}
            value={config.similarityThreshold || 0.45}
            onChange={(value) => handleChange('similarityThreshold', value)}
            marks={{
              0: '0',
              0.25: '0.25',
              0.5: '0.5',
              0.75: '0.75',
              1: '1',
            }}
          />
          <Text type="secondary" style={{ fontSize: 12 }}>
            阈值越低，分块越少（倾向于合并）；阈值越高，分块越多（倾向于分割）
          </Text>
        </Form.Item>

        <Divider />

        {/* 动态阈值开关 */}
        <Form.Item
          label={<Text strong>动态阈值调整</Text>}
          tooltip="启用后将根据文本特征自动调整相似度阈值"
        >
          <Switch
            checked={useDynamicThreshold}
            onChange={(checked) => handleChange('useDynamicThreshold', checked)}
            checkedChildren="启用"
            unCheckedChildren="禁用"
          />
        </Form.Item>

        {/* 动态阈值百分位 */}
        {useDynamicThreshold && (
          <Form.Item
            label={
              <Space>
                <Text strong>百分位阈值</Text>
                <Tag color="green">
                  {((config.percentileThreshold || 0.8) * 100).toFixed(0)}%
                </Tag>
              </Space>
            }
            tooltip="在所有句子对的相似度差异中，只有前 (1-百分位)% 的差异才会被视为断点"
          >
            <Slider
              min={0.5}
              max={0.95}
              step={0.01}
              value={config.percentileThreshold || 0.8}
              onChange={(value) => handleChange('percentileThreshold', value)}
              marks={{
                0.5: '50%',
                0.65: '65%',
                0.8: '80%',
                0.9: '90%',
                0.95: '95%',
              }}
            />
          </Form.Item>
        )}

        <Divider />

        {/* 断点方法选择 */}
        <Form.Item label={<Text strong>断点检测方法</Text>}>
          <Radio.Group
            value={config.breakpointMethod || 'PERCENTILE'}
            onChange={(e) => handleChange('breakpointMethod', e.target.value)}
            style={{ width: '100%' }}
          >
            <Space direction="vertical" style={{ width: '100%' }}>
              {BREAKPOINT_METHODS.map((method) => (
                <Radio
                  key={method.value}
                  value={method.value}
                  style={{
                    padding: 12,
                    border: '1px solid #d9d9d9',
                    borderRadius: 6,
                    width: '100%',
                    backgroundColor:
                      (config.breakpointMethod || 'PERCENTILE') === method.value
                        ? '#e6f4ff'
                        : '#fff',
                  }}
                >
                  <Space>
                    {method.icon}
                    <div>
                      <Text strong>{method.label}</Text>
                      <br />
                      <Text type="secondary" style={{ fontSize: 12 }}>
                        {method.description}
                      </Text>
                    </div>
                  </Space>
                </Radio>
              ))}
            </Space>
          </Radio.Group>
        </Form.Item>

        <Divider />

        {/* 分块大小限制 */}
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item
              label={<Text strong>最小分块大小</Text>}
              tooltip="分块的最小字符数"
            >
              <InputNumber
                min={50}
                max={1000}
                value={config.minChunkSize || 100}
                onChange={(value) => handleChange('minChunkSize', value)}
                style={{ width: '100%' }}
                addonAfter="字符"
              />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item
              label={<Text strong>最大分块大小</Text>}
              tooltip="分块的最大字符数，超过此值将被强制分割"
            >
              <InputNumber
                min={500}
                max={8000}
                value={config.maxChunkSize || 2000}
                onChange={(value) => handleChange('maxChunkSize', value)}
                style={{ width: '100%' }}
                addonAfter="字符"
              />
            </Form.Item>
          </Col>
        </Row>
      </Form>

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
        <Space size={[8, 8]} wrap>
          <Tag color="blue">
            相似度阈值: {(config.similarityThreshold || 0.45).toFixed(2)}
          </Tag>
          <Tag color={useDynamicThreshold ? 'green' : 'default'}>
            动态阈值: {useDynamicThreshold ? '启用' : '禁用'}
          </Tag>
          {useDynamicThreshold && (
            <Tag color="purple">
              百分位: {((config.percentileThreshold || 0.8) * 100).toFixed(0)}%
            </Tag>
          )}
          <Tag color="orange">
            断点方法:{' '}
            {BREAKPOINT_METHODS.find(
              (m) => m.value === (config.breakpointMethod || 'PERCENTILE')
            )?.label || '百分位法'}
          </Tag>
        </Space>
      </div>
    </div>
  );
};

SemanticChunkConfig.propTypes = {
  config: PropTypes.shape({
    similarityThreshold: PropTypes.number,
    useDynamicThreshold: PropTypes.bool,
    percentileThreshold: PropTypes.number,
    breakpointMethod: PropTypes.oneOf(['PERCENTILE', 'GRADIENT', 'FIXED_THRESHOLD']),
    minChunkSize: PropTypes.number,
    maxChunkSize: PropTypes.number,
  }),
  onChange: PropTypes.func.isRequired,
  disabled: PropTypes.bool,
};

SemanticChunkConfig.defaultProps = {
  config: {
    similarityThreshold: 0.45,
    useDynamicThreshold: true,
    percentileThreshold: 0.8,
    breakpointMethod: 'PERCENTILE',
    minChunkSize: 100,
    maxChunkSize: 2000,
  },
  disabled: false,
};

export default SemanticChunkConfig;
