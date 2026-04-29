import React, { useState, useEffect } from 'react';
import { Select, Tag, Space, Spin, Typography } from 'antd';
import { ToolOutlined } from '@ant-design/icons';
import { mcpApi } from '../../services/api';

const { Text } = Typography;

/**
 * MCP 工具选择器 — 在聊天页面上方展示，让用户选择要启用的 MCP 工具
 *
 * Props:
 *   value: string[] — 当前选中的工具名列表
 *   onChange: (toolNames: string[]) => void — 选中变化回调
 */
const McpToolSelector = ({ value = [], onChange }) => {
  const [tools, setTools] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    loadTools();
  }, []);

  const loadTools = async () => {
    setLoading(true);
    try {
      const res = await mcpApi.listTools();
      setTools(res.data || []);
    } catch {
      // 忽略加载失败（可能无 MCP 服务）
      setTools([]);
    } finally {
      setLoading(false);
    }
  };

  // 按 Server 分组
  const grouped = tools.reduce((acc, tool) => {
    const key = tool.serverName || '未分组';
    if (!acc[key]) acc[key] = [];
    acc[key].push(tool);
    return acc;
  }, {});

  const options = Object.entries(grouped).map(([server, serverTools]) => ({
    label: server,
    options: serverTools.map((t) => ({
      label: (
        <Space size={4}>
          <Text code>{t.name}</Text>
          {t.description && (
            <Text type="secondary" style={{ fontSize: 12 }}>
              — {t.description}
            </Text>
          )}
        </Space>
      ),
      value: t.name,
    })),
  }));

  if (tools.length === 0 && !loading) return null;

  return (
    <div style={{ padding: '8px 16px', borderBottom: '1px solid #f0f0f0', background: '#fafafa' }}>
      <Space>
        <ToolOutlined style={{ color: '#1677ff' }} />
        <Text type="secondary" style={{ fontSize: 13 }}>MCP 工具：</Text>
        {loading ? (
          <Spin size="small" />
        ) : (
          <Select
            mode="multiple"
            placeholder="选择要启用的 MCP 工具..."
            value={value}
            onChange={onChange}
            style={{ minWidth: 300, maxWidth: '100%' }}
            allowClear
            showSearch
            optionFilterProp="label"
            options={options}
            tagRender={(props) => (
              <Tag closable={props.closable} onClose={props.onClose} color="blue">
                {props.label}
              </Tag>
            )}
            size="small"
          />
        )}
        <Text
          type="secondary"
          style={{ fontSize: 12, cursor: 'pointer' }}
          onClick={loadTools}
        >
          刷新
        </Text>
      </Space>
    </div>
  );
};

export default McpToolSelector;
