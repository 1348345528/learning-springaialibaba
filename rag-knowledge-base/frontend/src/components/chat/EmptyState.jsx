import React from 'react';
import { RobotOutlined } from '@ant-design/icons';

const EmptyState = () => {
  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        height: '100%',
        color: '#bfbfbf',
      }}
    >
      <RobotOutlined style={{ fontSize: 48, marginBottom: 16 }} />
      <div>还没有对话记录，请开始提问吧</div>
    </div>
  );
};

export default EmptyState;