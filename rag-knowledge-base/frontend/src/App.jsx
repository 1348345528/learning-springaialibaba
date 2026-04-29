import React from 'react';
import { ConfigProvider } from 'antd';
import { Routes, Route, Navigate } from 'react-router-dom';
import Login from './pages/Login';
import RequireAuth from './components/RequireAuth';
import MainLayout from './components/MainLayout';
import DocumentUpload from './pages/DocumentUpload';
import ChunkManagement from './pages/ChunkManagement';
import ChatQA from './pages/ChatQA';
import McpManagement from './pages/McpManagement';
import UserManagement from './pages/UserManagement';
import RoleManagement from './pages/RoleManagement';
import MenuManagement from './pages/MenuManagement';
import './styles/markdown.css';

const theme = {
  token: {
    colorPrimary: '#1677ff',
    borderRadius: 6,
  },
};

const App = () => {
  return (
    <ConfigProvider theme={theme}>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route
          path="/"
          element={
            <RequireAuth>
              <MainLayout />
            </RequireAuth>
          }
        >
          <Route index element={<Navigate to="/rag/document" replace />} />
          <Route path="rag/document" element={<DocumentUpload />} />
          <Route path="rag/chunk" element={<ChunkManagement />} />
          <Route path="mcp" element={<McpManagement />} />
          <Route path="chat" element={<ChatQA />} />
          <Route path="sys/user" element={<UserManagement />} />
          <Route path="sys/role" element={<RoleManagement />} />
          <Route path="sys/menu" element={<MenuManagement />} />
        </Route>
      </Routes>
    </ConfigProvider>
  );
};

export default App;
