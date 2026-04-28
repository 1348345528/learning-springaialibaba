import React, { useState, useEffect } from 'react';
import {
  Card,
  Table,
  Button,
  Space,
  Input,
  Modal,
  Form,
  message,
  Popconfirm,
  Tag,
  Typography,
  Select,
  Divider,
} from 'antd';
import {
  SearchOutlined,
  EditOutlined,
  DeleteOutlined,
  ReloadOutlined,
  EyeOutlined,
  ExclamationCircleOutlined,
} from '@ant-design/icons';
import { vectorApi, documentApi } from '../services/api';

const { Title, Text } = Typography;
const { TextArea } = Input;

const ChunkManagement = () => {
  const [chunks, setChunks] = useState([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  });
  const [filters, setFilters] = useState({
    keyword: '',
    documentId: null,
  });
  const [documents, setDocuments] = useState([]);
  const [selectedChunks, setSelectedChunks] = useState([]);

  // 详情和编辑 Modal
  const [detailModal, setDetailModal] = useState({ visible: false, chunk: null, mode: 'view' });
  const [form] = Form.useForm();

  // 加载知识块列表
  const loadChunks = async (page = 1, pageSize = 10) => {
    setLoading(true);
    try {
      const params = {
        page: page - 1,
        size: pageSize,
      };
      if (filters.keyword) params.keyword = filters.keyword;
      const response = await vectorApi.listChunks(params);
      setChunks(response.data?.content || []);
      setPagination({
        current: page,
        pageSize: pageSize,
        total: response.data?.totalElements || 0,
      });
    } catch (error) {
      message.error(error.message || '加载失败');
    } finally {
      setLoading(false);
    }
  };

  // 加载文档列表（用于筛选）
  const loadDocuments = async () => {
    try {
      const response = await documentApi.list();
      setDocuments(Array.isArray(response.data) ? response.data : []);
    } catch (error) {
      console.error('加载文档列表失败', error);
    }
  };

  useEffect(() => {
    loadChunks();
    loadDocuments();
  }, []);

  // 分页变化
  const handleTableChange = (newPagination) => {
    loadChunks(newPagination.current, newPagination.pageSize);
  };

  // 搜索
  const handleSearch = () => {
    loadChunks(1, pagination.pageSize);
  };

  // 查看详情
  const handleView = (chunk) => {
    setDetailModal({ visible: true, chunk, mode: 'view' });
  };

  // 编辑
  const handleEdit = (chunk) => {
    form.setFieldsValue({
      content: chunk.content,
      tags: JSON.stringify(chunk.tags, null, 2),
    });
    setDetailModal({ visible: true, chunk, mode: 'edit' });
  };

  // 保存编辑
  const handleSave = async () => {
    try {
      const values = await form.validateFields();
      const updateData = {
        content: values.content,
        tags: values.tags ? JSON.parse(values.tags) : [],
      };

      await vectorApi.updateChunk(detailModal.chunk.id, updateData);
      message.success('更新成功');
      setDetailModal({ visible: false, chunk: null, mode: 'view' });
      loadChunks(pagination.current, pagination.pageSize);
    } catch (error) {
      if (error.errorFields) {
        return; // 表单验证失败
      }
      message.error(error.message || '更新失败');
    }
  };

  // 删除单个
  const handleDelete = async (id) => {
    try {
      await vectorApi.deleteChunk(id);
      message.success('删除成功');
      loadChunks(pagination.current, pagination.pageSize);
    } catch (error) {
      message.error(error.message || '删除失败');
    }
  };

  // 批量删除
  const handleBatchDelete = async () => {
    if (selectedChunks.length === 0) {
      message.warning('请先选择要删除的知识块');
      return;
    }

    Modal.confirm({
      title: '确认批量删除',
      icon: <ExclamationCircleOutlined />,
      content: `确定要删除选中的 ${selectedChunks.length} 个知识块吗？此操作不可恢复。`,
      okText: '确认删除',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          await vectorApi.batchDeleteChunks(selectedChunks);
          message.success('批量删除成功');
          setSelectedChunks([]);
          loadChunks(pagination.current, pagination.pageSize);
        } catch (error) {
          message.error(error.message || '批量删除失败');
        }
      },
    });
  };

  // 重新向量化
  const handleReindex = async (documentId) => {
    try {
      await vectorApi.reindex(documentId);
      message.success('重新向量化任务已提交');
    } catch (error) {
      message.error(error.message || '操作失败');
    }
  };

  // 表格列配置
  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 80,
      ellipsis: true,
    },
    {
      title: '内容预览',
      dataIndex: 'content',
      key: 'content',
      width: 300,
      ellipsis: true,
      render: (text) => text?.substring(0, 100) + '...',
    },
    {
      title: '文档名称',
      dataIndex: 'documentName',
      key: 'documentName',
      width: 100,
      ellipsis: true,
    },
    {
      title: '分块序号',
      dataIndex: 'chunkIndex',
      key: 'chunkIndex',
      width: 100,
    },
    {
      title: '分块策略',
      dataIndex: 'strategy',
      key: 'strategy',
      width: 100,
      render: (strategy) => <Tag color="blue">{strategy}</Tag>,
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 180,
      render: (date) => date ? new Date(date).toLocaleString() : '-',
    },
    {
      title: '操作',
      key: 'action',
      width: 180,
      fixed: 'right',
      render: (_, record) => (
        <Space size="small">
          <Button
            type="text"
            icon={<EyeOutlined />}
            onClick={() => handleView(record)}
            title="查看"
          />
          <Button
            type="text"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
            title="编辑"
          />
          <Popconfirm
            title="确认删除？"
            onConfirm={() => handleDelete(record.id)}
            okText="确认"
            cancelText="取消"
          >
            <Button
              type="text"
              danger
              icon={<DeleteOutlined />}
              title="删除"
            />
          </Popconfirm>
        </Space>
      ),
    },
  ];

  // 行选择配置
  const rowSelection = {
    selectedRowKeys: selectedChunks,
    onChange: (keys) => setSelectedChunks(keys),
  };

  return (
    <div style={{ padding: '24px', maxWidth: 1400, margin: '0 auto' }}>
      <Title level={3}>知识块管理</Title>
      <Text type="secondary">查看、编辑和管理知识库中的知识块</Text>

      <Divider />

      {/* 筛选区域 */}
      <Card size="small" style={{ marginBottom: 16 }}>
        <Space wrap style={{ width: '100%' }}>
          <Input
            placeholder="搜索内容关键词"
            prefix={<SearchOutlined />}
            value={filters.keyword}
            onChange={(e) => setFilters({ ...filters, keyword: e.target.value })}
            style={{ width: 200 }}
            onPressEnter={handleSearch}
          />
          <Select
            placeholder="按文档筛选"
            allowClear
            value={filters.documentId}
            onChange={(value) => setFilters({ ...filters, documentId: value })}
            style={{ width: 200 }}
            options={documents.map((docName) => ({
              value: docName,
              label: docName,
            }))}
          />
          <Button type="primary" icon={<SearchOutlined />} onClick={handleSearch}>
            搜索
          </Button>
          <Button icon={<ReloadOutlined />} onClick={() => loadChunks()}>
            刷新
          </Button>
        </Space>
      </Card>

      {/* 批量操作 */}
      <Space style={{ marginBottom: 16 }}>
        <Popconfirm
          title="确认批量删除？"
          onConfirm={handleBatchDelete}
          okText="确认"
          cancelText="取消"
          disabled={selectedChunks.length === 0}
        >
          <Button danger icon={<DeleteOutlined />} disabled={selectedChunks.length === 0}>
            批量删除 ({selectedChunks.length})
          </Button>
        </Popconfirm>
      </Space>

      {/* 表格 */}
      <Card>
        <Table
          rowSelection={rowSelection}
          columns={columns}
          dataSource={chunks}
          rowKey="id"
          loading={loading}
          pagination={{
            ...pagination,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条`,
          }}
          onChange={handleTableChange}
          scroll={{ x: 1000 }}
        />
      </Card>

      {/* 详情/编辑 Modal */}
      <Modal
        title={detailModal.mode === 'edit' ? '编辑知识块' : '知识块详情'}
        open={detailModal.visible}
        onCancel={() => setDetailModal({ visible: false, chunk: null, mode: 'view' })}
        width={700}
        footer={
          detailModal.mode === 'edit'
            ? [
                <Button key="cancel" onClick={() => setDetailModal({ visible: false, chunk: null, mode: 'view' })}>
                  取消
                </Button>,
                <Button key="save" type="primary" onClick={handleSave}>
                  保存
                </Button>,
              ]
            : [
                <Button key="close" onClick={() => setDetailModal({ visible: false, chunk: null, mode: 'view' })}>
                  关闭
                </Button>,
                <Button
                  key="edit"
                  type="primary"
                  onClick={() => setDetailModal({ ...detailModal, mode: 'edit' })}
                >
                  编辑
                </Button>,
              ]
        }
      >
        <Form form={form} layout="vertical">
          <Form.Item
            label="内容"
            name="content"
            rules={[{ required: true, message: '请输入内容' }]}
          >
            {detailModal.mode === 'edit' ? (
              <TextArea rows={8} />
            ) : (
              <div style={{ background: '#f5f5f5', padding: 12, borderRadius: 4, whiteSpace: 'pre-wrap' }}>
                {detailModal.chunk?.content}
              </div>
            )}
          </Form.Item>

          <Form.Item label="标签 (JSON)" name="tags">
            {detailModal.mode === 'edit' ? (
              <TextArea rows={4} placeholder='["tag1", "tag2"]' />
            ) : (
              <pre style={{ background: '#f5f5f5', padding: 12, borderRadius: 4 }}>
                {JSON.stringify(detailModal.chunk?.tags, null, 2)}
              </pre>
            )}
          </Form.Item>

          {detailModal.chunk && (
            <Space direction="vertical" style={{ width: '100%' }}>
              <Text type="secondary">文档名称: {detailModal.chunk.documentName}</Text>
              <Text type="secondary">分块序号: {detailModal.chunk.chunkIndex}</Text>
              <Text type="secondary">分块策略: {detailModal.chunk.strategy}</Text>
              <Text type="secondary">创建时间: {new Date(detailModal.chunk.createdAt).toLocaleString()}</Text>
            </Space>
          )}
        </Form>
      </Modal>
    </div>
  );
};

export default ChunkManagement;
