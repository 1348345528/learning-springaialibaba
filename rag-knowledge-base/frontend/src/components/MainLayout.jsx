import React, { useEffect, useState } from 'react';
import { Layout, Menu, Button, Avatar, Dropdown, Spin } from 'antd';
import {
  MenuUnfoldOutlined,
  MenuFoldOutlined,
  UserOutlined,
  LogoutOutlined,
} from '@ant-design/icons';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import useAuthStore from '../store/useAuthStore';

const { Header, Sider, Content } = Layout;

const MainLayout = () => {
  const [collapsed, setCollapsed] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const { user, menus, logout } = useAuthStore();
  const [menuItems, setMenuItems] = useState([]);

  // 递归构建菜单树
  const buildMenuTree = (menus) => {
    if (!menus || !Array.isArray(menus)) return [];
    return menus.map((menu) => ({
      key: menu.path || `menu-${menu.id}`,
      icon: menu.icon ? <span>{menu.icon}</span> : null,
      label: menu.name,
      children: menu.children && menu.children.length > 0 ? buildMenuTree(menu.children) : undefined,
    }));
  };

  useEffect(() => {
    if (menus && menus.length > 0) {
      setMenuItems(buildMenuTree(menus));
    }
  }, [menus]);

  const handleMenuClick = ({ key }) => {
    navigate(key);
  };

  const userMenu = {
    items: [
      {
        key: 'logout',
        icon: <LogoutOutlined />,
        label: '退出登录',
        onClick: logout,
      },
    ],
  };

  // 获取当前选中的菜单 key
  const selectedKeys = [location.pathname];

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider trigger={null} collapsible collapsed={collapsed}>
        <div
          style={{
            height: 64,
            margin: 16,
            color: '#fff',
            fontSize: collapsed ? 16 : 20,
            textAlign: 'center',
            lineHeight: '64px',
            fontWeight: 'bold',
          }}
        >
          {collapsed ? 'RAG' : 'RAG知识库'}
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={selectedKeys}
          items={menuItems}
          onClick={handleMenuClick}
        />
      </Sider>
      <Layout>
        <Header
          style={{
            padding: '0 24px',
            background: '#fff',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
          }}
        >
          <Button
            type="text"
            icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
            onClick={() => setCollapsed(!collapsed)}
            style={{ fontSize: 16, width: 64, height: 64 }}
          />
          <div>
            <Dropdown menu={userMenu} placement="bottomRight">
              <div style={{ cursor: 'pointer' }}>
                <Avatar icon={<UserOutlined />} />
                <span style={{ marginLeft: 8 }}>{user?.username || '用户'}</span>
              </div>
            </Dropdown>
          </div>
        </Header>
        <Content
          style={{
            margin: '24px 16px',
            padding: 24,
            background: '#fff',
            minHeight: 280,
          }}
        >
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
};

export default MainLayout;
