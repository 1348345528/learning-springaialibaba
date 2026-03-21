# Frontend Streaming Chat Compatibility Fix Design

**日期：** 2026-03-21
**状态：** 已批准

---

## 目标
修复前端聊天页无法正确展示流式输出的问题，并保证输出格式稳定、可兼容，能够同时处理纯文本 SSE 与结构化 JSON SSE 两种后端返回格式。

## 问题背景
当前后端 `/api/chat/stream` 已能够返回 SSE 文本流，但前端页面存在以下问题：

1. 仅在收到 `done` 事件时才将最终回答落入消息列表
2. 对当前后端返回的纯文本 `data: ...` SSE 支持不完整
3. 请求自然结束但没有 `done` 时，页面可能一直处于流式中间态
4. 输出格式缺乏统一归一化，导致兼容性和稳定性不足

## 目标格式要求
前端需要同时兼容以下输入：

### 1. 纯文本 SSE
```text
data: Hello
data: world
```

### 2. JSON SSE
```text
data: {"type":"chunk","content":"Hello"}
data: {"type":"done","fullResponse":"Hello world"}
```

### 3. 结束标记
```text
data: [DONE]
```

并统一转换为稳定的前端消息输出结构：

```js
{
  id,
  role: 'assistant',
  content: '最终完整文本',
  sources: [],
  timestamp
}
```

## 方案对比

### 方案 A：只修前端收尾
仅在请求结束时补充消息落盘逻辑。

**优点：** 改动最小。
**缺点：** 对多种 SSE 格式兼容不足，稳定性一般。

### 方案 B：前端重写流解析器（已选）
在前端统一做 SSE 解析与事件归一化，兼容纯文本 SSE 与 JSON SSE，并在无 `done` 时自动收尾。

**优点：**
- 兼容性最好
- 不依赖后端立即改协议
- 能保证实时显示与最终落盘
- 输出格式稳定

**缺点：** 比方案 A 多一些实现量。

### 方案 C：前后端一起改协议
统一改为结构化 JSON SSE。

**优点：** 协议清晰。
**缺点：** 改动范围过大，不适合作为当前最小修复路径。

## 最终设计
采用**方案 B**，只修改前端实现，统一前端内部流事件：

- `chunk`：追加到当前流式文本
- `done`：完成并落盘最终消息
- `error`：展示错误并结束 loading
- `end-without-done`：自然结束但无完成事件时自动收尾

## 修改范围
预计修改：

- `rag-knowledge-base/frontend/src/services/api.js`
  - 重构 XHR/SSE 解析逻辑
  - 将不同后端返回统一归一化为前端事件

- `rag-knowledge-base/frontend/src/pages/ChatTest.jsx`
  - 调整流式状态管理
  - 在无 `done` 的情况下也能在请求结束时正确落盘消息
  - 保证纯文本与 JSON 流都能实时展示

如有必要，可新增一个很小的前端工具函数文件，但优先保持最小改动。

## 验证标准
需要验证以下场景：

1. 纯文本 SSE 可以实时显示并在结束后落盘
2. JSON chunk/done 可以实时显示并正确完成
3. 无 `done` 的流也能在结束后自动完成
4. 空 chunk / 空行不会污染展示
5. 错误响应可正确提示并结束 loading

## 结果预期
修复后，前端聊天页应满足：

- 后端返回纯文本 SSE 时可正常流式展示
- 后端返回 JSON SSE 时可正常流式展示
- 连接结束时最终消息一定落盘
- 输出格式稳定，不依赖后端当前具体实现细节
