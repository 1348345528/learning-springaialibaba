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
  upload: (formData) => {
    return apiClient.post('/doc/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
  },
  list: (params) => {
    return apiClient.get('/doc/documents');
  },
  getById: (id) => {
    return apiClient.get(`/doc/${id}`);
  },
  delete: (id) => {
    return apiClient.delete(`/doc/${id}`);
  },
  getContent: (id) => {
    return apiClient.get(`/doc/${id}/content`);
  },
};

// ========== 向量/知识块 API ==========
export const vectorApi = {
  listChunks: (params) => {
    return apiClient.get('/doc/chunks', { params });
  },
  getChunkById: (id) => {
    return apiClient.get(`/doc/chunks/${id}`);
  },
  updateChunk: (id, data) => {
    return apiClient.put(`/doc/chunks/${id}`, data);
  },
  deleteChunk: (id) => {
    return apiClient.delete(`/doc/chunks/${id}`);
  },
  batchDeleteChunks: (ids) => {
    return apiClient.delete('/doc/chunks/batch', { data: ids });
  },
  reindex: (documentId) => {
    return apiClient.post(`/doc/${documentId}/reindex`);
  },
  // 查询子块
  getChildChunks: (parentId, page = 0, size = 20) => {
    return apiClient.get(`/doc/chunks/${parentId}/children`, { params: { page, size } });
  },
  // 查询父块
  getParentChunk: (childId) => {
    return apiClient.get(`/doc/chunks/${childId}/parent`);
  },
};

// ========== 分块预览与配置 API ==========
export const chunkApi = {
  // 分块预览
  preview: (data) => {
    return apiClient.post('/doc/preview', data, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
  },
  // 获取策略默认配置
  getStrategyConfigs: () => {
    return apiClient.get('/doc/config/defaults');
  },
};

// ========== SSE 事件块解析工具 ==========
// 解析 SSE 事件块
// 标准 SSE 格式：每个 data: 行是完整内容片段，用空行分隔事件
// 所有 data: 内容被拼接在一起，只有 [DONE] 单独返回
export function extractSseEventBlocks(text, remainder = '') {
  // 将上次的 remainder 拼接到文本前面
  const content = remainder + text;

  // 分割行
  const lines = content.split('\n');

  let fullContent = '';  // 累积所有 data: 内容
  let hasDone = false;   // 是否收到 [DONE]

  for (const line of lines) {
    // 跳过空行
    if (!line.trim()) {
      continue;
    }

    // 检查是否是 data: 行
    if (line.startsWith('data:')) {
      const dataContent = line.slice(5).replace(/^\s+/, ''); // 去掉 "data:" 和前导空格

      if (dataContent === '[DONE]') {
        hasDone = true;
      } else if (dataContent) {
        // 拼接内容，用换行分隔
        if (fullContent) {
          fullContent += '\n' + dataContent;
        } else {
          fullContent = dataContent;
        }
      }
    }
    // 非 data: 行，跳过
  }

  // 计算未处理的 remainder
  let newRemainder = '';
  const lastLine = lines[lines.length - 1];

  if (lastLine && lastLine.trim() && lastLine.startsWith('data:')) {
    const dataContent = lastLine.slice(5);
    // 如果最后一行是 data: 但没有内容或不完整，保留
    if (dataContent.trim() === '' || (!hasDone && !lastLine.includes('[DONE]'))) {
      newRemainder = lastLine;
    }
  }

  // 如果收到 [DONE]，返回 [DONE] 作为单独块
  if (hasDone) {
    // 如果有累积内容，先返回内容块，再返回 [DONE] 块
    if (fullContent) {
      return { blocks: [fullContent, '[DONE]'], remainder: '' };
    }
    return { blocks: ['[DONE]'], remainder: '' };
  }

  // 返回累积的内容
  if (fullContent) {
    return { blocks: [fullContent], remainder: newRemainder };
  }
  return { blocks: [], remainder: newRemainder };
}

// ========== 聊天 API ==========
export const chatApi = {
  chat: (data) => {
    return apiClient.post('/chat', data);
  },

  streamChat: (data, onMessage, onError) => {
    const xhr = new XMLHttpRequest();
    let isAborted = false;

    const promise = new Promise((resolve, reject) => {
      xhr.open('POST', `${API_BASE}/chat/stream`);
      xhr.setRequestHeader('Content-Type', 'application/json');

      let lastProcessedLength = 0;
      let remainder = '';

      xhr.onprogress = () => {
        if (isAborted) return;

        const text = xhr.responseText;
        const newText = text.slice(lastProcessedLength);
        lastProcessedLength = text.length;

        const { blocks, remainder: newRemainder } = extractSseEventBlocks(newText, remainder);
        remainder = newRemainder;

        for (const content of blocks) {
          if (content === '[DONE]') {
            onMessage({ type: 'done' });
            resolve();
            return;
          }
          // 尝试解析 JSON，如果失败则当作纯文本
          try {
            const parsed = JSON.parse(content);
            onMessage(parsed);
          } catch (e) {
            onMessage({ type: 'chunk', content: content });
          }
        }
      };

      xhr.onload = () => {
        if (isAborted) return;

        if (xhr.status >= 200 && xhr.status < 300) {
          // 处理最后可能剩余的内容
          const remainingText = xhr.responseText.slice(lastProcessedLength);
          const { blocks } = extractSseEventBlocks(remainingText, remainder);

          for (const content of blocks) {
            if (content === '[DONE]') continue;
            try {
              const parsed = JSON.parse(content);
              onMessage(parsed);
            } catch (e) {
              onMessage({ type: 'chunk', content: content });
            }
          }
          onMessage({ type: 'done' });
          resolve();
        } else {
          reject(new Error(`HTTP error! status: ${xhr.status}`));
        }
      };

      xhr.onerror = () => {
        if (isAborted) return;
        reject(new Error('网络请求失败'));
      };

      xhr.onabort = () => {
        isAborted = true;
        reject(new Error('aborted'));
      };

      xhr.send(JSON.stringify(data));
    });

    // 返回 Promise 和 abort 方法
    return {
      promise,
      abort: () => {
        isAborted = true;
        xhr.abort();
      },
    };
  },

  getHistory: (params) => {
    return apiClient.get('/chat/history', { params });
  },

  clearHistory: () => {
    return apiClient.delete('/chat/history');
  },
};

export default apiClient;
