# CLAUDE.md — RAG 知识库系统目录导航

```
rag-knowledge-base/
│
├── auth-service/                   # 认证授权服务
│   ├── config/                     #   安全、CORS、数据初始化配置
│   ├── controller/                 #   登录、用户、角色、菜单 API
│   ├── dto/                        #   请求/响应 DTO
│   ├── entity/                     #   JPA 实体 (SysUser, SysRole, SysMenu)
│   ├── filter/                     #   JWT 请求过滤器
│   ├── repository/                 #   数据访问层
│   ├── security/                   #   JWT 工具类
│   └── service/                    #   业务逻辑实现
│
├── chat-service/                   # 聊天服务
│   ├── config/                     #   Redis/Security/WebClient 配置
│   ├── controller/                 #   流式聊天、会话管理 API
│   ├── dto/                        #   请求/响应 DTO
│   ├── entity/                     #   JPA 实体 (Conversation, ChatMessage)
│   ├── model/                      #   领域模型 (ChatMemoryMessage)
│   ├── repository/                 #   数据访问 (Redis + MySQL JPA)
│   └── service/                    #   对话、多级缓存、历史摘要
│
├── doc-processing-service/         # 文档处理服务
│   ├── chunker/                    #   分块策略 (固定长度/递归/语义/分层/混合/自定义)
│   ├── config/                     #   安全配置
│   ├── controller/                 #   上传、分块预览 API
│   ├── dto/                        #   请求/响应 DTO
│   ├── entity/                     #   JPA 实体 (Chunk)
│   ├── parser/                     #   文档解析器 (PDF/DOCX/TXT/Excel)
│   ├── repository/                 #   数据访问层
│   └── service/                    #   文档处理业务逻辑
│
├── rag-retrieval-service/          # 向量检索服务
│   ├── config/                     #   Milvus 配置
│   ├── controller/                 #   向量索引/搜索 API
│   ├── dto/                        #   请求/响应 DTO
│   ├── entity/                     #   向量相关实体
│   ├── repository/                 #   Milvus 数据访问
│   └── service/                    #   向量存储与语义检索
│
├── frontend/                       # React + Vite 前端
│   ├── src/
│   │   ├── components/             #   通用组件 (布局/分块配置/聊天组件)
│   │   ├── pages/                  #   页面 (登录/文档上传/分块管理/聊天/用户角色菜单管理)
│   │   ├── services/               #   API 封装 (axios + SSE 流式请求)
│   │   ├── store/                  #   Zustand 状态管理
│   │   └── styles/                 #   全局样式
│   └── tests/                      #   E2E 测试 (Playwright)
│
└── docs/                           # 项目文档
```

## 各服务通用目录说明

| 目录 | 说明 |
|------|------|
| `config/` | 安全、CORS、第三方客户端等配置类 |
| `controller/` | REST API 入口 |
| `dto/` | 请求/响应数据传输对象 |
| `entity/` | JPA 实体类 |
| `repository/` | 数据访问层 (JPA Repository / Milvus SDK) |
| `service/` | 核心业务逻辑 |
