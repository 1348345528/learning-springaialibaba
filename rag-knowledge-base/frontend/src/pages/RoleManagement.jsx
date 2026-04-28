import React, { useState, useEffect } from 'react';
import {
  Table, Button, Modal, Form, Input, message, Space, Popconfirm, Tag, Tree
} from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import api from '../services/api';

const RoleManagement = () => {
  const [roles, setRoles] = useState([]);
  const [menus, setMenus] = useState([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingRole, setEditingRole] = useState(null);
  const [form] = Form.useForm();
  const [checkedMenuKeys, setCheckedMenuKeys] = useState([]);

  // 获取角色列表
  const fetchRoles = async () => {
    setLoading(true);
    try {
      const response = await api.get('/sys/role/list');
      setRoles(response.data);
    } catch (error) {
      message.error('获取角色列表失败');
    } finally {
      setLoading(false);
    }
  };

  // 获取菜单树（用于分配菜单权限）
  const fetchMenuTree = async () => {
    try {
      const response = await api.get('/sys/menu/tree');
      setMenus(response.data);
    } catch (error) {
      message.error('获取菜单树失败');
    }
  };

  // 获取某个角色已分配的菜单ID列表
  const fetchRoleMenus = async (roleId) => {
    try {
      const response = await api.get(`/sys/role/menu/${roleId}`);
      return response.data; // 假设返回菜单ID数组
    } catch (error) {
      message.error('获取角色菜单失败');
      return [];
    }
  };

  useEffect(() => {
    fetchRoles();
    fetchMenuTree();
  }, []);

  // 打开新增/编辑弹窗
  const showModal = async (role = null) => {
    setEditingRole(role);
    if (role) {
      form.setFieldsValue({
        name: role.name,
        code: role.code,
        description: role.description,
      });
      const menuIds = await fetchRoleMenus(role.id);
      setCheckedMenuKeys(menuIds);
    } else {
      form.resetFields();
      setCheckedMenuKeys([]);
    }
    setModalVisible(true);
  };

  // 保存角色（包括分配的菜单）
  const handleSave = async () => {
    try {
      const values = await form.validateFields();
      const payload = {
        ...values,
        menuIds: checkedMenuKeys
      };
      if (editingRole) {
        await api.put(`/sys/role/${editingRole.id}`, payload);
        message.success('更新成功');
      } else {
        await api.post('/sys/role', payload);
        message.success('创建成功');
      }
      setModalVisible(false);
      fetchRoles();
    } catch (error) {
      message.error(editingRole ? '更新失败' : '创建失败');
    }
  };

  // 删除角色
  const handleDelete = async (id) => {
    try {
      await api.delete(`/sys/role/${id}`);
      message.success('删除成功');
      fetchRoles();
    } catch (error) {
      message.error('删除失败');
    }
  };

  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
    },
    {
      title: '角色名称',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: '角色编码',
      dataIndex: 'code',
      key: 'code',
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Space size="middle">
          <Button icon={<EditOutlined />} onClick={() => showModal(record)}>编辑</Button>
          <Popconfirm
            title="确定删除该角色吗？"
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

  // 将后端返回的菜单树转换为antd Tree可用的格式（key, title, children）
  const formatTreeData = (nodes) => {
    return nodes.map(node => ({
      key: node.id,
      title: node.name,
      children: node.children ? formatTreeData(node.children) : []
    }));
  };

  const treeData = formatTreeData(menus);

  const onCheck = (checkedKeys) => {
    setCheckedMenuKeys(checkedKeys);
  };

  return (
    <div>
      <div style={{ marginBottom: 16, textAlign: 'right' }}>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => showModal()}>
          新增角色
        </Button>
      </div>
      <Table columns={columns} dataSource={roles} rowKey="id" loading={loading} />

      <Modal
        title={editingRole ? '编辑角色' : '新增角色'}
        open={modalVisible}
        onOk={handleSave}
        onCancel={() => setModalVisible(false)}
        okText="保存"
        cancelText="取消"
        width={600}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="name"
            label="角色名称"
            rules={[{ required: true, message: '请输入角色名称' }]}
          >
            <Input />
          </Form.Item>
          <Form.Item
            name="code"
            label="角色编码"
            rules={[{ required: true, message: '请输入角色编码' }]}
          >
            <Input />
          </Form.Item>
          <Form.Item
            name="description"
            label="描述"
          >
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item label="菜单权限">
            <Tree
              checkable
              treeData={treeData}
              checkedKeys={checkedMenuKeys}
              onCheck={onCheck}
              defaultExpandAll
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default RoleManagement;
