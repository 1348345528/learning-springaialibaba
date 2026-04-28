import React, { useState, useRef, useCallback, useEffect } from 'react';
import { Typography, Divider, message, Button } from 'antd';
import { chatApi, conversationApi } from '../services/api';
import ConversationSidebar from '../components/chat/ConversationSidebar';
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
  const [conversations, setConversations] = useState([]);
  const [activeConversationId, setActiveConversationId] = useState(null);
  const [messages, setMessages] = useState([]);
  const [isStreaming, setIsStreaming] = useState(false);
  const [streamingId, setStreamingId] = useState(null);
  const [error, setError] = useState(null);

  const [sidebarVisible, setSidebarVisible] = useState(true);
  const streamRequestRef = useRef(null);

  // 加载会话列表
  useEffect(() => {
    loadConversations();
  }, []);

  const loadConversations = async () => {
    try {
      const data = await conversationApi.list();
      setConversations(data || []);
    } catch (error) {
      console.error('Failed to load conversations:', error);
    }
  };

  // 切换会话
  const handleSelectConversation = useCallback(async (conversationId) => {
    setActiveConversationId(conversationId);
    setMessages([]);
    setError(null);
    setStreamingId(null);

    if (!conversationId) return;

    // 从后端加载会话历史
    try {
      const response = await conversationApi.getMessages(conversationId);
      const data = response.data || [];
      const mapped = data.map((msg, index) => ({
        id: `history-${index}`,
        role: msg.role,
        content: msg.content,
        timestamp: msg.timestamp || Date.now(),
        status: 'success',
      }));
      setMessages(mapped);
    } catch (error) {
      console.error('Failed to load conversation messages:', error);
    }
  }, []);

  // 添加消息
  const addMessage = useCallback((role, content, status = 'success') => {
    const newMessage = {
      id: `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
      role,
      content,
      timestamp: Date.now(),
      status
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

    // 确保有活动会话
    let currentConversationId = activeConversationId;
    if (!currentConversationId) {
      // 创建新会话
      try {
        const newConv = await conversationApi.create('新会话');
        currentConversationId = newConv.conversationId;
        setActiveConversationId(currentConversationId);
        setConversations([newConv, ...conversations]);
      } catch (error) {
        console.error('Failed to create conversation:', error);
        message.error('创建会话失败');
        return;
      }
    }

    const userMessage = text.trim();
    setError(null);
    setIsStreaming(true);

    // 添加用户消息
    addMessage(ROLE.USER, userMessage);

    // 添加空的 AI 消息占位
    const assistantMsgId = addMessage(ROLE.ASSISTANT, '', 'loading');
    setStreamingId(assistantMsgId);

    // 緻加conversationId到请求
    const requestWithConversationId = {
      message: userMessage,
      conversationId: currentConversationId,
    };

    // 累积内容
    let fullContent = '';

    try {
      const { promise, abort } = chatApi.streamChat(
        requestWithConversationId,
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
            // 刷新会话列表（标题和时间戳已更新）
            loadConversations();
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
    setActiveConversationId(null);
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
        height: 'calc(100vh - 64px)',
        background: '#fff',
      }}
    >
      {/* 会话侧边栏 */}
      {sidebarVisible && (
        <ConversationSidebar
          activeConversationId={activeConversationId}
          onSelectConversation={handleSelectConversation}
        />
      )}

      {/* 主聊天区域 */}
      <div
        style={{
          flex: 1,
          display: 'flex',
          flexDirection: 'column',
          overflow: 'hidden',
        }}
      >
        {/* 页面标题 */}
        <div style={{ padding: '24px 24px 0' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <Title level={3} style={{ margin: 0 }}>智能问答</Title>
            <Text type="secondary">基于知识库的智能问答助手</Text>
            <Button
              type="text"
              onClick={() => setSidebarVisible(!sidebarVisible)}
            >
              {sidebarVisible ? '隐藏会话' : '显示会话'}
            </Button>
          </div>
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
    </div>
  );
};

export default ChatQA;
