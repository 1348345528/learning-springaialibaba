# RAG 知识库系统项目描述

## 一、项目概述

RAG Knowledge Base 是一个基于检索增强生成（Retrieval-Augmented Generation）技术的知识库系统，用于文档处理、向量化存储和智能问答。

### 1.1 核心功能

- **文档上传与解析**：支持 PDF、Word、Excel、TXT、Markdown 等格式
- **智能分块**：提供固定长度、递归、语义、分层、混合、自定义规则等多种分块策略
- **向量存储**：基于 Milvus 向量数据库实现高效语义检索
- **智能问答**：基于检索上下文调用大模型生成准确回答
- **会话管理**：支持多会话管理、会话历史持久化
- **多级缓存**：Redis + MySQL 两级存储架构，兼顾性能与持久化

### 1.2 系统架构

系统采用微服务架构，包含三个后端服务和一个前端应用：

| 服务 | 端口 | 职责 |
|------|------|------|
| chat-service | 8083 | 处理聊天请求，会话管理，多级缓存，调用 AI 模型生成回复 |
| doc-processing-service | 8082 | 文档上传、解析、分块、存储到 MySQL，分块预览 |
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
| 缓存 | Redis | 7.x |
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
| 测试框架 | Playwright | E2E 测试 |

---

## 三、目录结构

```
rag-knowledge-base/
├── chat-service/                      # 聊天服务（端口 8083）
│   └── src/main/java/com/example/chat/
│       ├── ChatApplication.java        # 启动类
│       ├── config/
│       │   ├── WebClientConfig.java   # WebClient 配置
│       │   └── RedisConfig.java       # Redis 配置
│       ├── controller/
│       │   ├── ChatController.java    # 聊天 API 控制器
│       │   └── ConversationController.java  # 会话管理控制器
│       ├── dto/
│       │   ├── ChatRequest.java       # 聊天请求 DTO
│       │   ├── ConversationDto.java   # 会话 DTO
│       │   └── CreateConversationRequest.java  # 创建会话请求
│       ├── entity/
│       │   ├── ConversationEntity.java # 会话实体
│       │   └── ChatMessageEntity.java  # 聊天消息实体
│       ├── model/
│       │   └── ChatMemoryMessage.java  # 聊天内存消息模型
│       ├── repository/
│       │   ├── RedisChatMemoryRepository.java  # Redis 仓库
│       │   ├── MysqlChatMemoryRepository.java  # MySQL 仓库
│       │   └── jpa/
│       │       ├── ConversationJpaRepository.java
│       │       └── ChatMessageJpaRepository.java
│       └── service/
│           ├── RagChatService.java    # RAG 聊天服务
│           ├── MultiLevelChatMemory.java  # 多级缓存服务
│           └── ChatMemorySyncService.java # 异步同步服务
│
├── doc-processing-service/            # 文档处理服务（端口 8082）
│   └── src/main/java/com/example/doc/
│       ├── DocApplication.java         # 启动类
│       ├── chunker/                   # 分块策略
│       │   ├── ChunkConfig.java       # 分块配置
│       │   ├── ChunkStrategy.java     # 分块策略接口
│       │   ├── TextChunk.java         # 分块结果
│       │   ├── config/
│       │   │   ├── RecursiveChunkConfig.java   # 递归分块配置
│       │   │   ├── SemanticChunkConfig.java    # 语义分块配置
│       │   │   └── HierarchicalChunkConfig.java # 分层分块配置
│       │   └── impl/
│       │       ├── FixedLengthChunker.java   # 固定长度分块
│       │       ├── RecursiveChunker.java     # 递归分块
│       │       ├── SemanticChunker.java      # 语义分块
│       │       ├── TrueSemanticChunker.java  # 真语义分块
│       │       ├── HierarchicalChunker.java  # 分层分块
│       │       ├── HybridChunker.java        # 混合分块
│       │       └── CustomRuleChunker.java    # 自定义规则分块
│       ├── controller/
│       │   ├── DocumentController.java # 文档 API 控制器
│       │   └── ChunkPreviewController.java  # 分块预览控制器
│       ├── dto/
│       │   ├── ChunkDto.java          # 分块 DTO
│       │   ├── ChunkRequest.java      # 分块请求
│       │   ├── UploadRequest.java     # 上传请求
│       │   ├── ChunkPreviewRequest.java   # 分块预览请求
│       │   └── ChunkPreviewResponse.java  # 分块预览响应
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
│           ├── DocumentService.java   # 文档服务
│           └── ChunkPreviewService.java  # 分块预览服务
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
│       ├── App.jsx                   # 根组件
│       ├── pages/
│       │   ├── DocumentUpload.jsx    # 文档上传页面
│       │   ├── ChunkManagement.jsx   # 知识块管理页面
│       │   └── ChatQA.jsx            # 智能问答页面
│       ├── components/
│       │   ├── chat/                 # 聊天组件目录
│       │   │   ├── ConversationList.jsx    # 对话列表组件
│       │   │   ├── ConversationSidebar.jsx # 会话侧边栏组件
│       │   │   ├── InputArea.jsx           # 输入区域组件
│       │   │   ├── MessageBubble.jsx       # 消息气泡组件
│       │   │   └── EmptyState.jsx          # 空状态组件
│       │   ├── ChunkConfig/          # 分块配置组件
│       │   │   ├── HierarchicalChunkConfig.jsx  # 分层分块配置
│       │   │   ├── RecursiveChunkConfig.jsx     # 递归分块配置
│       │   │   └── SemanticChunkConfig.jsx      # 语义分块配置
│       │   ├── ChunkPreview.jsx      # 分块预览组件
│       │   ├── ChunkStatistics.jsx   # 分块统计组件
│       │   └── ChunkStrategySelector.jsx  # 分块策略选择器
│       ├── services/
│       │   └── api.js               # API 调用封装
│       └── tests/
│           ├── e2e/                  # E2E 测试
│           │   ├── chat-memory.spec.js  # 聊天内存测试
│           │   └── document-upload.spec.js  # 文档上传测试
│           └── pages/                # Page Object 模型
│               └── ChatQAPage.js
│
└── docs/                              # 项目文档
    └── PROJECT_DESCRIPTION.md        # 项目描述文档（本文件）
```

