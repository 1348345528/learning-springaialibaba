import React, { useState } from 'react';
import { Button, message, Tooltip } from 'antd';
import { marked } from 'marked';
import {
  CopyOutlined,
  CheckOutlined,
  ReloadOutlined,
} from '@ant-design/icons';

// 配置 marked 选项
marked.setOptions({
  breaks: true,      // 转换 GFM 换行符为 <br>
  gfm: true,         // 启用 GitHub 风格的 Markdown
});

// 使用默认渲染器
marked.use({});

const MessageBubble = ({ message: msg, isStreaming, onRetry }) => {
  const [copied, setCopied] = useState(false);

  const isUser = msg.role === 'user';
  const isError = msg.status === 'error';

  // 格式化时间戳
  const formatTime = (timestamp) => {
    if (!timestamp) return '';
    const date = new Date(timestamp);
    return date.toLocaleTimeString('zh-CN', {
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  // 渲染内容
  const renderContent = (content) => {
    if (!content) return null;

    // AI 消息
    if (!isUser) {
      // 流式传输中：显示纯文本（保持格式但不解析 Markdown）
      if (isStreaming) {
        return (
          <div style={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word', fontSize: 14, lineHeight: '22px' }}>
            {content}
            <span style={{ display: 'inline-block', animation: 'blink 1s infinite' }}>|</span>
          </div>
        );
      }

      // 流式结束后：渲染 Markdown
      const htmlContent = marked.parse(content);

      return (
        <div
          className="markdown-body"
          style={{ fontSize: 14, lineHeight: '22px' }}
          dangerouslySetInnerHTML={{ __html: htmlContent }}
        />
      );
    }

    // 用户消息保持纯文本
    return (
      <div style={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word', fontSize: 14, lineHeight: '22px' }}>
        {content}
      </div>
    );
  };

  // 复制处理
  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(msg.content);
      setCopied(true);
      message.success('已复制到剪贴板');
      setTimeout(() => setCopied(false), 2000);
    } catch (err) {
      message.error('复制失败');
    }
  };

  // 重试处理
  const handleRetry = () => {
    if (onRetry) {
      onRetry(msg);
    }
  };

  // 计算间距：同一用户连续消息12px，不同角色消息24px
  const getMessageSpacing = () => {
    if (typeof msg.spacing === 'number') {
      return msg.spacing;
    }
    return 12; // 默认间距
  };

  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: isUser ? 'flex-end' : 'flex-start',
        marginBottom: getMessageSpacing(),
      }}
    >
      {/* 气泡主体 */}
      <div
        style={{
          maxWidth: '80%',
          padding: '12px 16px',
          borderRadius: 8,
          background: isError
            ? '#fff2f0'
            : isUser
            ? '#e6f4ff'
            : '#f5f5f5',
          color: isError ? '#ff4d4f' : isUser ? '#1677ff' : '#262626',
          border: isError ? '1px solid #ff4d4f' : isUser ? '1px solid #91caff' : '1px solid #d9d9d9',
        }}
      >
        {renderContent(msg.content)}

        {/* 错误状态显示重试按钮 */}
        {isError && (
          <Button
            type="link"
            size="small"
            icon={<ReloadOutlined />}
            onClick={handleRetry}
            style={{ padding: 0, marginTop: 8 }}
          >
            重试
          </Button>
        )}
      </div>

      {/* 底部信息栏 */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: 8,
          marginTop: 4,
          padding: '0 4px',
        }}
      >
        {/* 时间戳 */}
        <span style={{ fontSize: 12, color: '#bfbfbf' }}>{formatTime(msg.timestamp)}</span>

        {/* 操作按钮 */}
        {!isUser && !isStreaming && msg.status !== 'loading' && (
          <Tooltip title={copied ? '已复制' : '复制'}>
            <Button
              type="text"
              size="small"
              icon={copied ? <CheckOutlined /> : <CopyOutlined />}
              onClick={handleCopy}
              style={{ color: '#8c8c8c' }}
            />
          </Tooltip>
        )}
      </div>
    </div>
  );
};

export default MessageBubble;