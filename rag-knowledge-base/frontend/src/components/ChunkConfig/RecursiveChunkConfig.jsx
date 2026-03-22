import React, { useState } from 'react';
import PropTypes from 'prop-types';
import {
  Form,
  Slider,
  InputNumber,
  Switch,
  Collapse,
  Tag,
  Space,
  Typography,
  Divider,
  Input,
  Button,
  Row,
  Col,
} from 'antd';
import {
  SettingOutlined,
  PlusOutlined,
  DeleteOutlined,
} from '@ant-design/icons';

const { Panel } = Collapse;
const { Text, Title } = Typography;
const { TextArea } = Input;

// 默认分隔符配置
const DEFAULT_SEPARATORS = [
  { value: '\n\n', label: '段落 (双换行)' },
  { value: '\n', label: '行 (单换行)' },
  { value: '。', label: '句号' },
  { value: '！', label: '感叹号' },
  { value: '？', label: '问号' },
  { value: '；', label: '分号' },
  { value: '，', label: '逗号' },
  { value: ' ', label: '空格' },
  { value: '', label: '字符' },
];

const RecursiveChunkConfig = ({ config, onChange, disabled }) => {
  const [separators, setSeparators] = useState(
    config.separators || DEFAULT_SEPARATORS.map((s) => s.value)
  );
  const [newSeparator, setNewSeparator] = useState('');

  // 处理配置变更
  const handleChange = (key, value) => {
    onChange({
      ...config,
      [key]: value,
    });
  };

  // 添加自定义分隔符
  const handleAddSeparator = () => {
    if (newSeparator && !separators.includes(newSeparator)) {
      const newSeparators = [...separators, newSeparator];
      setSeparators(newSeparators);
      handleChange('separators', newSeparators);
      setNewSeparator('');
    }
  };

  // 删除分隔符
  const handleRemoveSeparator = (index) => {
    const newSeparators = separators.filter((_, i) => i !== index);
    setSeparators(newSeparators);
    handleChange('separators', newSeparators);
  };

  // 重置为默认分隔符
  const handleResetSeparators = () => {
    const defaultSeparators = DEFAULT_SEPARATORS.map((s) => s.value);
    setSeparators(defaultSeparators);
    handleChange('separators', defaultSeparators);
  };

  // 格式化分隔符显示
  const formatSeparator = (sep) => {
    if (sep === '\n\n') return '\\n\\n';
    if (sep === '\n') return '\\n';
    if (sep === '\t') return '\\t';
    if (sep === ' ') return '[空格]';
    if (sep === '') return '[空]';
    return sep;
  };

  return (
    <div>
      {/* 基础配置 */}
      <Form layout="vertical" disabled={disabled}>
        {/* 分块大小 */}
        <Form.Item
          label={
            <Space>
              <Text strong>分块大小</Text>
              <Tag color="blue">{config.chunkSize || 500} 字符</Tag>
            </Space>
          }
          tooltip="每个分块的目标字符数"
        >
          <Slider
            min={100}
            max={4000}
            value={config.chunkSize || 500}
            onChange={(value) => handleChange('chunkSize', value)}
            marks={{
              100: '100',
              500: '500',
              1000: '1000',
              2000: '2000',
              3000: '3000',
              4000: '4000',
            }}
          />
        </Form.Item>

        {/* 重叠大小 */}
        <Form.Item
          label={
            <Space>
              <Text strong>分块重叠</Text>
              <Tag color="green">{config.overlap || 50} 字符</Tag>
            </Space>
          }
          tooltip="相邻分块之间的重叠字符数，有助于保持上下文连贯"
        >
          <Slider
            min={0}
            max={250}
            value={config.overlap || 50}
            onChange={(value) => handleChange('overlap', value)}
            marks={{
              0: '0',
              50: '50',
              100: '100',
              150: '150',
              200: '200',
              250: '250',
            }}
          />
        </Form.Item>

        {/* 最小分块大小 */}
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item
              label={<Text strong>最小分块大小</Text>}
              tooltip="分块的最小字符数，小于此值的文本将被合并"
            >
              <InputNumber
                min={10}
                max={config.chunkSize || 500}
                value={config.minChunkSize || 50}
                onChange={(value) => handleChange('minChunkSize', value)}
                style={{ width: '100%' }}
                addonAfter="字符"
              />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item
              label={<Text strong>保留分隔符</Text>}
              tooltip="是否在分块中保留分隔符"
            >
              <Switch
                checked={config.keepSeparator !== false}
                onChange={(checked) => handleChange('keepSeparator', checked)}
                checkedChildren="保留"
                unCheckedChildren="移除"
              />
            </Form.Item>
          </Col>
        </Row>
      </Form>

      <Divider />

      {/* 高级配置 */}
      <Collapse
        ghost
        items={[
          {
            key: 'advanced',
            label: (
              <Space>
                <SettingOutlined />
                <Text strong>高级配置</Text>
                <Tag color="orange">分隔符编辑器</Tag>
              </Space>
            ),
            children: (
              <div>
                <Text type="secondary" style={{ display: 'block', marginBottom: 12 }}>
                  分隔符按优先级排序，系统会依次尝试使用这些分隔符进行文本分割
                </Text>

                {/* 当前分隔符列表 */}
                <div style={{ marginBottom: 16 }}>
                  <Text strong style={{ display: 'block', marginBottom: 8 }}>
                    当前分隔符（按优先级排序）：
                  </Text>
                  <Space size={[8, 8]} wrap>
                    {separators.map((sep, index) => (
                      <Tag
                        key={index}
                        closable={!disabled}
                        onClose={() => handleRemoveSeparator(index)}
                        style={{
                          padding: '4px 8px',
                          fontSize: 13,
                        }}
                      >
                        {index + 1}. {formatSeparator(sep)}
                      </Tag>
                    ))}
                  </Space>
                </div>

                {/* 添加自定义分隔符 */}
                <Space.Compact style={{ width: '100%', marginBottom: 16 }}>
                  <Input
                    placeholder="输入自定义分隔符"
                    value={newSeparator}
                    onChange={(e) => setNewSeparator(e.target.value)}
                    onPressEnter={handleAddSeparator}
                    disabled={disabled}
                  />
                  <Button
                    type="primary"
                    icon={<PlusOutlined />}
                    onClick={handleAddSeparator}
                    disabled={disabled || !newSeparator}
                  >
                    添加
                  </Button>
                </Space.Compact>

                {/* 预设分隔符 */}
                <div style={{ marginBottom: 16 }}>
                  <Text strong style={{ display: 'block', marginBottom: 8 }}>
                    预设分隔符：
                  </Text>
                  <Space size={[8, 8]} wrap>
                    {DEFAULT_SEPARATORS.map((item, index) => (
                      <Tag
                        key={index}
                        style={{
                          cursor: 'pointer',
                          opacity: separators.includes(item.value) ? 0.5 : 1,
                        }}
                        onClick={() => {
                          if (!separators.includes(item.value) && !disabled) {
                            const newSeparators = [...separators, item.value];
                            setSeparators(newSeparators);
                            handleChange('separators', newSeparators);
                          }
                        }}
                      >
                        {item.label}
                      </Tag>
                    ))}
                  </Space>
                </div>

                {/* 重置按钮 */}
                <Button
                  onClick={handleResetSeparators}
                  disabled={disabled}
                  icon={<DeleteOutlined />}
                >
                  重置为默认
                </Button>
              </div>
            ),
          },
        ]}
      />
    </div>
  );
};

RecursiveChunkConfig.propTypes = {
  config: PropTypes.shape({
    chunkSize: PropTypes.number,
    overlap: PropTypes.number,
    minChunkSize: PropTypes.number,
    keepSeparator: PropTypes.bool,
    separators: PropTypes.arrayOf(PropTypes.string),
  }),
  onChange: PropTypes.func.isRequired,
  disabled: PropTypes.bool,
};

RecursiveChunkConfig.defaultProps = {
  config: {
    chunkSize: 500,
    overlap: 50,
    minChunkSize: 50,
    keepSeparator: true,
    separators: ['\n\n', '\n', '。', '！', '？', '；', '，', ' ', ''],
  },
  disabled: false,
};

export default RecursiveChunkConfig;
