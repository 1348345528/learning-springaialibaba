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
// 标准 SSE 格式：
// 1. data:content\n\n (单个事件)
// 2. data:first line\ndata:second line\n\n (同一事件的多个data行用换行连接)
// 3. 事件之间用空行分隔
export function extractSseEventBlocks(text, remainder = '') {
  const blocks = [];
  // 将上次的 remainder 拼接到文本前面
  const content = remainder + text;

  // 分割行
  const lines = content.split('\n');

  let i = 0;
  while (i < lines.length) {
    const line = lines[i];

    // 跳过空行（事件分隔符）
    if (!line.trim()) {
      i++;
      continue;
    }

    // 检查是否是 data: 行
    if (line.startsWith('data:')) {
      // 提取 data: 后面的内容
      let dataContent = line.slice(5); // 去掉 "data:"

      // 同一事件的多个 data: 行用换行连接
      // 空行才表示事件结束
      let j = i + 1;
      while (j < lines.length) {
        const nextLine = lines[j];

        // 如果遇到空行，说明事件结束
        if (!nextLine.trim()) {
          break;
        }

        // 如果遇到新的 data: 行，说明当前事件结束
        if (nextLine.startsWith('data:')) {
          break;
        }

        // 否则，这是同一事件的延续内容（换行）
        dataContent += '\n' + nextLine;
        j++;
      }

      // 更新索引
      i = j;

      // 去掉前导空格但保留内部换行
      dataContent = dataContent.replace(/^\s+/, '');

      if (dataContent) {
        blocks.push(dataContent);
      }
    } else {
      // 非 data: 行，跳过（可能是 event: 或其他字段）
      i++;
    }
  }

  // 计算未处理的 remainder
  let newRemainder = '';
  const lastLine = lines[lines.length - 1];

  if (lastLine && !lastLine.trim()) {
    // 最后一行是空行，说明完整的块都已处理
    newRemainder = '';
  } else if (lastLine && !lastLine.startsWith('data:')) {
    // 最后一行不是 data: 开头，可能是被截断的非 data: 行
    newRemainder = lastLine;
  } else if (lastLine && lastLine.startsWith('data:')) {
    // 最后一行是 data: 开头
    const dataContent = lastLine.slice(5);
    if (dataContent.trim() === '') {
      // 只有 "data:" 没有内容，等待更多数据
      newRemainder = lastLine;
    } else if (!lastLine.includes('[DONE]')) {
      // 有内容但不是 [DONE]，可能是不完整的数据块
      // 保留以便下次处理
      newRemainder = lastLine;
    }
    // 如果是 [DONE]，不需要保留
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
