import React, { useState, useRef, useEffect } from 'react';
import {
  Card,
  Input,
  Button,
  Space,
  Typography,
  Spin,
  Tag,
  Divider,
  Empty,
  Tooltip,
  message,
} from 'antd';
import {
  SendOutlined,
  ClearOutlined,
  LoadingOutlined,
  RobotOutlined,
  UserOutlined,
  DatabaseOutlined,
} from '@ant-design/icons';
import { chatApi } from '../services/api';

const { Title, Text } = Typography;
const { TextArea } = Input;

const ChatTest = () => {
  const [messages, setMessages] = useState([]);
  const [inputValue, setInputValue] = useState('');
  const [loading, setLoading] = useState(false);
  const [streamResponse, setStreamResponse] = useState('');
  const [showSources, setShowSources] = useState(true);
  const messagesEndRef = useRef(null);

  // 自动滚动到底部
  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages, streamResponse]);

  // 发送消息
  const handleSend = () => {
    if (!inputValue.trim() || loading) return;

    const userMessage = {
      id: Date.now(),
      role: 'user',
      content: inputValue.trim(),
      timestamp: new Date(),
    };

    setMessages((prev) => [...prev, userMessage]);
    setInputValue('');
    setStreamResponse('');
    setLoading(true);

    // 使用流式API
    chatApi.streamChat(
      { message: userMessage.content },
      (data) => {
        // 处理 JSON 格式的响应
        if (data && data.type === 'chunk') {
          setStreamResponse((prev) => prev + data.content);
        } else if (data && data.type === 'done') {
          const assistantMessage = {
            id: Date.now() + 1,
            role: 'assistant',
            content: data.fullResponse || streamResponse,
            sources: data.sources || [],
            timestamp: new Date(),
          };
          setMessages((prev) => [...prev, assistantMessage]);
          setStreamResponse('');
          setLoading(false);
        } else if (data && data.type === 'error') {
          message.error(data.message || '响应出错');
          setLoading(false);
        }
        // 处理直接返回的字符串内容 (SSE plain text)
        else if (typeof data === 'string') {
          setStreamResponse((prev) => prev + data);
        }
      },
      (error) => {
        message.error(error.message || '请求失败');
        setLoading(false);
      }
    );
  };

  // 处理按键
  const handleKeyPress = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  // 清空聊天记录
  const handleClear = () => {
    setMessages([]);
    setStreamResponse('');
  };

  // 重新发送
  const handleResend = (message) => {
    if (message.role === 'user') {
      setInputValue(message.content);
    }
  };

  // 渲染消息内容
  const renderMessageContent = (content) => {
    return content.split('\n').map((line, i) => (
      <React.Fragment key={i}>
        {line}
        {i < content.split('\n').length - 1 && <br />}
      </React.Fragment>
    ));
  };

  return (
    <div style={{ padding: '24px', maxWidth: 1000, margin: '0 auto', height: 'calc(100vh - 100px)', display: 'flex', flexDirection: 'column' }}>
      <Title level={3}>问答测试</Title>
      <Text type="secondary">基于知识库进行问答测试</Text>

      <Divider />

      {/* 聊天区域 */}
      <Card
        style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}
        bodyStyle={{ flex: 1, display: 'flex', flexDirection: 'column', padding: 0, overflow: 'hidden' }}
      >
        {/* 消息列表 */}
        <div style={{ flex: 1, overflow: 'auto', padding: '16px' }}>
          {messages.length === 0 && !loading ? (
            <Empty
              image={Empty.PRESENTED_IMAGE_SIMPLE}
              description="开始提问吧"
            >
              <Text type="secondary">基于已上传的文档进行问答</Text>
            </Empty>
          ) : (
            messages.map((msg) => (
              <div
                key={msg.id}
                style={{
                  display: 'flex',
                  marginBottom: 16,
                  justifyContent: msg.role === 'user' ? 'flex-end' : 'flex-start',
                }}
              >
                <div
                  style={{
                    maxWidth: '70%',
                    display: 'flex',
                    flexDirection: 'row',
                    alignItems: 'flex-start',
                    gap: 8,
                  }}
                >
                  {msg.role === 'assistant' && (
                    <div
                      style={{
                        width: 32,
                        height: 32,
                        borderRadius: '50%',
                        background: '#1890ff',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        color: '#fff',
                        flexShrink: 0,
                      }}
                    >
                      <RobotOutlined />
                    </div>
                  )}

                  <div
                    style={{
                      background: msg.role === 'user' ? '#1890ff' : '#f5f5f5',
                      color: msg.role === 'user' ? '#fff' : '#000',
                      padding: '12px 16px',
                      borderRadius: 12,
                      wordBreak: 'break-word',
                      whiteSpace: 'pre-wrap',
                    }}
                  >
                    {msg.role === 'user' ? (
                      <div>{renderMessageContent(msg.content)}</div>
                    ) : (
                      <>
                        <div>{renderMessageContent(msg.content)}</div>

                        {/* 来源信息 */}
                        {msg.sources && msg.sources.length > 0 && showSources && (
                          <div style={{ marginTop: 12, paddingTop: 12, borderTop: '1px solid #e8e8e8' }}>
                            <Space>
                              <DatabaseOutlined />
                              <Text type="secondary" style={{ fontSize: 12 }}>参考文档：</Text>
                              {msg.sources.map((source, idx) => (
                                <Tag key={idx} color="blue" style={{ margin: 0 }}>
                                  {source.fileName || source.chunkIndex}
                                </Tag>
                              ))}
                            </Space>
                          </div>
                        )}
                      </>
                    )}
                  </div>

                  {msg.role === 'user' && (
                    <div
                      style={{
                        width: 32,
                        height: 32,
                        borderRadius: '50%',
                        background: '#52c41a',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        color: '#fff',
                        flexShrink: 0,
                      }}
                    >
                      <UserOutlined />
                    </div>
                  )}
                </div>
              </div>
            ))
          )}

          {/* 流式响应显示 */}
          {streamResponse && (
            <div
              style={{
                display: 'flex',
                marginBottom: 16,
                justifyContent: 'flex-start',
              }}
            >
              <div
                style={{
                  maxWidth: '70%',
                  display: 'flex',
                  flexDirection: 'row',
                  alignItems: 'flex-start',
                  gap: 8,
                }}
              >
                <div
                  style={{
                    width: 32,
                    height: 32,
                    borderRadius: '50%',
                    background: '#1890ff',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    color: '#fff',
                    flexShrink: 0,
                  }}
                >
                  <RobotOutlined />
                </div>
                <div
                  style={{
                    background: '#f5f5f5',
                    padding: '12px 16px',
                    borderRadius: 12,
                    wordBreak: 'break-word',
                    whiteSpace: 'pre-wrap',
                  }}
                >
                  {renderMessageContent(streamResponse)}
                  <Spin indicator={<LoadingOutlined spin />} style={{ marginLeft: 8 }} />
                </div>
              </div>
            </div>
          )}

          <div ref={messagesEndRef} />
        </div>

        {/* 输入区域 */}
        <div
          style={{
            padding: '16px',
            borderTop: '1px solid #f0f0f0',
            background: '#fff',
          }}
        >
          <Space direction="vertical" style={{ width: '100%' }}>
            <TextArea
              value={inputValue}
              onChange={(e) => setInputValue(e.target.value)}
              onKeyPress={handleKeyPress}
              placeholder="输入问题，按 Enter 发送，Shift + Enter 换行"
              autoSize={{ minRows: 1, maxRows: 4 }}
              disabled={loading}
              style={{ borderRadius: 8 }}
            />
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <Space>
                <Button
                  icon={<ClearOutlined />}
                  onClick={handleClear}
                  disabled={loading || messages.length === 0}
                >
                  清空
                </Button>
                <Tooltip title={showSources ? '隐藏参考文档' : '显示参考文档'}>
                  <Button
                    icon={<DatabaseOutlined />}
                    onClick={() => setShowSources(!showSources)}
                    type={showSources ? 'primary' : 'default'}
                  >
                    {showSources ? '显示来源' : '隐藏来源'}
                  </Button>
                </Tooltip>
              </Space>
              <Button
                type="primary"
                icon={<SendOutlined />}
                onClick={handleSend}
                loading={loading}
                disabled={!inputValue.trim()}
              >
                发送
              </Button>
            </div>
          </Space>
        </div>
      </Card>
    </div>
  );
};

export default ChatTest;
