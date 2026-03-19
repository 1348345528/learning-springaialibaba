# RAG 知识库系统设计文档

**日期：** 2026-03-19
**版本：** 1.0
**状态：** 已批准

---

## 一、系统概述

### 1.1 项目背景
基于 Spring AI Alibaba 构建 RAG（Retrieval-Augmented Generation）知识库系统，使 AI 能够根据知识库内容回答用户问题，适用于智能客服场景。

### 1.2 系统规模
- **文档规模：** 1000-10000 个文档（中型）
- **用户规模：** 单管理员，无需多用户认证

---

## 二、系统架构

### 2.1 架构风格
**微服务架构**，服务间通过 HTTP/REST 同步通信。

### 2.2 服务划分

```
┌─────────────────────────────────────────────────────────────────┐
│                        React Frontend                            │
│                    (Ant Design + Vite)                          │
└────────────────────────────┬────────────────────────────────────┘
                             │ HTTP/REST
        ┌────────────────────┼────────────────────┐
        ▼                    ▼                    ▼
┌───────────────┐    ┌───────────────┐    ┌───────────────┐
│  文档处理服务  │    │  RAG检索服务   │    │  AI问答服务    │
│   Port:8081   │    │   Port:8082   │    │   Port:8083   │
└───────┬───────┘    └───────┬───────┘    └───────┬───────┘
        │                    │                    │
        ▼                    ▼                    ▼
   ┌────────┐         ┌──────────┐         ┌─────────┐
   │  MySQL  │         │  Milvus  │         │ MiniMax │
   │(元数据) │         │ (向量库)  │         │(AI模型) │
   └────────┘         └──────────┘         └─────────┘
```

### 2.3 服务职责

| 服务 | 端口 | 职责 |
|------|------|------|
| 文档处理服务 | 8081 | 文档上传、格式解析、文本拆分、知识块 CRUD |
| RAG检索服务 | 8082 | 向量嵌入、Milvus 存储、相似度检索 |
| AI问答服务 | 8083 | RAG 检索增强、流式回复生成 |

---

## 三、技术栈

| 组件 | 技术选型 |
|------|---------|
| 后端框架 | Spring Boot 3.4 + Spring Cloud |
| 前端框架 | React 18 + Ant Design 5 + Vite |
| 向量数据库 | Milvus (Docker部署) |
| 元数据库 | MySQL 8 |
| AI模型 | MiniMax (via Spring AI Alibaba) |
| 文档解析 | Apache Tika + EasyPoi + POI |
| 服务通信 | Spring Cloud OpenFeign |

---

## 四、功能模块设计

### 4.1 文档处理服务

#### 4.1.1 支持的文件格式

| 格式 | 解析库 |
|------|--------|
| DOCX | Apache POI / EasyPoi |
| PDF | Apache Tika |
| Excel (.xlsx, .xls) | EasyPoi |
| TXT | Java IO |

#### 4.1.2 拆分策略

| 策略 | 说明 | 参数 |
|------|------|------|
| 固定长度 | 按字符数/Token数拆分 | `chunkSize`, `overlap` |
| 语义拆分 | 按段落、标题、章节边界拆分 | `keepHeaders`, `minParagraphLength` |
| 混合拆分 | 语义拆分 + 超长块二次拆分 | 综合上述参数 |
| 自定义规则 | 支持配置分隔符、标题级别 | `delimiters`, `headerLevels` |

#### 4.1.3 API 端点

| 方法 | 端点 | 说明 |
|------|------|------|
| POST | `/api/doc/upload` | 上传文档 |
| POST | `/api/doc/chunk` | 文档拆分 |
| GET | `/api/doc/chunks` | 获取知识块列表 |
| GET | `/api/doc/chunks/{id}` | 获取知识块详情 |
| PUT | `/api/doc/chunks/{id}` | 更新知识块 |
| DELETE | `/api/doc/chunks/{id}` | 删除知识块 |
| DELETE | `/api/doc/chunks/batch` | 批量删除 |

### 4.2 RAG 检索服务

#### 4.2.1 向量存储

- 使用 MiniMax Embedding 模型生成向量
- 向量存储于 Milvus
- 元数据存储于 MySQL

#### 4.2.2 检索策略

- 相似度检索（余弦相似度）
- 支持 top-k 召回

#### 4.2.3 API 端点

