# Maven 父 POM 多模块改造设计

## 目标

在 `rag-knowledge-base/pom.xml` 创建父 POM，将 5 个独立 Maven 模块改造为标准多模块项目，统一管理依赖版本和插件配置。

## 当前状态

- 5 个独立模块，各自以 `spring-boot-starter-parent:3.4.0` 为 parent
- GroupId: `com.example`，Version: `0.0.1-SNAPSHOT`
- Java 17，Spring Boot 3.4.0
- 大量依赖重复声明，版本管理不一致

## 父 POM 结构

**位置：** `rag-knowledge-base/pom.xml`

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.4.0</version>
</parent>

<groupId>com.example</groupId>
<artifactId>rag-knowledge-base</artifactId>
<version>0.0.1-SNAPSHOT</version>
<packaging>pom</packaging>

<modules>
    <module>auth-service</module>
    <module>chat-service</module>
    <module>doc-processing-service</module>
    <module>mcp-server-example</module>
    <module>rag-retrieval-service</module>
</modules>
```

## 统一版本属性

```xml
<properties>
    <java.version>17</java.version>
    <spring-ai.version>1.1.2</spring-ai.version>
    <jjwt.version>0.12.5</jjwt.version>
    <fastjson.version>2.0.57</fastjson.version>
    <mysql-connector.version>8.4.0</mysql-connector.version>
    <springdoc-openapi.version>2.3.0</springdoc-openapi.version>
    <tika.version>2.9.1</tika.version>
    <poi-ooxml.version>5.2.5</poi-ooxml.version>
    <dashscope-sdk.version>2.22.0</dashscope-sdk.version>
    <milvus-sdk.version>2.4.6</milvus-sdk.version>
</properties>
```

## dependencyManagement 统一管控

以下依赖版本在父 POM 中统一声明，子模块引用时省略 `<version>`：

| 依赖 | 适用范围 | scope |
|---|---|---|
| spring-boot-starter-web | 4 个服务模块 | compile |
| spring-boot-starter-security | 4 个服务模块 | compile |
| spring-boot-starter-data-jpa | 4 个服务模块 | compile |
| spring-boot-starter-validation | 4 个服务模块 | compile |
| spring-boot-starter-webflux | chat, doc-processing | compile |
| spring-boot-starter-test | 4 个服务模块 | test |
| mysql-connector-j | 4 个服务模块 | runtime |
| lombok | 4 个服务模块 | provided |
| jjwt-api / jjwt-impl / jjwt-jackson | 4 个服务模块 | compile / runtime |
| fastjson | chat, doc-processing, rag-retrieval | compile |
| spring-ai-starter-model-deepseek | chat-service | compile |
| spring-ai-starter-mcp-client | chat-service | compile |
| spring-ai-starter-mcp-server-webflux | mcp-server-example | compile |
| springdoc-openapi-starter-webmvc-ui | doc-processing | compile |
| tika-core | doc-processing | compile |
| poi-ooxml | doc-processing | compile |
| dashscope-sdk-java | rag-retrieval | compile |
| milvus-sdk-java | rag-retrieval | compile |

## pluginManagement 统一配置

```xml
<pluginManagement>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <configuration>
                <excludes>
                    <exclude>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                    </exclude>
                </excludes>
            </configuration>
        </plugin>
    </plugins>
</pluginManagement>
```

## 子模块 POM 改造

每个子模块：

1. `<parent>` 指向 `com.example:rag-knowledge-base:0.0.1-SNAPSHOT`（使用 `<relativePath>../pom.xml</relativePath>`）
2. 删除 `spring-boot-starter-parent` 的 parent 声明
3. 删除 `<properties>` 中已在父 POM 定义的版本号
4. 共享依赖去掉 `<version>` 和 `<scope>`（由 dependencyManagement 管控）
5. 模块独有依赖保持不变

## 顺手修复

- `rag-retrieval-service` 的 `spring-boot-starter-test` 缺少 `test` scope，在父 POM dependencyManagement 中统一设为 test
- `auth-service` 和 `chat-service` 的 `mysql-connector-j` 版本从继承 boot parent 改为显式指定 8.4.0，与其他模块一致
