import React from 'react';
import PropTypes from 'prop-types';
import {
  Form,
  Slider,
  InputNumber,
  Row,
  Col,
  Space,
  Typography,
  Tag,
  Divider,
  Alert,
} from 'antd';

const { Text } = Typography;

const DEFAULT_CONFIG = {
  chunkSize: 1000,
  minChunkSize: 100,
};

const SemanticBoundaryChunkConfig = ({ config: propConfig, onChange, disabled = false }) => {
  const config = { ...DEFAULT_CONFIG, ...propConfig };

  const handleChange = (key, value) => {
    onChange({
      ...DEFAULT_CONFIG,
      ...propConfig,
      [key]: value,
    });
  };

  return (
    <div>
      <Alert
        message="语义边界分块说明"
        description="基于语义边界检测进行分块，通过句子级别的语义向量距离识别自然段落边界，适合结构清晰的文档。"
        type="info"
        showIcon
        style={{ marginBottom: 24 }}
      />

      <Form layout="vertical" disabled={disabled}>
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item
              label={<Text strong>分块大小</Text>}
              tooltip="目标分块大小（字符数），算法会尽量在语义边界处产生接近此大小的分块"
            >
              <InputNumber
                min={200}
                max={8000}
                value={config.chunkSize || 1000}
                onChange={(value) => handleChange('chunkSize', value)}
                style={{ width: '100%' }}
                addonAfter="字符"
              />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item
              label={<Text strong>最小分块大小</Text>}
              tooltip="分块的最小字符数，避免产生过小的碎片分块"
            >
              <InputNumber
                min={50}
                max={config.chunkSize || 1000}
                value={config.minChunkSize || 100}
                onChange={(value) => handleChange('minChunkSize', value)}
                style={{ width: '100%' }}
                addonAfter="字符"
              />
            </Form.Item>
          </Col>
        </Row>
      </Form>

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
          <Tag color="blue">分块大小: {config.chunkSize || 1000}</Tag>
          <Tag color="green">最小分块: {config.minChunkSize || 100}</Tag>
        </Space>
      </div>
    </div>
  );
};

SemanticBoundaryChunkConfig.propTypes = {
  config: PropTypes.shape({
    chunkSize: PropTypes.number,
    minChunkSize: PropTypes.number,
  }),
  onChange: PropTypes.func.isRequired,
  disabled: PropTypes.bool,
};

export default SemanticBoundaryChunkConfig;