---

## 四、服务详解

### 4.1 chat-service（聊天服务）

**端口**：8083

**功能**：
- 处理用户聊天请求，调用 AI 模型生成回复（支持 SSE 流式输出）
- 会话管理（创建、查询、更新、删除）
- 多级缓存（Redis L1 + MySQL L2）

**核心端点**：

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 流式聊天 | POST | `/api/chat/stream` | SSE 流式返回 AI 回复 |
| 创建会话 | POST | `/api/conversations` | 创建新会话 |
| 获取会话列表 | GET | `/api/conversations` | 获取所有会话 |
| 获取单个会话 | GET | `/api/conversations/{id}` | 根据 ID 获取会话 |
| 更新会话标题 | PUT | `/api/conversations/{id}/title` | 更新会话标题 |
| 删除会话 | DELETE | `/api/conversations/{id}` | 删除会话及历史 |

**多级缓存架构**：

```
┌─────────────────────────────────────────────────────────────┐
│                     MultiLevelChatMemory                     │
├─────────────────────────────────────────────────────────────┤
│  L1 Cache (Redis)          │  L2 Persistence (MySQL)        │
│  - 快速访问                 │  - 持久存储                     │
│  - TTL 24小时              │  - 会话 + 消息实体               │
│  - 异步同步到 L2            │  - 支持历史恢复                  │
└─────────────────────────────────────────────────────────────┘
```

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
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 5000ms
  datasource:
    url: jdbc:mysql://localhost:3306/rag_knowledgebase
    username: root
    password: root
rag:
  service:
    base-url: http://localhost:8081
chat:
  memory:
    redis:
      key-prefix: "chat:memory:"
      ttl-hours: 24