| 方法 | 端点 | 说明 |
|------|------|------|
| POST | `/api/vector/search` | 知识检索 |
| GET | `/api/vector/stats` | 获取统计信息 |

### 4.3 AI 问答服务

#### 4.3.1 问答流程

1. 接收用户问题
2. 调用 RAG 检索服务获取相关知识块
3. 将知识块内容作为上下文
4. 调用 MiniMax 模型生成回复
5. 流式返回回复

#### 4.3.2 API 端点

| 方法 | 端点 | 说明 |
|------|------|------|
| POST | `/api/chat/stream` | 流式问答 |
| GET | `/api/chat/health` | 健康检查 |

---

## 五、数据模型

### 5.1 知识块实体

```java
public class Chunk {
    private String id;              // UUID
    private String content;         // 知识块文本内容
    private String documentName;    // 来源文档名
    private Integer chunkSize;      // 块大小（字符数）
    private Integer chunkIndex;     // 块序号
    private String strategy;       // 使用的拆分策略
    private List<String> tags;     // 标签
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### 5.2 知识块表结构 (MySQL)

```sql
CREATE TABLE chunks (
    id VARCHAR(36) PRIMARY KEY,
    content TEXT NOT NULL,
    document_name VARCHAR(255),
    chunk_size INT,
    chunk_index INT,
    strategy VARCHAR(50),
    tags JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_document_name (document_name),
    INDEX idx_created_at (created_at)
);
```

---

## 六、Web 管理界面

### 6.1 页面模块

| 页面 | 功能 |
|------|------|
| 文档上传 | Drag & Drop 上传，支持 docx/pdf/excel/txt |
| 拆分配置 | 选择拆分策略，配置参数 |
| 知识块管理 | 列表查看、分页搜索、编辑、删除、批量操作 |
| 问答测试 | 输入问题，流式查看 AI 回复 |

### 6.2 组件结构

```
src/
├── pages/
│   ├── DocumentUpload.jsx      # 文档上传页
│   ├── ChunkManagement.jsx     # 知识块管理页
│   └── ChatTest.jsx            # 问答测试页
├── components/
│   ├── ChunkList.jsx           # 知识块列表组件
│   ├── ChunkEditor.jsx         # 知识块编辑组件
│   └── ChatStream.jsx          # 流式聊天组件
└── services/
    └── api.js                  # API 调用封装
```

---

## 七、配置管理

### 7.1 各服务配置

**文档处理服务 (application.yml)**
```yaml
server:
  port: 8081
spring:
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 100MB
```

**RAG 检索服务 (application.yml)**
```yaml
server:
  port: 8082
spring:
  ai:
    minimax:
      api-key: ${MINIMAX_API_KEY}
      base-url: https://api.minimaxi.com
```

**AI 问答服务 (application.yml)**
```yaml
server:
  port: 8083
spring:
  ai:
    minimax:
      api-key: ${MINIMAX_API_KEY}
```

### 7.2 Milvus 连接配置

```yaml
milvus:
  host: localhost
  port: 19530
  collection: rag_chunks
  dimension: 1536  # MiniMax Embedding 维度
```

---

## 八、部署架构

### 8.1 Docker Compose 部署

```yaml
version: '3.8'
services:
  milvus:
    image: milvusdb/milvus:v2.3.3
    ports:
      - "19530:19530"
      - "9091:9091"
    volumes:
      - milvus_data:/var/lib/milvus
    environment:
      ETCD_ENDPOINTS: etcd:2379
      MINIO_ADDRESS: minio:9000

  mysql:
    image: mysql:8
    ports:
      - "3306:3306"
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: rag_knowledgebase

  doc-service:
    build: ./doc-processing-service
    ports:
      - "8081:8081"

  rag-service:
    build: ./rag-retrieval-service
    ports:
      - "8082:8082"

  chat-service:
    build: ./chat-service
    ports:
      - "8083:8083"

  frontend:
    build: ./frontend
    ports:
      - "3000:80"
```

---

## 九、待确认事项

- [ ] Milvus 连接认证配置
- [ ] MiniMax API Key 配置方式
- [ ] 生产环境域名和 HTTPS 配置
- [ ] 日志收集和监控方案

---

## 十、后续计划

1. 创建项目脚手架
2. 实现文档处理服务
3. 实现 RAG 检索服务
4. 实现 AI 问答服务
5. 开发 React 前端
6. 集成测试
7. 部署上线
