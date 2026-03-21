# RAG 知识库系统项目描述

## 一、项目概述

RAG Knowledge Base 是一个基于检索增强生成（Retrieval-Augmented Generation）技术的知识库系统，用于文档处理、向量化存储和智能问答。

### 1.1 核心功能

- **文档上传与解析**：支持 PDF、Word、Excel、TXT、Markdown 等格式
- **智能分块**：提供固定长度、语义、混合、自定义规则等多种分块策略
- **向量存储**：基于 Milvus 向量数据库实现高效语义检索
- **智能问答**：基于检索上下文调用大模型生成准确回答

### 1.2 系统架构

系统采用微服务架构，包含三个后端服务和一个前端应用：

| 服务 | 端口 | 职责 |
|------|------|------|
| chat-service | 8083 | 处理聊天请求，调用 AI 模型生成回复 |
| doc-processing-service | 8082 | 文档上传、解析、分块、存储到 MySQL |
| rag-retrieval-service | 8081 | 向量存储和语义检索，集成 Milvus |
| frontend | 3000 | React 前端应用 |

---

## 二、技术栈

### 2.1 后端技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 框架 | Spring Boot | 3.4.0 |
| Java | OpenJDK | 17 |
| 关系数据库 | MySQL | 8.x |
| 向量数据库 | Milvus | 2.4.x |
| AI 框架 | Spring AI | 1.1.2 |
| 大模型 | MiniMax | MiniMax-M2.5-highspeed |
| 嵌入模型 | Qwen3-Embedding-8B | 4096 维 |
| 文档解析 | Apache Tika + POI | 2.9.1 / 5.2.5 |

### 2.2 前端技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 框架 | React | 18.2.0 |
| 构建工具 | Vite | 5.0.8 |
| UI 组件库 | Ant Design | 5.12.0 |
| HTTP 客户端 | Axios | 1.6.2 |

---

## 三、目录结构

```
rag-knowledge-base/
├── chat-service/                      # 聊天服务（端口 8083）
│   └── src/main/java/com/example/chat/
│       ├── ChatApplication.java        # 启动类
│       ├── config/
│       │   └── WebClientConfig.java   # WebClient 配置
│       ├── controller/
│       │   └── ChatController.java    # 聊天 API 控制器
│       ├── dto/
│       │   └── ChatRequest.java       # 聊天请求 DTO
│       └── service/
│           └── RagChatService.java    # 聊天服务实现
│
├── doc-processing-service/            # 文档处理服务（端口 8082）
│   └── src/main/java/com/example/doc/
│       ├── DocApplication.java         # 启动类
│       ├── chunker/                   # 分块策略
│       │   ├── ChunkConfig.java       # 分块配置
│       │   ├── ChunkStrategy.java     # 分块策略接口
│       │   ├── TextChunk.java         # 分块结果
│       │   └── impl/
│       │       ├── FixedLengthChunker.java   # 固定长度分块
│       │       ├── SemanticChunker.java      # 语义分块
│       │       ├── HybridChunker.java        # 混合分块
│       │       └── CustomRuleChunker.java    # 自定义规则分块
│       ├── controller/
│       │   └── DocumentController.java # 文档 API 控制器
│       ├── dto/
│       │   ├── ChunkDto.java          # 分块 DTO
│       │   ├── ChunkRequest.java      # 分块请求
│       │   └── UploadRequest.java     # 上传请求
│       ├── entity/
│       │   └── Chunk.java            # 分块实体
│       ├── parser/                    # 文档解析器
│       │   ├── DocumentParser.java    # 解析器接口
│       │   ├── DocumentParserFactory.java  # 解析器工厂
│       │   └── impl/
│       │       ├── PdfParser.java     # PDF 解析（Tika）
│       │       ├── DocxParser.java    # Word 解析（POI）
│       │       ├── TxtParser.java     # 文本解析
│       │       └── ExcelParser.java   # Excel 解析
│       ├── repository/
│       │   └── ChunkRepository.java   # JPA 仓库
│       └── service/
│           └── DocumentService.java   # 文档服务
│
├── rag-retrieval-service/             # RAG 检索服务（端口 8081）
│   └── src/main/java/com/example/rag/
│       ├── RagApplication.java         # 启动类
│       ├── config/
│       │   └── MilvusConfig.java      # Milvus 客户端配置
│       ├── controller/
│       │   └── VectorController.java   # 向量 API 控制器
│       ├── dto/
│       │   ├── IndexRequest.java      # 索引请求
│       │   ├── SearchRequest.java     # 搜索请求
│       │   └── SearchResult.java      # 搜索结果
│       ├── entity/
│       │   └── Chunk.java            # 简化的分块实体
│       ├── repository/
│       │   └── ChunkRepository.java   # JPA 仓库
│       └── service/
│           ├── EmbeddingService.java  # 嵌入服务
│           ├── MilvusService.java     # Milvus 核心操作
│           └── VectorService.java     # 向量服务
│
├── frontend/                          # 前端应用（端口 3000）
│   └── src/
│       ├── main.jsx                  # 入口文件
│       ├── App.jsx                  # 根组件
│       ├── pages/
│       │   ├── DocumentUpload.jsx    # 文档上传页面
│       │   ├── ChunkManagement.jsx   # 知识块管理页面
│       │   └── ChatQA.jsx            # 智能问答页面
│       ├── components/
│       │   └── chat/                 # 聊天组件目录
│       │       ├── ConversationList.jsx  # 对话列表组件
│       │       ├── InputArea.jsx         # 输入区域组件
│       │       └── MessageBubble.jsx     # 消息气泡组件
│       └── services/
│           └── api.js               # API 调用封装
│
└── docs/                              # 项目文档
    └── PROJECT_DESCRIPTION.md        # 项目描述文档（本文件）
```

