import React, { useState } from 'react';
import { ConfigProvider, Layout, Menu, Typography } from 'antd';
import {
  UploadOutlined,
  DatabaseOutlined,
  FileTextOutlined,
  RobotOutlined,
} from '@ant-design/icons';

import DocumentUpload from './pages/DocumentUpload';
import ChunkManagement from './pages/ChunkManagement';
import ChatQA from './pages/ChatQA';
import './styles/markdown.css';

const { Header, Content, Sider } = Layout;
const { Title } = Typography;

// 主题配置
const theme = {
  token: {
    colorPrimary: '#1677ff',
    borderRadius: 6,
  },
};

const App = () => {
  const [currentKey, setCurrentKey] = useState('upload');

  // 菜单项配置
  const menuItems = [
    {
      key: 'upload',
      icon: <UploadOutlined />,
      label: '文档上传',
    },
    {
      key: 'chunks',
      icon: <DatabaseOutlined />,
      label: '知识块管理',
    },
    {
      key: 'chat',
      icon: <RobotOutlined />,
      label: '智能问答',
    },
  ];

  // 渲染当前页面
  const renderContent = () => {
    switch (currentKey) {
      case 'upload':
        return <DocumentUpload />;
      case 'chunks':
        return <ChunkManagement />;
      case 'chat':
        return <ChatQA />;
      default:
        return <DocumentUpload />;
    }
  };

  return (
    <ConfigProvider theme={theme}>
      <Layout style={{ minHeight: '100vh' }}>
        <Header
          style={{
            display: 'flex',
            alignItems: 'center',
            padding: '0 24px',
            background: '#fff',
            borderBottom: '1px solid #f0f0f0',
          }}
        >
          <FileTextOutlined style={{ fontSize: 24, marginRight: 12, color: '#1677ff' }} />
          <Title level={4} style={{ margin: 0 }}>RAG 知识库系统</Title>
        </Header>

        <Layout>
          <Sider
            width={200}
            style={{
              background: '#fff',
              borderRight: '1px solid #f0f0f0',
            }}
          >
            <Menu
              mode="inline"
              selectedKeys={[currentKey]}
              onClick={(e) => setCurrentKey(e.key)}
              items={menuItems}
              style={{
                height: '100%',
                borderRight: 0,
              }}
            />
          </Sider>

          <Layout style={{ padding: 0, background: '#f5f5f5' }}>
            <Content
              style={{
                background: '#fff',
                minHeight: 'calc(100vh - 64px)',
                overflow: 'auto',
              }}
            >
              {renderContent()}
            </Content>
          </Layout>
        </Layout>
      </Layout>
    </ConfigProvider>
  );
};

export default App;
