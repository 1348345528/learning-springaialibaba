import React, { useState } from 'react';
import { Form, Input, Button, Card, message, Typography } from 'antd';
import { UserOutlined, LockOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { authService } from '../services/api';
import useAuthStore from '../store/useAuthStore';

const { Title } = Typography;

const Login = () => {
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const setAuth = useAuthStore((state) => state.setAuth);
  const setUser = useAuthStore((state) => state.setUser);
  const setMenus = useAuthStore((state) => state.setMenus);
  const setPermissions = useAuthStore((state) => state.setPermissions);

  const onFinish = async (values) => {
    setLoading(true);
    try {
      const { data } = await authService.login(values.username, values.password);
      const { token, user } = data;
      setAuth(token, user);

      // 获取用户详细信息和菜单
      const [userInfoRes, menuRes] = await Promise.all([
        authService.getUserInfo(),
        authService.getMenuTree(),
      ]);

      setUser(userInfoRes.data);
      setMenus(menuRes.data);
      setPermissions(userInfoRes.data.permissions || []);

      message.success('登录成功');
      navigate('/');
    } catch (error) {
      message.error(error.response?.data?.message || '登录失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      style={{
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        height: '100vh',
        background: '#f0f2f5',
      }}
    >
      <Card style={{ width: 400, boxShadow: '0 4px 12px rgba(0,0,0,0.1)' }}>
        <div style={{ textAlign: 'center', marginBottom: 32 }}>
          <Title level={3}>RAG 知识库系统</Title>
          <Title level={5} type="secondary">
            登录
          </Title>
        </div>
        <Form name="login" onFinish={onFinish} autoComplete="off">
          <Form.Item
            name="username"
            rules={[{ required: true, message: '请输入用户名' }]}
          >
            <Input
              prefix={<UserOutlined />}
              placeholder="用户名"
              size="large"
            />
          </Form.Item>
          <Form.Item
            name="password"
            rules={[{ required: true, message: '请输入密码' }]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="密码"
              size="large"
            />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" loading={loading} block size="large">
              登录
            </Button>
          </Form.Item>
        </Form>
        <div style={{ textAlign: 'center', color: '#999', marginTop: 16 }}>
          默认账号: admin / admin123  |  user / user123
        </div>
      </Card>
    </div>
  );
};

export default Login;
