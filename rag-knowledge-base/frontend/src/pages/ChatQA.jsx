import React, { useState, useRef, useCallback } from 'react';
import { Typography, Divider, message } from 'antd';
import { chatApi } from '../services/api';
import ConversationList from '../components/chat/ConversationList';
import InputArea from '../components/chat/InputArea';

const { Title, Text } = Typography;

// 消息角色
const ROLE = {
  USER: 'user',
  ASSISTANT: 'assistant',
};

// 输入校验：限制非法字符
const INVALID_CHARS = /[<>{}]/;
const MAX_INPUT_LENGTH = 500;

const ChatQA = () => {
  const [messages, setMessages] = useState([]);
  const [isStreaming, setIsStreaming] = useState(false);
  const [streamingId, setStreamingId] = useState(null);
  const [error, setError] = useState(null);

  const streamRequestRef = useRef(null);

  // 添加消息
  const addMessage = useCallback((role, content, status = 'success') => {
    const newMessage = {
      id: `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
      role,
      content,
      timestamp: Date.now(),
      status,
    };
    setMessages((prev) => [...prev, newMessage]);
    return newMessage.id;
  }, []);

  // 发送消息
  const handleSend = async (text) => {
    // 输入校验
    if (!text || !text.trim()) {
      message.warning('请输入问题');
      return;
    }
    if (INVALID_CHARS.test(text)) {
      message.warning('输入包含非法字符');
      return;
    }
    if (text.length > MAX_INPUT_LENGTH) {
      message.warning(`输入过长，最多 ${MAX_INPUT_LENGTH} 个字符`);
      return;
    }

    const userMessage = text.trim();
    setError(null);
    setIsStreaming(true);

    // 添加用户消息
    addMessage(ROLE.USER, userMessage);

    // 添加空的 AI 消息占位
    const assistantMsgId = addMessage(ROLE.ASSISTANT, '', 'loading');
    setStreamingId(assistantMsgId);

    // 累积内容
    let fullContent = '';

    try {
      const { promise, abort } = chatApi.streamChat(
        { message: userMessage },
        (data) => {
          if (data.type === 'done') {
            // 流结束
            setMessages((prev) =>
              prev.map((msg) =>
                msg.id === assistantMsgId
                  ? { ...msg, content: fullContent, status: 'success' }
                  : msg
              )
            );
            setStreamingId(null);
          } else if (data.type === 'chunk') {
            // 纯文本块
            fullContent += data.content;
            setMessages((prev) =>
              prev.map((msg) =>
                msg.id === assistantMsgId
                  ? { ...msg, content: fullContent }
                  : msg
              )
            );
          } else if (data.content !== undefined) {
            // JSON 响应
            fullContent += data.content;
            setMessages((prev) =>
              prev.map((msg) =>
                msg.id === assistantMsgId
                  ? { ...msg, content: fullContent }
                  : msg
              )
            );
          }
        },
        (errorMsg) => {
          throw new Error(errorMsg);
        }
      );

      streamRequestRef.current = { promise, abort };
      await promise;
    } catch (err) {
      // 处理取消
      if (err.message === 'aborted') {
        if (fullContent) {
          setMessages((prev) =>
            prev.map((msg) =>
              msg.id === assistantMsgId
                ? { ...msg, content: fullContent + ' [已停止]', status: 'success' }
                : msg
            )
          );
        }
        setStreamingId(null);
        return;
      }

      // 处理错误
      setMessages((prev) =>
        prev.map((msg) =>
          msg.id === assistantMsgId
            ? { ...msg, content: err.message || '抱歉，服务端出错', status: 'error' }
            : msg
        )
      );
      setError(err.message);
      setStreamingId(null);
    } finally {
      setIsStreaming(false);
      streamRequestRef.current = null;
    }
  };

  // 停止生成
  const handleStop = () => {
    if (streamRequestRef.current) {
      streamRequestRef.current.abort();
      setIsStreaming(false);
    }
  };

  // 重置对话
  const handleReset = () => {
    setMessages([]);
    setError(null);
    setStreamingId(null);
  };

  // 重试失败的消息
  const handleRetry = (failedMsg) => {
    // 移除失败消息
    setMessages((prev) => prev.filter((msg) => msg.id !== failedMsg.id));
    // 重新发送
    handleSend(failedMsg.content);
  };

  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        height: 'calc(100vh - 64px)',
        background: '#fff',
      }}
    >
      {/* 页面标题 */}
      <div style={{ padding: '24px 24px 0' }}>
        <Title level={3} style={{ margin: 0 }}>智能问答</Title>
        <Text type="secondary">基于知识库的智能问答助手</Text>
        <Divider style={{ marginTop: 16, marginBottom: 0 }} />
      </div>

      {/* 错误提示 */}
      {error && !isStreaming && (
        <div
          style={{
            margin: '0 24px 16px',
            padding: '8px 12px',
            background: '#fff2f0',
            border: '1px solid #ff4d4f',
            borderRadius: 4,
            color: '#ff4d4f',
          }}
        >
          抱歉，网络连接失败，请重试
        </div>
      )}

      {/* 对话列表 */}
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
        <ConversationList
          messages={messages}
          loading={false}
          streamingId={streamingId}
          onRetry={handleRetry}
        />
      </div>

      {/* 输入区域 */}
      <InputArea
        onSend={handleSend}
        onReset={isStreaming ? handleStop : handleReset}
        disabled={isStreaming}
        maxLength={MAX_INPUT_LENGTH}
        messageCount={messages.length}
      />

      {/* CSS 动画 */}
      <style>{`
        @keyframes blink {
          0%, 100% { opacity: 1; }
          50% { opacity: 0; }
        }
      `}</style>
    </div>
  );
};

export default ChatQA;