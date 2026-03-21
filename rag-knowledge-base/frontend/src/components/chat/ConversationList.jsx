import React, { useRef, useEffect } from 'react';
import { Spin } from 'antd';
import { UserOutlined, RobotOutlined } from '@ant-design/icons';
import MessageBubble from './MessageBubble';
import EmptyState from './EmptyState';

const ConversationList = ({ messages, loading, streamingId, onRetry }) => {
  const listRef = useRef(null);

  // 自动滚动到底部
  useEffect(() => {
    if (listRef.current) {
      listRef.current.scrollTop = listRef.current.scrollHeight;
    }
  }, [messages, streamingId]);

  // 首次加载
  if (loading && messages.length === 0) {
    return (
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          height: '100%',
        }}
      >
        <Spin tip="加载中..." />
      </div>
    );
  }

  // 空状态
  if (messages.length === 0 && !streamingId) {
    return <EmptyState />;
  }

  // 计算消息间距：同一用户连续消息12px，不同角色消息24px
  const getMessageSpacing = (currentMsg, prevMsg) => {
    if (!prevMsg) return 24; // 第一条消息
    if (prevMsg.role === currentMsg.role) return 12; // 同一用户连续
    return 24; // 不同角色
  };

  return (
    <div
      ref={listRef}
      style={{
        flex: 1,
        overflow: 'auto',
        padding: '24px',
      }}
      role="log"
      aria-live="polite"
    >
      {messages.map((msg, index) => {
        const prevMsg = index > 0 ? messages[index - 1] : null;
        const spacing = getMessageSpacing(msg, prevMsg);
        return (
          <MessageBubble
            key={msg.id}
            message={{ ...msg, spacing }}
            isStreaming={streamingId === msg.id}
            onRetry={msg.status === 'error' ? () => onRetry?.(msg) : undefined}
          />
        );
      })}
    </div>
  );
};

export default ConversationList;