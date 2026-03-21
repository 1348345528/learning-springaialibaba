import React, { useState, useMemo } from 'react';
import { Button, message, Tooltip } from 'antd';
import { marked } from 'marked';
import {
  CopyOutlined,
  CheckOutlined,
  ExpandOutlined,
  CompressOutlined,
  ReloadOutlined,
} from '@ant-design/icons';

// 配置 marked 选项
marked.setOptions({
  breaks: true,      // 转换 GFM 换行符为 <br>
  gfm: true,         // 启用 GitHub 风格的 Markdown
});

// 自定义渲染规则，确保列表和标题正确显示
const renderer = new marked.Renderer();

// 确保列表项正确渲染
renderer.listitem = function(text) {
  // 检查是否包含任务列表（checkbox）
  if (text.includes('<input')) {
    return `<li class="task-list-item">${text}</li>\n`;
  }
  return `<li>${text}</li>\n`;
};

marked.use({ renderer });

const MessageBubble = ({ message: msg, isStreaming, onRetry }) => {
  const [expanded, setExpanded] = useState(false);
  const [copied, setCopied] = useState(false);

  const isUser = msg.role === 'user';
  const isError = msg.status === 'error';
  const LINE_COUNT_THRESHOLD = 5;
  const lines = (msg.content || '').split('\n');
  const needsCollapse = lines.length > LINE_COUNT_THRESHOLD && !isUser;
  const isCollapsed = needsCollapse && !expanded;

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
  const renderContent = (content, collapse) => {
    if (!content) return null;

    // AI 消息使用 Markdown 渲染
    if (!isUser) {
      const markdownContent = collapse
        ? lines.slice(0, LINE_COUNT_THRESHOLD).join('\n') + '\n...'
        : content;

      // 使用 marked 解析 Markdown
      const htmlContent = marked.parse(markdownContent);

      return (
        <div
          className="markdown-body"
          style={{ fontSize: 14, lineHeight: '22px' }}
          dangerouslySetInnerHTML={{ __html: htmlContent }}
        >
          {isStreaming && <span style={{ display: 'inline-block', animation: 'blink 1s infinite' }}>|</span>}
        </div>
      );
    }

    // 用户消息保持纯文本
    const displayContent = collapse
      ? lines.slice(0, LINE_COUNT_THRESHOLD).join('\n') + '\n...'
      : content;

    return (
      <div style={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word', fontSize: 14, lineHeight: '22px' }}>
        {displayContent}
        {isStreaming && <span style={{ display: 'inline-block', animation: 'blink 1s infinite' }}>|</span>}
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
        {renderContent(msg.content, isCollapsed)}

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
          <div style={{ display: 'flex', gap: 4 }}>
            <Tooltip title={copied ? '已复制' : '复制'}>
              <Button
                type="text"
                size="small"
                icon={copied ? <CheckOutlined /> : <CopyOutlined />}
                onClick={handleCopy}
                style={{ color: '#8c8c8c' }}
              />
            </Tooltip>

            {needsCollapse && (
              <Tooltip title={expanded ? '折叠' : '展开'}>
                <Button
                  type="text"
                  size="small"
                  icon={expanded ? <CompressOutlined /> : <ExpandOutlined />}
                  onClick={() => setExpanded(!expanded)}
                  style={{ color: '#8c8c8c' }}
                />
              </Tooltip>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default MessageBubble;