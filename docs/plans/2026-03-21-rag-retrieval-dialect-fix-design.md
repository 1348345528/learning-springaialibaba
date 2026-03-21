# RAG Retrieval Service JPA Dialect Fix Design

**日期：** 2026-03-21
**状态：** 已批准

---

## 目标
为 `rag-retrieval-service` 增加最小化的 JPA 方言配置，使 Hibernate 在无法通过 JDBC metadata 自动推断数据库方言时，仍可完成 `entityManagerFactory` 初始化。

## 问题背景
当前 `rag-retrieval-service` 已引入：

- `spring-boot-starter-data-jpa`
- `mysql-connector-j`
- `spring.datasource.*`

但未显式配置 `hibernate.dialect`。当 Hibernate 启动期未能读取到 JDBC metadata 时，会抛出：

- `Unable to determine Dialect without JDBC metadata`

同仓库中的 `doc-processing-service` 已采用显式配置 `org.hibernate.dialect.MySQLDialect`，可作为现有项目内的工作样例。

## 方案对比

### 方案 A（已选定）
仅在 `rag-retrieval-service` 的 `application.yml` 中补充：

```yml
spring:
  jpa:
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
```

**优点：**
- 改动最小
- 与现有服务保持一致
- 可直接规避方言自动推断失败

**缺点：**
- 不能替代数据库连通性排查

### 方案 B
补充 dialect，并同时标准化 MySQL URL 参数。

### 方案 C
不加 dialect，仅继续深挖数据库连接失败根因。

## 最终设计
采用**方案 A**，仅修改以下文件：

- `rag-knowledge-base/rag-retrieval-service/src/main/resources/application.yml`

本次不修改：

- Java 业务代码
- Maven 依赖
- 其他服务配置

## 预期结果
- Hibernate 不再依赖 JDBC metadata 自动识别数据库方言
- `entityManagerFactory` 初始化可继续向前推进
- 如果数据库本身仍不可连接，后续报错会收敛到更具体的连接问题
