import React, { useState, useEffect } from 'react';
import {
  Table, Button, Modal, Form, Input, Select, message, Space, Popconfirm, Tag
} from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import api from '../services/api';

const { Option } = Select;

const UserManagement = () => {
  const [users, setUsers] = useState([]);
  const [roles, setRoles] = useState([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingUser, setEditingUser] = useState(null);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);
  const [form] = Form.useForm();

  const fetchUsers = async (p = page, s = pageSize) => {
    setLoading(true);
    try {
      const response = await api.get('/sys/user/list', { params: { page: p, size: s } });
      const data = response.data;
      setUsers(data.records || []);
      setTotal(data.total || 0);
    } catch (error) {
      message.error('获取用户列表失败');
    } finally {
      setLoading(false);
    }
  };

  const fetchRoles = async () => {
    try {
      const response = await api.get('/sys/role/list');
      setRoles(response.data);
    } catch (error) {
      message.error('获取角色列表失败');
    }
  };

  useEffect(() => {
    fetchUsers();
    fetchRoles();
  }, []);

  const showModal = (user = null) => {
    setEditingUser(user);
    if (user) {
      form.setFieldsValue({
        username: user.username,
        email: user.email,
        status: user.status,
        roleIds: (user.roles || []).map(r => r.id),
      });
    } else {
      form.resetFields();
    }
    setModalVisible(true);
  };

  const handleSave = async () => {
    try {
      const values = await form.validateFields();
      if (editingUser) {
        await api.put(`/sys/user/${editingUser.id}`, values);
        message.success('更新成功');
      } else {
        await api.post('/sys/user', values);
        message.success('创建成功');
      }
      setModalVisible(false);
      fetchUsers();
    } catch (error) {
      message.error(editingUser ? '更新失败' : '创建失败');
    }
  };

  const handleDelete = async (id) => {
    try {
      await api.delete(`/sys/user/${id}`);
      message.success('删除成功');
      fetchUsers();
    } catch (error) {
      message.error('删除失败');
    }
  };

  const handlePageChange = (p, s) => {
    setPage(p);
    setPageSize(s);
    fetchUsers(p, s);
  };

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
    { title: '用户名', dataIndex: 'username', key: 'username' },
    { title: '邮箱', dataIndex: 'email', key: 'email' },
    {
      title: '状态', dataIndex: 'status', key: 'status',
      render: (status) => (
        <Tag color={status === 1 ? 'green' : 'red'}>
          {status === 1 ? '启用' : '禁用'}
        </Tag>
      ),
    },
    {
      title: '角色', dataIndex: 'roles', key: 'roles',
      render: (roles) => (
        <>
          {roles && roles.map(role => (
            <Tag key={role.id} color="blue">{role.name}</Tag>
          ))}
        </>
      ),
    },
    {
      title: '操作', key: 'action', width: 180,
      render: (_, record) => (
        <Space size="middle">
          <Button icon={<EditOutlined />} onClick={() => showModal(record)}>编辑</Button>
          <Popconfirm
            title="确定删除该用户吗？"
            onConfirm={() => handleDelete(record.id)}
            okText="是"
            cancelText="否"
          >
            <Button icon={<DeleteOutlined />} danger>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <div style={{ marginBottom: 16, textAlign: 'right' }}>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => showModal()}>
          新增用户
        </Button>
      </div>
      <Table
        columns={columns}
        dataSource={users}
        rowKey="id"
        loading={loading}
        pagination={{
          current: page,
          pageSize: pageSize,
          total: total,
          showSizeChanger: true,
          showTotal: (t) => `共 ${t} 条`,
          onChange: handlePageChange,
        }}
      />

      <Modal
        title={editingUser ? '编辑用户' : '新增用户'}
        open={modalVisible}
        onOk={handleSave}
        onCancel={() => setModalVisible(false)}
        okText="保存"
        cancelText="取消"
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="username"
            label="用户名"
            rules={[{ required: true, message: '请输入用户名' }]}
          >
            <Input />
          </Form.Item>
          {!editingUser && (
            <Form.Item
              name="password"
              label="密码"
              rules={[{ required: true, message: '请输入密码' }]}
            >
              <Input.Password />
            </Form.Item>
          )}
          <Form.Item
            name="email"
            label="邮箱"
            rules={[{ type: 'email', message: '请输入有效的邮箱地址' }]}
          >
            <Input />
          </Form.Item>
          <Form.Item
            name="status"
            label="状态"
            rules={[{ required: true }]}
            initialValue={1}
          >
            <Select>
              <Option value={1}>启用</Option>
              <Option value={0}>禁用</Option>
            </Select>
          </Form.Item>
          <Form.Item name="roleIds" label="角色">
            <Select mode="multiple" placeholder="请选择角色">
              {roles.map(role => (
                <Option key={role.id} value={role.id}>{role.name}</Option>
              ))}
            </Select>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default UserManagement;
