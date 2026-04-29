import React, { useState, useEffect, useCallback } from 'react';
import {
  Card, Table, Button, Modal, Form, Input, Select, Tag, Space, message,
  Typography, Divider, Popconfirm, Alert,
} from 'antd';
import {
  PlusOutlined, DeleteOutlined, ReloadOutlined,
  ApiOutlined, CheckCircleOutlined, CloseCircleOutlined,
} from '@ant-design/icons';
import { mcpApi } from '../services/api';

const { Title, Text } = Typography;
const { TextArea } = Input;

const McpManagement = () => {
  const [servers, setServers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [modalLoading, setModalLoading] = useState(false);
  const [form] = Form.useForm();
  const [serverType, setServerType] = useState('STDIO');

  const loadServers = useCallback(async () => {
    setLoading(true);
    try {
      const res = await mcpApi.listServers();
      setServers(res.data || []);
    } catch (err) {
      message.error('加载 MCP Server 列表失败');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { loadServers(); }, [loadServers]);

  const handleRegister = async (values) => {
    setModalLoading(true);
    try {
      await mcpApi.register(values);
      message.success('MCP Server 注册成功');
      setModalOpen(false);
      form.resetFields();
      loadServers();
    } catch (err) {
      message.error(err.response?.data || '注册失败');
    } finally {
      setModalLoading(false);
    }
  };

  const handleUnregister = async (id) => {
    try {
      await mcpApi.unregister(id);
      message.success('MCP Server 已断开');
      loadServers();
    } catch (err) {
      message.error('断开失败');
    }
  };

  const handleReconnect = async (record) => {
    try {
      const dto = {
        name: record.name,
        description: record.description,
        type: record.type,
        command: record.command,
        args: record.args,
        url: record.url,
      };
      await mcpApi.reconnect(record.id, dto);
      message.success('重连成功');
      loadServers();
    } catch (err) {
      message.error('重连失败');
    }
  };

  const columns = [
    {
      title: '名称',
      dataIndex: 'name',
      key: 'name',
      render: (text, record) => (
        <Space>
          <ApiOutlined />
          <Text strong>{text}</Text>
          <Tag color={record.type === 'STDIO' ? 'blue' : 'green'}>
            {record.type}
          </Tag>
        </Space>
      ),
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      ellipsis: true,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status) => (
        status === 'ONLINE' ? (
          <Tag icon={<CheckCircleOutlined />} color="success">已连接</Tag>
        ) : status === 'ERROR' ? (
          <Tag icon={<CloseCircleOutlined />} color="error">错误</Tag>
        ) : (
          <Tag color="default">离线</Tag>
        )
      ),
    },
    {
      title: '工具数',
      dataIndex: 'toolCount',
      key: 'toolCount',
      render: (count) => <Text strong>{count ?? 0}</Text>,
    },
    {
      title: '操作',
      key: 'actions',
      render: (_, record) => (
        <Space>
          <Button
            size="small"
            icon={<ReloadOutlined />}
            onClick={() => handleReconnect(record)}
          >
            重连
          </Button>
          <Popconfirm
            title="确认断开此 MCP Server？"
            onConfirm={() => handleUnregister(record.id)}
          >
            <Button size="small" danger icon={<DeleteOutlined />}>
              断开
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const expandedRowRender = (record) => {
    if (!record.tools || record.tools.length === 0) {
      return <Text type="secondary">暂无工具</Text>;
    }
    return (
      <Table
        dataSource={record.tools}
        rowKey="name"
        pagination={false}
        showHeader={false}
        size="small"
        columns={[
          { dataIndex: 'name', title: '名称', render: (t) => <Text code>{t}</Text> },
          { dataIndex: 'description', title: '描述', ellipsis: true },
        ]}
      />
    );
  };

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <div>
          <Title level={3} style={{ margin: 0 }}>MCP Server 管理</Title>
          <Text type="secondary">注册和管理外部 MCP 工具服务</Text>
        </div>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setModalOpen(true)}>
          注册 MCP Server
        </Button>
      </div>

      <Divider style={{ margin: '12px 0' }} />

      <Alert
        message="提示：MCP Server 注册后会自动连接并提取工具列表。连接信息存储在内存中，重启后需重新注册。"
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
      />

      <Table
        columns={columns}
        dataSource={servers}
        rowKey="id"
        loading={loading}
        expandable={{ expandedRowRender, defaultExpandAllRows: false }}
        pagination={false}
      />

      <Modal
        title="注册 MCP Server"
        open={modalOpen}
        onCancel={() => { setModalOpen(false); form.resetFields(); }}
        footer={null}
        width={600}
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleRegister}
          initialValues={{ type: 'STDIO' }}
          onValuesChange={(changed) => {
            if (changed.type) setServerType(changed.type);
          }}
        >
          <Form.Item name="type" label="连接类型" rules={[{ required: true }]}>
            <Select>
              <Select.Option value="STDIO">STDIO（本地进程）</Select.Option>
              <Select.Option value="SSE">SSE（远程 HTTP）</Select.Option>
            </Select>
          </Form.Item>

          <Form.Item name="name" label="名称" rules={[{ required: true, message: '请输入名称' }]}>
            <Input placeholder="如：天气服务、数据库查询服务" />
          </Form.Item>

          <Form.Item name="description" label="描述">
            <Input placeholder="可选描述信息" />
          </Form.Item>

          {serverType === 'STDIO' ? (
            <>
              <Form.Item
                name="command"
                label="命令"
                rules={[{ required: true, message: '请输入可执行命令' }]}
                extra="如: npx、python、node"
              >
                <Input placeholder="npx" />
              </Form.Item>
              <Form.Item
                name="args"
                label="参数 (JSON 数组)"
                extra='如: ["-y", "@modelcontextprotocol/server-everything"]'
              >
                <TextArea rows={3} placeholder='["-y", "@modelcontextprotocol/server-everything"]' />
              </Form.Item>
            </>
          ) : (
            <Form.Item
              name="url"
              label="SSE 地址"
              rules={[{ required: true, message: '请输入 SSE 服务地址' }]}
              extra="MCP Server 的 SSE 端点 URL"
            >
              <Input placeholder="http://localhost:8080/mcp/v1/sse" />
            </Form.Item>
          )}

          <Form.Item name="envVars" label="环境变量 (JSON)">
            <TextArea rows={3} placeholder='{"API_KEY": "xxx"}' />
          </Form.Item>

          <Form.Item>
            <Space style={{ width: '100%', justifyContent: 'flex-end' }}>
              <Button onClick={() => { setModalOpen(false); form.resetFields(); }}>取消</Button>
              <Button type="primary" htmlType="submit" loading={modalLoading}>注册并连接</Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default McpManagement;
