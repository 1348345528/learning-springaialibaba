import React, { useMemo } from 'react';
import PropTypes from 'prop-types';
import { Card, Row, Col, Statistic, Progress, Space, Typography, Tag, Divider } from 'antd';
import {
  FileTextOutlined,
  AlignCenterOutlined,
  ArrowDownOutlined,
  ArrowUpOutlined,
  BarChartOutlined,
} from '@ant-design/icons';

const { Text, Title } = Typography;

// 计算分块统计信息
const calculateStatistics = (chunks) => {
  if (!chunks || chunks.length === 0) {
    return {
      totalChunks: 0,
      totalChars: 0,
      avgSize: 0,
      minSize: 0,
      maxSize: 0,
      sizeDistribution: [],
      typeDistribution: {},
    };
  }

  // 获取每个分块的字符数
  const sizes = chunks.map((chunk) => (chunk.content || chunk.text || '').length);
  const totalChars = sizes.reduce((sum, size) => sum + size, 0);
  const avgSize = Math.round(totalChars / chunks.length);
  const minSize = Math.min(...sizes);
  const maxSize = Math.max(...sizes);

  // 计算大小分布
  const sizeRanges = [
    { label: '< 200', min: 0, max: 199 },
    { label: '200-500', min: 200, max: 500 },
    { label: '500-1000', min: 500, max: 1000 },
    { label: '1000-2000', min: 1000, max: 2000 },
    { label: '> 2000', min: 2001, max: Infinity },
  ];

  const sizeDistribution = sizeRanges.map((range) => ({
    label: range.label,
    count: sizes.filter((size) => size >= range.min && size <= range.max).length,
    percentage: 0,
  }));

  sizeDistribution.forEach((item) => {
    item.percentage = Math.round((item.count / chunks.length) * 100);
  });

  // 计算类型分布
  const typeDistribution = {};
  chunks.forEach((chunk) => {
    const type = chunk.type || 'standalone';
    typeDistribution[type] = (typeDistribution[type] || 0) + 1;
  });

  return {
    totalChunks: chunks.length,
    totalChars,
    avgSize,
    minSize,
    maxSize,
    sizeDistribution,
    typeDistribution,
  };
};

const ChunkStatistics = ({ chunks, loading }) => {
  const stats = useMemo(() => calculateStatistics(chunks), [chunks]);

  if (stats.totalChunks === 0) {
    return null;
  }

  return (
    <Card
      title={
        <Space>
          <BarChartOutlined />
          <Text strong>分块统计</Text>
        </Space>
      }
      loading={loading}
      style={{ marginBottom: 24 }}
    >
      {/* 主要统计指标 */}
      <Row gutter={[24, 24]}>
        <Col xs={12} sm={6}>
          <Statistic
            title="总分块数"
            value={stats.totalChunks}
            prefix={<FileTextOutlined />}
            suffix="块"
            valueStyle={{ color: '#1890ff' }}
          />
        </Col>
        <Col xs={12} sm={6}>
          <Statistic
            title="总字符数"
            value={stats.totalChars}
            valueStyle={{ color: '#52c41a' }}
          />
        </Col>
        <Col xs={12} sm={6}>
          <Statistic
            title="平均块大小"
            value={stats.avgSize}
            prefix={<AlignCenterOutlined />}
            suffix="字符"
            valueStyle={{ color: '#722ed1' }}
          />
        </Col>
        <Col xs={12} sm={6}>
          <Space direction="vertical" size={0}>
            <Text type="secondary">块大小范围</Text>
            <Space>
              <Tag color="orange" icon={<ArrowDownOutlined />}>
                最小: {stats.minSize}
              </Tag>
              <Tag color="blue" icon={<ArrowUpOutlined />}>
                最大: {stats.maxSize}
              </Tag>
            </Space>
          </Space>
        </Col>
      </Row>

      <Divider />

      {/* 大小分布 */}
      <div style={{ marginBottom: 16 }}>
        <Text strong style={{ display: 'block', marginBottom: 12 }}>
          分块大小分布
        </Text>
        <Row gutter={[12, 12]}>
          {stats.sizeDistribution.map((item, index) => (
            <Col xs={24} sm={12} md={4} key={index}>
              <div
                style={{
                  padding: 12,
                  backgroundColor: '#fafafa',
                  borderRadius: 6,
                  textAlign: 'center',
                }}
              >
                <Text type="secondary" style={{ fontSize: 12 }}>
                  {item.label} 字符
                </Text>
                <div style={{ marginTop: 8 }}>
                  <Text strong style={{ fontSize: 18 }}>
                    {item.count}
                  </Text>
                  <Text type="secondary" style={{ fontSize: 12, marginLeft: 4 }}>
                    ({item.percentage}%)
                  </Text>
                </div>
                <Progress
                  percent={item.percentage}
                  showInfo={false}
                  size="small"
                  strokeColor={{
                    '0%': '#1890ff',
                    '100%': '#52c41a',
                  }}
                  style={{ marginTop: 8 }}
                />
              </div>
            </Col>
          ))}
        </Row>
      </div>

      {/* 类型分布 */}
      {Object.keys(stats.typeDistribution).length > 1 && (
        <div>
          <Text strong style={{ display: 'block', marginBottom: 12 }}>
            分块类型分布
          </Text>
          <Space size={[12, 12]} wrap>
            {Object.entries(stats.typeDistribution).map(([type, count]) => {
              const typeColors = {
                parent: 'blue',
                child: 'green',
                standalone: 'default',
              };
              const typeLabels = {
                parent: '父块',
                child: '子块',
                standalone: '独立块',
              };
              return (
                <Tag
                  key={type}
                  color={typeColors[type] || 'default'}
                  style={{ padding: '4px 12px', fontSize: 13 }}
                >
                  {typeLabels[type] || type}: {count} 块
                </Tag>
              );
            })}
          </Space>
        </div>
      )}
    </Card>
  );
};

ChunkStatistics.propTypes = {
  chunks: PropTypes.arrayOf(
    PropTypes.shape({
      content: PropTypes.string,
      text: PropTypes.string,
      type: PropTypes.string,
    })
  ),
  loading: PropTypes.bool,
};

ChunkStatistics.defaultProps = {
  chunks: [],
  loading: false,
};

export default ChunkStatistics;
