import React, { useState, useEffect } from 'react';
import {
  Table, Button, Modal, Form, Input, Select, InputNumber, message, Space, Popconfirm, Tag
} from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import api from '../services/api';

const { Option } = Select;

const MenuManagement = () => {
  const [menus, setMenus] = useState([]);
  const [flatMenus, setFlatMenus] = useState([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingMenu, setEditingMenu] = useState(null);
  const [form] = Form.useForm();

  // 获取菜单树（用于表格展示需转换为一维数组）
  const fetchMenus = async () => {
    setLoading(true);
    try {
      const response = await api.get('/sys/menu/tree');
      setMenus(response.data);
      // 将树形结构平铺为一维数组（用于表格渲染）
      const flatten = (nodes, parentName = '') => {
        let result = [];
        for (const node of nodes) {
          result.push({
            ...node,
            parentName: parentName || '根节点'
          });
          if (node.children && node.children.length) {
            result = result.concat(flatten(node.children, node.name));
          }
        }
        return result;
      };
      setFlatMenus(flatten(response.data));
    } catch (error) {
      message.error('获取菜单列表失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchMenus();
  }, []);

  // 打开新增/编辑弹窗
  const showModal = (menu = null) => {
    setEditingMenu(menu);
    if (menu) {
      form.setFieldsValue({
        parentId: menu.parentId,
        name: menu.name,
        path: menu.path,
        component: menu.component,
        icon: menu.icon,
        sortOrder: menu.sortOrder,
        menuType: menu.menuType,
        permission: menu.permission,
        visible: menu.visible
      });
    } else {
      form.resetFields();
      form.setFieldsValue({
        parentId: 0,
        sortOrder: 0,
        menuType: 2,
        visible: 1
      });
    }
    setModalVisible(true);
  };

  // 保存菜单
  const handleSave = async () => {
    try {
      const values = await form.validateFields();
      if (editingMenu) {
        await api.put(`/sys/menu/${editingMenu.id}`, values);
        message.success('更新成功');
      } else {
        await api.post('/sys/menu', values);
        message.success('创建成功');
      }
      setModalVisible(false);
      fetchMenus();
    } catch (error) {
      message.error(editingMenu ? '更新失败' : '创建失败');
    }
  };

  // 删除菜单
  const handleDelete = async (id) => {
    try {
      await api.delete(`/sys/menu/${id}`);
      message.success('删除成功');
      fetchMenus();
    } catch (error) {
      message.error('删除失败');
    }
  };

  const columns = [
    {
      title: '菜单名称',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: '父级菜单',
      dataIndex: 'parentName',
      key: 'parentName',
    },
    {
      title: '路径',
      dataIndex: 'path',
      key: 'path',
    },
    {
      title: '组件',
      dataIndex: 'component',
      key: 'component',
    },
    {
      title: '图标',
      dataIndex: 'icon',
      key: 'icon',
    },
    {
      title: '排序',
      dataIndex: 'sortOrder',
      key: 'sortOrder',
    },
    {
      title: '类型',
      dataIndex: 'menuType',
      key: 'menuType',
      render: (type) => {
        if (type === 1) return <Tag color="blue">目录</Tag>;
        if (type === 2) return <Tag color="green">菜单</Tag>;
        if (type === 3) return <Tag color="orange">按钮</Tag>;
        return type;
      }
    },
    {
      title: '权限标识',
      dataIndex: 'permission',
      key: 'permission',
    },
    {
      title: '可见',
      dataIndex: 'visible',
      key: 'visible',
      render: (visible) => (
        <Tag color={visible === 1 ? 'green' : 'red'}>
          {visible === 1 ? '显示' : '隐藏'}
        </Tag>
      )
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Space size="middle">
          <Button icon={<EditOutlined />} onClick={() => showModal(record)}>编辑</Button>
          <Popconfirm
            title="确定删除该菜单吗？"
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

  // 构建父级菜单选项（用于选择上级菜单）
  const getParentMenuOptions = (nodes, prefix = '') => {
    let options = [{ value: 0, label: '根节点' }];
    for (const node of nodes) {
      options.push({ value: node.id, label: prefix + node.name });
      if (node.children && node.children.length) {
        options = options.concat(getParentMenuOptions(node.children, prefix + '  '));
      }
    }
    return options;
  };

  const parentMenuOptions = getParentMenuOptions(menus);

  return (
    <div>
      <div style={{ marginBottom: 16, textAlign: 'right' }}>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => showModal()}>
          新增菜单
        </Button>
      </div>
      <Table columns={columns} dataSource={flatMenus} rowKey="id" loading={loading} />

      <Modal
        title={editingMenu ? '编辑菜单' : '新增菜单'}
        open={modalVisible}
        onOk={handleSave}
        onCancel={() => setModalVisible(false)}
        okText="保存"
        cancelText="取消"
        width={600}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="parentId"
            label="上级菜单"
          >
            <Select options={parentMenuOptions} />
          </Form.Item>
          <Form.Item
            name="name"
            label="菜单名称"
            rules={[{ required: true, message: '请输入菜单名称' }]}
          >
            <Input />
          </Form.Item>
          <Form.Item
            name="path"
            label="路由路径"
          >
            <Input />
          </Form.Item>
          <Form.Item
            name="component"
            label="组件名称"
          >
            <Input />
          </Form.Item>
          <Form.Item
            name="icon"
            label="图标"
          >
            <Input />
          </Form.Item>
          <Form.Item
            name="sortOrder"
            label="排序"
          >
            <InputNumber min={0} />
          </Form.Item>
          <Form.Item
            name="menuType"
            label="菜单类型"
            rules={[{ required: true }]}
          >
            <Select>
              <Option value={1}>目录</Option>
              <Option value={2}>菜单</Option>
              <Option value={3}>按钮</Option>
            </Select>
          </Form.Item>
          <Form.Item
            name="permission"
            label="权限标识"
          >
            <Input placeholder="如：doc:upload" />
          </Form.Item>
          <Form.Item
            name="visible"
            label="可见性"
            rules={[{ required: true }]}
          >
            <Select>
              <Option value={1}>显示</Option>
              <Option value={0}>隐藏</Option>
            </Select>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default MenuManagement;