---

## 四、服务详解

### 4.1 chat-service（聊天服务）

**端口**：8083

**功能**：处理用户聊天请求，调用 AI 模型生成回复（支持 SSE 流式输出）

**核心端点**：

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 流式聊天 | POST | `/api/chat/stream` | SSE 流式返回 AI 回复 |

**配置**（application.yml）：

```yaml
server:
  port: 8083
spring:
  ai:
    minimax:
      api-key: <API_KEY>
      base-url: https://api.minimaxi.com
      chat:
        options:
          model: MiniMax-M2.5-highspeed
rag:
  service:
    base-url: http://localhost:8082  # RAG 服务地址
```

---

### 4.2 doc-processing-service（文档处理服务）

**端口**：8082

**功能**：文档上传、解析、分块、存储到 MySQL，并异步索引到 Milvus

**支持的文件格式**：`.txt`, `.md`, `.pdf`, `.docx`

**核心端点**：

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 上传文档 | POST | `/api/doc/upload` | 上传并分块 |
| 获取分块列表 | GET | `/api/doc/chunks` | 分页查询 |
| 获取文档列表 | GET | `/api/doc/documents` | 获取所有文档名 |
| 获取单个分块 | GET | `/api/doc/chunks/{id}` | 根据 ID 查询 |
| 更新分块 | PUT | `/api/doc/chunks/{id}` | 更新内容 |
| 删除分块 | DELETE | `/api/doc/chunks/{id}` | 删除单个 |
| 批量删除 | DELETE | `/api/doc/chunks/batch` | 批量删除 |

**配置**（application.yml）：

```yaml
server:
  port: 8082
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/rag_knowledgebase
    username: root
    password: root
  ai:
    openai:
      api-key: <API_KEY>
      base-url: https://api.siliconflow.cn
      embedding:
        options:
          model: Qwen/Qwen3-Embedding-8B
milvus:
  host: localhost
  port: 19530
  collection: rag_chunks
  dimension: 4096
```

---

### 4.3 rag-retrieval-service（RAG 检索服务）

**端口**：8081

**功能**：向量化存储和语义检索，集成 Milvus 向量数据库

**核心端点**：

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 索引分块 | POST | `/api/vector/index` | 将分块存入 Milvus |
| 搜索 | POST | `/api/vector/search` | 语义检索 |

**配置**（application.yml）：

```yaml
server:
  port: 8081
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/rag_knowledgebase
    username: root
    password: root
  jpa:
    hibernate:
      ddl-auto: update
  servlet:
    multipart:
      max-file-size: 50MB
app:
  rag-service-url: http://localhost:8082
```

---

### 4.4 frontend（前端应用）

**端口**：3000

**技术栈**：React + Vite + Ant Design + Axios

**主要页面**：

#### DocumentUpload（文档上传页面）

- 拖拽上传支持
- 分块策略选择：固定长度、语义、混合、自定义规则
- 分块大小和重叠配置
- 嵌入模型选择
- 上传进度显示

#### ChunkManagement（知识块管理页面）

- 知识块列表（分页、排序）
- 关键词搜索
- 按文档筛选
- 查看/编辑/删除知识块
- 批量删除
- 重新向量化

#### ChatQA（智能问答页面）

**功能特性**：

