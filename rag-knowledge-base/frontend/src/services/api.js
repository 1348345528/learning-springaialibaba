import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '';

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
});

// 请求拦截器：添加 token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// 响应拦截器：处理 401
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default api;

export const authService = {
  login: (username, password) => api.post('/auth/login', { username, password }),
  getUserInfo: () => api.get('/auth/user/info'),
  getMenuTree: () => api.get('/auth/menu/tree'),
};

export const userService = {
  list: () => api.get('/sys/user'),
  create: (data) => api.post('/sys/user', data),
  update: (id, data) => api.put(`/sys/user/${id}`, data),
  delete: (id) => api.delete(`/sys/user/${id}`),
  assignRoles: (userId, roleIds) => api.post(`/sys/user/${userId}/roles`, { roleIds }),
};

export const roleService = {
  list: () => api.get('/sys/role'),
  create: (data) => api.post('/sys/role', data),
  update: (id, data) => api.put(`/sys/role/${id}`, data),
  delete: (id) => api.delete(`/sys/role/${id}`),
  assignMenus: (roleId, menuIds) => api.post(`/sys/role/${roleId}/menus`, { menuIds }),
};

export const menuService = {
  list: () => api.get('/sys/menu'),
  create: (data) => api.post('/sys/menu', data),
  update: (id, data) => api.put(`/sys/menu/${id}`, data),
  delete: (id) => api.delete(`/sys/menu/${id}`),
};

// 会话管理 API
export const conversationApi = {
  list: () => api.get('/api/conversations'),
  create: (title) => api.post('/api/conversations', { title }),
  delete: (conversationId) => api.delete(`/api/conversations/${conversationId}`),
  updateTitle: (conversationId, title) => api.put(`/api/conversations/${conversationId}/title`, title, {
    headers: { 'Content-Type': 'text/plain' },
  }),
  getMessages: (conversationId) => api.get(`/api/conversations/${conversationId}/messages`),
};

// 聊天 API（SSE 流式）
export const chatApi = {
  streamChat: (request, onData, onError) => {
    const xhr = new XMLHttpRequest();
    xhr.open('POST', '/api/chat/stream');
    xhr.setRequestHeader('Content-Type', 'application/json');
    const token = localStorage.getItem('token');
    if (token) {
      xhr.setRequestHeader('Authorization', `Bearer ${token}`);
    }

    let lastIndex = 0;
    xhr.onprogress = () => {
      const newData = xhr.responseText.substring(lastIndex);
      lastIndex = xhr.responseText.length;

      const lines = newData.split('\n').filter(Boolean);
      for (const line of lines) {
        if (line.startsWith('data:')) {
          const data = line.substring(5).trim();
          if (data === '[DONE]') {
            onData({ type: 'done' });
          } else if (data.startsWith('{')) {
            // JSON 对象：尝试解析提取 content/response
            try {
              const parsed = JSON.parse(data);
              onData({ type: 'chunk', content: parsed.content || parsed.response || data });
            } catch {
              onData({ type: 'chunk', content: data });
            }
          } else {
            // 纯文本（含纯数字如 "18"），直接使用
            onData({ type: 'chunk', content: data });
          }
        }
      }
    };

    xhr.onerror = () => onError('网络连接失败');
    xhr.onabort = () => onError('aborted');

    xhr.send(JSON.stringify(request));

    return {
      promise: new Promise((resolve, reject) => {
        xhr.onloadend = () => {
          if (xhr.status >= 200 && xhr.status < 300) {
            resolve();
          } else {
            reject(new Error(xhr.responseText || '请求失败'));
          }
        };
      }),
      abort: () => xhr.abort(),
    };
  },
};

// 文档管理 API
export const documentApi = {
  list: () => api.get('/api/doc/documents'),
};

// 分块预览 API
export const chunkApi = {
  preview: (data) => api.post('/api/chunk/preview', data),
};

// 向量知识块管理 API
export const vectorApi = {
  listChunks: (params) => api.get('/api/doc/chunks', { params }),
  updateChunk: (id, data) => api.put(`/api/doc/chunks/${id}`, data),
  deleteChunk: (id) => api.delete(`/api/doc/chunks/${id}`),
  batchDeleteChunks: (ids) => api.post('/api/doc/chunks/batch-delete', { ids }),
  reindex: (documentId) => api.post('/api/doc/chunks/reindex', { documentId }),
};

// MCP Server 管理 API
export const mcpApi = {
  listServers: () => api.get('/api/mcp/servers'),
  register: (data) => api.post('/api/mcp/servers', data),
  unregister: (id) => api.delete(`/api/mcp/servers/${id}`),
  reconnect: (id, data) => api.post(`/api/mcp/servers/${id}/reconnect`, data),
  listTools: () => api.get('/api/mcp/tools'),
};
