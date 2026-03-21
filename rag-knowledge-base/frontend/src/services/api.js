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
    return apiClient.post('/doc/chunks/batch-delete', { ids });
  },
  reindex: (documentId) => {
    return apiClient.post(`/doc/${documentId}/reindex`);
  },
};

// ========== SSE 事件块解析工具 ==========
// 解析 SSE 事件块
// 支持两种格式：
// 1. data:content\n\n (标准 SSE)
// 2. data:content\ndata:content2\ndata:[DONE]\n (无空行的多事件)
export function extractSseEventBlocks(text, remainder = '') {
  const blocks = [];
  // 将上次的 remainder 拼接到文本前面
  const content = remainder + text;

  // 分割行
  const lines = content.split('\n');

  let i = 0;
  while (i < lines.length) {
    const line = lines[i];

    // 跳过空行
    if (!line.trim()) {
      i++;
      continue;
    }

    // 检查是否是 data: 行
    if (line.startsWith('data:')) {
      // 提取 data: 后面的内容
      let dataContent = line.slice(5); // 去掉 "data:"

      // 检查下一个非空行
      let j = i + 1;
      while (j < lines.length && !lines[j].startsWith('data:')) {
        // 如果下一行不是 data:，可能是内容的一部分（换行）
        if (lines[j].trim()) {
          dataContent += '\n' + lines[j];
        }
        j++;
      }

      // 更新索引（跳过已处理的非 data: 行）
      if (j > i + 1) {
        i = j - 1;
      }

      // 去掉前导空格但保留内部换行
      dataContent = dataContent.replace(/^\s+/, '');

      if (dataContent) {
        blocks.push(dataContent);
      }
    }
    i++;
  }

  // 计算未处理的 remainder（最后一行如果不以 data: 结尾，可能是被截断的）
  let newRemainder = '';
  const lastLine = lines[lines.length - 1];
  if (lastLine && !lastLine.trim()) {
    // 最后一行是空行，说明完整的块都已处理
    newRemainder = '';
  } else if (lastLine && !lastLine.startsWith('data:')) {
    // 最后一行不是 data: 开头，可能是未完成的 content 行
    newRemainder = lastLine;
  } else if (lastLine && lastLine.startsWith('data:') && !lastLine.includes('[DONE]') && lines[lines.length - 2]?.trim()) {
    // 最后一行是 data: 但可能不完整（比如只有 "data:" 没有内容）
    // 检查是否所有行都被处理了
    const processedAny = blocks.length > 0;
    if (!processedAny && remainder === '' && lastLine === 'data:') {
      newRemainder = '';
    }
  }

  return { blocks, remainder: newRemainder };
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