- 基于知识库的智能问答助手
- 支持流式输出（SSE），实时显示 AI 回复
- 支持 Markdown 渲染（标题、列表、代码块、表格等）
- 支持特殊符号和 emoji 显示
- 支持复制对话内容
- 提供实时打字光标效果

**技术实现**：

- 前端通过 SSE（Server-Sent Events）接收流式响应
- 流式传输过程中显示纯文本，光标闪烁提示数据正在传输
- 流式结束后一次性渲染完整的 Markdown 内容
- 使用剪贴板 API 实现一键复制回答内容
- 输入框支持多行文本，提供非法字符、空输入、超长文本校验
- 支持停止生成功能，可在流式输出过程中中断请求

**Vite 代理配置**（vite.config.js）：

```javascript
server: {
  port: 3000,
  proxy: {
    '/api/chat': { target: 'http://localhost:8083' },
    '/api': { target: 'http://localhost:8081' }
  }
}
```

---

## 五、数据模型

### 5.1 MySQL 表结构（chunks 表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | VARCHAR(36) | UUID 主键 |
| content | TEXT | 分块内容 |
| document_name | VARCHAR(255) | 文档名称 |
| chunk_size | INT | 分块大小 |
| chunk_index | INT | 分块序号 |
| strategy | VARCHAR(50) | 分块策略 |
| tags | JSON | 标签 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

### 5.2 Milvus 集合结构（rag_chunks）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | VarChar(36) | 主键 |
| embedding | FloatVector(4096) | 4096 维向量 |
| content | VarChar(65535) | 内容 |
| document_name | VarChar(255) | 文档名 |

---

## 六、服务调用关系

```
┌─────────────┐     ┌──────────────────────┐     ┌─────────────────┐
│   Frontend  │────▶│  doc-processing-svc  │────▶│   Milvus        │
│   :3000     │     │      :8082           │     │   :19530        │
└─────────────┘     └──────────────────────┘     └─────────────────┘
                            │                            ▲
                            │ 异步索引                     │ 插入向量
                            ▼                            │
                     ┌─────────────────┐                 │
                     │ rag-retrieval   │────────────────┘
                     │    :8081        │
                     └─────────────────┘
                            ▲
                            │ 搜索
                            │
                     ┌─────────────┐     ┌─────────────────┐
                     │ chat-service │────▶│  MiniMax API    │
                     │    :8083     │     │                 │
                     └─────────────┘     └─────────────────┘
```

---

## 七、数据流

### 7.1 文档上传流程

1. 用户上传文档 → doc-processing-service
2. 解析文档内容（Tika/POI）
3. 按策略分块（FixedLength/Semantic/Hybrid/CustomRule）
4. 存入 MySQL
5. 异步调用 rag-retrieval-service 索引到 Milvus

### 7.2 问答流程

1. 用户提问 → chat-service
2. 同步调用 rag-retrieval-service 进行语义检索
3. rag-retrieval-service 在 Milvus 中搜索相似向量
4. 从 MySQL 获取完整内容
5. 组装上下文调用大模型生成回答
6. SSE 流式返回给用户

---

## 八、关键配置参数

| 参数 | 默认值 | 说明 |
|------|--------|------|
| chunkSize | 500 | 分块大小（字符数） |
| overlap | 50 | 分块重叠大小 |
| topK | 5 | 检索返回数量 |
| embedding 维度 | 4096 | Qwen3-Embedding 模型 |
| Milvus collection | rag_chunks | 向量集合名 |
| max-file-size | 50MB | 最大上传文件大小 |

---

## 九、快速开始

### 9.1 环境要求

- JDK 17+
- Node.js 18+
- MySQL 8.x
- Milvus 2.4.x

### 9.2 启动顺序

1. 启动 MySQL，创建数据库 `rag_knowledgebase`
2. 启动 Milvus 向量数据库
3. 启动 rag-retrieval-service（端口 8081）
4. 启动 doc-processing-service（端口 8082）
5. 启动 chat-service（端口 8083）
6. 启动 frontend（端口 3000）

### 9.3 访问地址

- 前端应用：http://localhost:3000
- chat-service API：http://localhost:8083
- doc-processing-service API：http://localhost:8082
- rag-retrieval-service API：http://localhost:8081

---

## 十、相关文档

| 文档 | 路径 | 说明 |
|------|------|------|
| 架构设计 | docs/architecture.md | 系统架构设计文档 |
| API 设计 | docs/api/ | API 接口设计文档 |
| 需求规格 | docs/requirements/ | 需求规格文档 |
| UI 设计 | docs/ui/ | 页面/组件设计文档 |