```

---

### 4.2 doc-processing-service（文档处理服务）

**端口**：8082

**功能**：文档上传、解析、分块、存储到 MySQL，并异步索引到 Milvus

**支持的文件格式**：`.txt`, `.md`, `.pdf`, `.docx`, `.xlsx`

**分块策略**：

| 策略 | 说明 | 适用场景 |
|------|------|---------|
| FixedLength | 固定字符数分块 | 结构化文档 |
| Recursive | 递归分块，按分隔符层级分割 | 通用文档 |
| Semantic | 语义分块，按语义边界分割 | 长文本 |
| Hierarchical | 分层分块，父子结构 | 需要上下文的场景 |
| Hybrid | 混合分块，结合多种策略 | 复杂文档 |
| CustomRule | 自定义正则规则分块 | 特殊格式 |

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
| 分块预览 | POST | `/api/chunk/preview` | 预览分块效果 |
| 获取策略配置 | GET | `/api/chunk/preview/strategies` | 获取默认配置 |

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

**技术栈**：React + Vite + Ant Design + Axios + Playwright

**主要页面**：

#### DocumentUpload（文档上传页面）

- 拖拽上传支持
- 分块策略选择：固定长度、递归、语义、分层、混合、自定义规则
- 分块大小和重叠配置
- 嵌入模型选择
- 上传进度显示
- 分块预览功能

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
- 会话侧边栏，支持多会话管理
- 会话创建、切换、删除
- 停止生成功能
- 复制对话内容
- 实时打字光标效果

**Vite 代理配置**（vite.config.js）：

```javascript
server: {
  port: 3000,
  proxy: {
    '/api/chat': { target: 'http://localhost:8083', changeOrigin: true },
    '/api/conversations': { target: 'http://localhost:8083', changeOrigin: true },
    '/api/chunk': { target: 'http://localhost:8082', changeOrigin: true },
    '/api/doc': { target: 'http://localhost:8082', changeOrigin: true },
    '/api': { target: 'http://localhost:8081', changeOrigin: true }
  }
}
```

---

## 五、数据模型

### 5.1 MySQL 表结构

#### chunks 表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | VARCHAR(36) | UUID 主键 |
| content | TEXT | 分块内容 |
| document_name | VARCHAR(255) | 文档名称 |
| chunk_size | INT | 分块大小 |
| chunk_index | INT | 分块序号 |
| strategy | VARCHAR(50) | 分块策略 |
| parent_id | VARCHAR(36) | 父块 ID（分层分块） |
| tags | JSON | 标签 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

#### conversations 表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 自增主键 |
| conversation_id | VARCHAR(36) | 会话 UUID |
| title | VARCHAR(255) | 会话标题 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

#### chat_messages 表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 自增主键 |
| conversation_id | VARCHAR(36) | 关联会话 ID |
| role | VARCHAR(20) | 消息角色（user/assistant） |
| content | TEXT | 消息内容 |
| created_at | DATETIME | 创建时间 |

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
       │                    │                            ▲
       │                    │ 异步索引                     │ 插入向量
       │                    ▼                            │
       │             ┌─────────────────┐                 │
       │             │ rag-retrieval   │────────────────┘
       │             │    :8081        │
       │             └─────────────────┘
       │                    ▲
       │                    │ 搜索
       │                    │
       │             ┌─────────────┐     ┌─────────────────┐
       └────────────▶│ chat-service │────▶│  MiniMax API    │
                     │    :8083     │     │                 │
                     └─────────────┘     └─────────────────┘
                            │
                            ▼
                     ┌─────────────┐
                     │ Redis/MySQL │
                     │  (ChatMemory)│
                     └─────────────┘
```

---

## 七、数据流

### 7.1 文档上传流程

1. 用户上传文档 → doc-processing-service
2. 解析文档内容（Tika/POI）
3. 按策略分块（FixedLength/Recursive/Semantic/Hierarchical/Hybrid/CustomRule）
4. 存入 MySQL（支持父子块关系）
5. 异步调用 rag-retrieval-service 索引到 Milvus

### 7.2 问答流程

1. 用户提问 → chat-service
2. 从多级缓存获取会话历史（Redis → MySQL）
3. 同步调用 rag-retrieval-service 进行语义检索
4. rag-retrieval-service 在 Milvus 中搜索相似向量
5. 从 MySQL 获取完整内容
6. 组装上下文调用大模型生成回答
7. SSE 流式返回给用户
8. 异步保存消息到多级缓存

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
| Redis TTL | 24h | 聊天缓存过期时间 |

---

## 九、快速开始

### 9.1 环境要求

- JDK 17+
- Node.js 18+
- MySQL 8.x
- Redis 7.x
- Milvus 2.4.x

### 9.2 启动顺序

1. 启动 MySQL，创建数据库 `rag_knowledgebase`
2. 启动 Redis
3. 启动 Milvus 向量数据库
4. 启动 rag-retrieval-service（端口 8081）
5. 启动 doc-processing-service（端口 8082）
6. 启动 chat-service（端口 8083）
7. 启动 frontend（端口 3000）

### 9.3 访问地址

- 前端应用：http://localhost:3000
- chat-service API：http://localhost:8083
- doc-processing-service API：http://localhost:8082
- rag-retrieval-service API：http://localhost:8081

---

## 十、测试

### 10.1 E2E 测试

项目使用 Playwright 进行端到端测试：

```bash
cd frontend
npm run test:e2e
```

测试覆盖：
- 聊天内存功能
- 文档上传流程
- 会话管理
- 流式输出

---

## 十一、相关文档

| 文档 | 路径 | 说明 |
|------|------|------|
| 架构设计 | docs/architecture.md | 系统架构设计文档 |
| API 设计 | docs/api/ | API 接口设计文档 |
| 需求规格 | docs/requirements/ | 需求规格文档 |
| UI 设计 | docs/ui/ | 页面/组件设计文档 |
