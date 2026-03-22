import React from 'react';
import PropTypes from 'prop-types';
import { Card, Row, Col, Tag, Typography, Space } from 'antd';
import {
  ScissorOutlined,
  BulbOutlined,
  ApartmentOutlined,
  LineOutlined,
  MergeCellsOutlined,
  ToolOutlined,
  CheckOutlined,
} from '@ant-design/icons';

const { Text, Paragraph } = Typography;

// 分块策略配置
const STRATEGIES = [
  {
    key: 'recursive',
    name: '递归分块',
    description: '基于分隔符层级递归分割，保留文本结构完整性',
    icon: <ScissorOutlined style={{ fontSize: 24 }} />,
    badge: '推荐',
    badgeColor: 'green',
    features: ['保留段落结构', '智能分隔符识别', '适合结构化文档'],
  },
  {
    key: 'true_semantic',
    name: '语义分块',
    description: '基于语义相似度动态分块，保持语义连贯性',
    icon: <BulbOutlined style={{ fontSize: 24 }} />,
    badge: '高级',
    badgeColor: 'blue',
    features: ['语义感知', '动态阈值', '适合长文本'],
  },
  {
    key: 'hierarchical',
    name: '分层分块',
    description: '父块和子块双层结构，支持多粒度检索',
    icon: <ApartmentOutlined style={{ fontSize: 24 }} />,
    badge: '高级',
    badgeColor: 'purple',
    features: ['父子层级', '多粒度检索', '适合复杂文档'],
  },
  {
    key: 'fixed_length',
    name: '固定长度分块',
    description: '按固定字符数切分，简单高效',
    icon: <LineOutlined style={{ fontSize: 24 }} />,
    badge: null,
    badgeColor: null,
    features: ['简单高效', '可预测大小', '适合纯文本'],
  },
  {
    key: 'hybrid',
    name: '混合分块',
    description: '结合多种策略优点，灵活适应不同场景',
    icon: <MergeCellsOutlined style={{ fontSize: 24 }} />,
    badge: '高级',
    badgeColor: 'orange',
    features: ['策略融合', '自适应', '通用性强'],
  },
  {
    key: 'custom_rule',
    name: '自定义规则分块',
    description: '按自定义分隔符规则分块，高度可定制',
    icon: <ToolOutlined style={{ fontSize: 24 }} />,
    badge: null,
    badgeColor: null,
    features: ['自定义分隔符', '灵活配置', '适合特殊格式'],
  },
];

const ChunkStrategySelector = ({ value, onChange, disabled }) => {
  return (
    <div>
      <Row gutter={[16, 16]}>
        {STRATEGIES.map((strategy) => {
          const isSelected = value === strategy.key;
          return (
            <Col xs={24} sm={12} lg={8} key={strategy.key}>
              <Card
                hoverable
                onClick={() => !disabled && onChange(strategy.key)}
                className={`strategy-card ${isSelected ? 'strategy-card-selected' : ''}`}
                style={{
                  cursor: disabled ? 'not-allowed' : 'pointer',
                  borderColor: isSelected ? '#1890ff' : undefined,
                  backgroundColor: isSelected ? '#e6f4ff' : undefined,
                  transition: 'all 0.3s',
                  opacity: disabled ? 0.6 : 1,
                }}
              >
                <div style={{ position: 'relative' }}>
                  {/* 选中标记 */}
                  {isSelected && (
                    <div
                      style={{
                        position: 'absolute',
                        top: -8,
                        right: -8,
                        width: 24,
                        height: 24,
                        borderRadius: '50%',
                        backgroundColor: '#1890ff',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                      }}
                    >
                      <CheckOutlined style={{ color: '#fff', fontSize: 12 }} />
                    </div>
                  )}

                  {/* 图标和标题 */}
                  <Space direction="vertical" size="small" style={{ width: '100%' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                      <div
                        style={{
                          color: isSelected ? '#1890ff' : '#666',
                          transition: 'color 0.3s',
                        }}
                      >
                        {strategy.icon}
                      </div>
                      <div style={{ flex: 1 }}>
                        <Space>
                          <Text strong style={{ fontSize: 15 }}>
                            {strategy.name}
                          </Text>
                          {strategy.badge && (
                            <Tag color={strategy.badgeColor} style={{ margin: 0 }}>
                              {strategy.badge}
                            </Tag>
                          )}
                        </Space>
                      </div>
                    </div>

                    {/* 描述 */}
                    <Paragraph
                      type="secondary"
                      style={{ margin: 0, fontSize: 12 }}
                      ellipsis={{ rows: 2 }}
                    >
                      {strategy.description}
                    </Paragraph>

                    {/* 特性标签 */}
                    <div style={{ marginTop: 8 }}>
                      <Space size={[4, 4]} wrap>
                        {strategy.features.map((feature, idx) => (
                          <Tag
                            key={idx}
                            style={{
                              margin: 0,
                              fontSize: 11,
                              backgroundColor: isSelected ? '#bae0ff' : '#f5f5f5',
                              border: 'none',
                            }}
                          >
                            {feature}
                          </Tag>
                        ))}
                      </Space>
                    </div>
                  </Space>
                </div>
              </Card>
            </Col>
          );
        })}
      </Row>

      {/* 选中策略说明 */}
      {value && (
        <Card
          size="small"
          style={{
            marginTop: 16,
            backgroundColor: '#fafafa',
            border: '1px dashed #d9d9d9',
          }}
        >
          <Text type="secondary">
            <strong>当前选择：</strong>
            {STRATEGIES.find((s) => s.key === value)?.description}
          </Text>
        </Card>
      )}

      <style>
        {`
          .strategy-card:hover {
            transform: translateY(-2px);
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
          }
          .strategy-card-selected:hover {
            transform: translateY(-2px);
            box-shadow: 0 4px 12px rgba(24, 144, 255, 0.3);
          }
        `}
      </style>
    </div>
  );
};

ChunkStrategySelector.propTypes = {
  value: PropTypes.oneOf([
    'recursive',
    'true_semantic',
    'hierarchical',
    'fixed_length',
    'hybrid',
    'custom_rule',
  ]),
  onChange: PropTypes.func.isRequired,
  disabled: PropTypes.bool,
};

ChunkStrategySelector.defaultProps = {
  value: 'recursive',
  disabled: false,
};

export default ChunkStrategySelector;
