import axios from 'axios';

const API_BASE = '/api';

// 创建 axios 实例
const apiClient = axios.create({
  baseURL: API_BASE,
  timeout: 60000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 请求拦截器
apiClient.interceptors.request.use(
  (config) => {
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 响应拦截器
apiClient.interceptors.response.use(
  (response) => {
    return response.data;
  },
  (error) => {
    const message = error.response?.data?.message || error.message || '请求失败';
    return Promise.reject(new Error(message));
  }
);

// ========== 文档 API ==========
export const documentApi = {
  // 上传文档
  upload: (formData) => {
    return apiClient.post('/doc/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
  },

  // 获取文档列表
  list: (params) => {
    return apiClient.get('/doc/list', { params });
  },

  // 获取文档详情
  getById: (id) => {
    return apiClient.get(`/doc/${id}`);
  },

  // 删除文档
  delete: (id) => {
    return apiClient.delete(`/doc/${id}`);
  },

  // 获取文档内容
  getContent: (id) => {
    return apiClient.get(`/doc/${id}/content`);
  },
};

// ========== 向量/知识块 API ==========
export const vectorApi = {
  // 获取知识块列表
  listChunks: (params) => {
    return apiClient.get('/doc/chunks', { params });
  },

  // 获取知识块详情
  getChunkById: (id) => {
    return apiClient.get(`/doc/chunks/${id}`);
  },

  // 更新知识块
  updateChunk: (id, data) => {
    return apiClient.put(`/doc/chunks/${id}`, data);
  },

  // 删除知识块
  deleteChunk: (id) => {
    return apiClient.delete(`/doc/chunks/${id}`);
  },

  // 批量删除知识块
  batchDeleteChunks: (ids) => {
    return apiClient.post('/doc/chunks/batch-delete', { ids });
  },

  // 重新向量化
  reindex: (documentId) => {
    return apiClient.post(`/doc/${documentId}/reindex`);
  },
};

// ========== 聊天 API ==========
export const chatApi = {
  // 发送消息（普通请求）
  chat: (data) => {
    return apiClient.post('/chat', data);
  },

  // 流式聊天 - 使用 XMLHttpRequest 实现
  streamChat: (data, onMessage, onError) => {
    return new Promise((resolve, reject) => {
      const xhr = new XMLHttpRequest();
      xhr.open('POST', `${API_BASE}/chat/stream`);
      xhr.setRequestHeader('Content-Type', 'application/json');

      let buffer = '';

      xhr.onprogress = () => {
        const text = xhr.responseText;
        const newContent = text.slice(buffer.length);
        buffer = text;

        // 处理 SSE 格式: data: 内容\n\n
        const lines = newContent.split('\n');
        for (const line of lines) {
          if (line.startsWith('data: ')) {
            const content = line.slice(6).trim();
            if (content === '[DONE]') {
              resolve();
              return;
            }
            // 尝试解析 JSON，如果失败则当作纯文本
            try {
              const parsed = JSON.parse(content);
              onMessage(parsed);
            } catch (e) {
              // SSE 流直接返回字符串内容
              onMessage({ type: 'chunk', content: content });
            }
          }
        }
      };

      xhr.onload = () => {
        if (xhr.status >= 200 && xhr.status < 300) {
          resolve();
        } else {
          reject(new Error(`HTTP error! status: ${xhr.status}`));
        }
      };

      xhr.onerror = () => {
        reject(new Error('网络请求失败'));
      };

      xhr.send(JSON.stringify(data));
    });
  },

  // 获取聊天历史
  getHistory: (params) => {
    return apiClient.get('/chat/history', { params });
  },

  // 清空聊天历史
  clearHistory: () => {
    return apiClient.delete('/chat/history');
  },
};

export default apiClient;
