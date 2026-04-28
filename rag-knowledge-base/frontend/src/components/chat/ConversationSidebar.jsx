import React, { useState, useEffect } from 'react';
import { List, Button, Modal, Input, Typography, Empty, Popconfirm, message } from 'antd';
import { PlusOutlined, DeleteOutlined, EditOutlined, MessageOutlined } from '@ant-design/icons';
import { conversationApi } from '../../services/api';

const { Text } = Typography;

/**
 * 会话侧边栏组件
 */
const ConversationSidebar = ({ activeConversationId, onSelectConversation }) => {
  const [conversations, setConversations] = useState([]);
  const [loading, setLoading] = useState(false);
  const [editModalVisible, setEditModalVisible] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [editTitle, setEditTitle] = useState('');

  // 加载会话列表
  useEffect(() => {
    loadConversations();
  }, []);

  const loadConversations = async () => {
    setLoading(true);
    try {
      const response = await conversationApi.list();
      setConversations(response.data || []);
    } catch (error) {
      console.error('Failed to load conversations:', error);
      setConversations([]);
    } finally {
      setLoading(false);
    }
  };

  // 创建新会话
  const handleCreate = async () => {
    try {
      const response = await conversationApi.create('New Conversation');
      const newConv = response.data;
      setConversations([newConv, ...conversations]);
      onSelectConversation(newConv.conversationId);
    } catch (error) {
      console.error('Failed to create conversation:', error);
      message.error('创建会话失败');
    }
  };

  // 删除会话
  const handleDelete = async (conversationId) => {
    try {
      await conversationApi.delete(conversationId);
      setConversations(conversations.filter(c => c.conversationId !== conversationId));
      if (activeConversationId === conversationId) {
        onSelectConversation(null);
      }
      message.success('会话已删除');
    } catch (error) {
      console.error('Failed to delete conversation:', error);
      message.error('删除会话失败');
    }
  };

  // 更新标题
  const handleUpdateTitle = async () => {
    if (!editingId || !editTitle.trim()) return;
    try {
      await conversationApi.updateTitle(editingId, editTitle.trim());
      setConversations(conversations.map(c =>
        c.conversationId === editingId ? { ...c, title: editTitle.trim() } : c
      ));
      setEditModalVisible(false);
      setEditingId(null);
      setEditTitle('');
      message.success('标题已更新');
    } catch (error) {
      console.error('Failed to update title:', error);
      message.error('更新标题失败');
    }
  };

  // 格式化时间
  const formatTime = (timestamp) => {
    if (!timestamp) return '';
    const date = new Date(timestamp);
    return date.toLocaleString('zh-CN', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  return (
    <div
      style={{
        width: 260,
        height: '100%',
        borderRight: '1px solid #e8e8e8',
        display: 'flex',
        flexDirection: 'column',
        background: '#fafafa',
        overflow: 'hidden',
      }}
    >
      {/* 头部:新建会话按钮 */}
      <div
        style={{
          padding: '12px 16px',
          borderBottom: '1px solid #e8e8e8',
          display: 'flex',
          alignItems: 'center',
        }}
      >
        <Button
          type="primary"
          icon={<PlusOutlined />}
          onClick={handleCreate}
          block
        >
          新建会话
        </Button>
      </div>

      {/* 会话列表 */}
      <div
        style={{
          flex: 1,
          overflow: 'auto',
          padding: '8px 0',
        }}
      >
        {loading ? (
          <div style={{ textAlign: 'center', padding: '24px' }}>
            <Empty description="加载中..." />
          </div>
        ) : conversations.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '24px' }}>
            <Empty description="暂无会话" image={Empty.PRESENTED_IMAGE_SIMPLE} />
          </div>
        ) : (
          <List
            dataSource={conversations}
            renderItem={(item) => (
              <List.Item
                key={item.conversationId}
                onClick={() => onSelectConversation(item.conversationId)}
                style={{
                  padding: '12px 16px',
                  cursor: 'pointer',
                  backgroundColor: activeConversationId === item.conversationId ? '#e6f7ff' : 'transparent',
                  borderRadius: '4px',
                  marginBottom: '4px',
                  transition: 'background-color 0.3s',
                }}
                actions={[
                  <Button
                    key="edit"
                    type="text"
                    size="small"
                    icon={<EditOutlined />}
                    onClick={(e) => {
                      e.stopPropagation();
                      setEditingId(item.conversationId);
                      setEditTitle(item.title);
                      setEditModalVisible(true);
                    }}
                  />,
                  <Popconfirm
                    key="delete"
                    title="确定要删除此会话吗？"
                    onConfirm={(e) => {
                      e.stopPropagation();
                      handleDelete(item.conversationId);
                    }}
                    okText="确定"
                    cancelText="取消"
                  >
                    <Button
                      type="text"
                      danger
                      size="small"
                      icon={<DeleteOutlined />}
                      onClick={(e) => e.stopPropagation()}
                    />
                  </Popconfirm>
                ]}
              >
                <List.Item.Meta
                  avatar={<MessageOutlined style={{ color: '#1890ff' }} />}
                  title={<Text ellipsis style={{ margin: 0 }}>{item.title || '新会话'}</Text>}
                  description={<Text type="secondary" style={{ fontSize: '12px' }}>{formatTime(item.updatedAt)}</Text>}
                />
              </List.Item>
            )}
          />
        )}
      </div>

      {/* 编辑标题弹窗 */}
      <Modal
        title="编辑会话标题"
        open={editModalVisible}
        onOk={handleUpdateTitle}
        onCancel={() => {
          setEditModalVisible(false);
          setEditingId(null);
          setEditTitle('');
        }}
        okText="确定"
        cancelText="取消"
      >
        <Input
          placeholder="请输入新标题"
          value={editTitle}
          onChange={(e) => setEditTitle(e.target.value)}
          onPressEnter={handleUpdateTitle}
          autoFocus
        />
      </Modal>
    </div>
  );
};

export default ConversationSidebar;
