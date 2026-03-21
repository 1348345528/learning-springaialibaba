import React, { useState, useEffect } from 'react';
import { Input, Button, Space, Modal } from 'antd';
import { SendOutlined, StopOutlined, ClearOutlined } from '@ant-design/icons';

const { TextArea } = Input;

const InputArea = ({ onSend, onReset, disabled, maxLength = 500, messageCount = 0 }) => {
  const [value, setValue] = useState('');
  const [showResetConfirm, setShowResetConfirm] = useState(false);

  // 输入校验
  const INVALID_CHARS = /[<>{}]/;
  const isValidInput = (val) => {
    if (!val.trim()) return { valid: false, error: '请输入问题' };
    if (INVALID_CHARS.test(val)) return { valid: false, error: '输入包含非法字符' };
    return { valid: true };
  };

  // 字符统计
  const charCount = value.length;
  const isNearLimit = charCount > maxLength * 0.8;
  const isOverLimit = charCount > maxLength;

  // 发送处理
  const handleSend = () => {
    const { valid, error } = isValidInput(value);
    if (!valid) {
      return;
    }
    onSend(value.trim());
    setValue('');
  };

  // 重置确认
  const handleResetClick = () => {
    if (value.trim()) {
      setShowResetConfirm(true);
    } else {
      onReset();
    }
  };

  const confirmReset = () => {
    setValue('');
    setShowResetConfirm(false);
    onReset();
  };

  // 键盘事件
  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey && !disabled) {
      e.preventDefault();
      handleSend();
    }
  };

  return (
    <>
      <div
        style={{
          padding: '12px 16px',
          background: '#fff',
          borderTop: '1px solid #f0f0f0',
        }}
      >
        <Space direction="vertical" style={{ width: '100%' }} size="middle">
          {/* 输入框 */}
          <div>
            <TextArea
              value={value}
              onChange={(e) => setValue(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="请输入您的问题，按 Enter 发送..."
              autoSize={{ minRows: 2, maxRows: 6 }}
              disabled={disabled}
              maxLength={maxLength + 100}
              aria-label="问题输入框"
              style={{
                borderColor: isOverLimit ? '#ff4d4f' : isNearLimit ? '#faad14' : '#d9d9d9',
              }}
            />
          </div>

          {/* 底部操作栏 */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            {/* 字数提示 */}
            <span
              style={{
                fontSize: 12,
                color: isOverLimit ? '#ff4d4f' : isNearLimit ? '#faad14' : '#bfbfbf',
              }}
            >
              {charCount}/{maxLength}
            </span>

            {/* 按钮组 */}
            <Space>
              {disabled ? (
                <Button
                  danger
                  icon={<StopOutlined />}
                  onClick={onReset}
                  aria-label="停止生成"
                >
                  停止
                </Button>
              ) : (
                <Button
                  type="primary"
                  icon={<SendOutlined />}
                  onClick={handleSend}
                  disabled={!value.trim() || isOverLimit}
                  aria-label="发送消息"
                >
                  发送
                </Button>
              )}
              <Button
                icon={<ClearOutlined />}
                onClick={handleResetClick}
                disabled={messageCount === 0}
                aria-label="清空对话"
              >
                重置
              </Button>
            </Space>
          </div>
        </Space>
      </div>

      {/* 重置确认弹窗 */}
      <Modal
        title="确认清空"
        open={showResetConfirm}
        onOk={confirmReset}
        onCancel={() => setShowResetConfirm(false)}
        okText="确认"
        cancelText="取消"
      >
        <p>确认清空对话记录？此操作不可恢复。</p>
      </Modal>
    </>
  );
};

export default InputArea;