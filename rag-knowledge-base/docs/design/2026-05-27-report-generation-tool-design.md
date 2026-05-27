# 报表生成 Tool 设计文档

> 日期：2026-05-27
> 状态：设计中

---

## 1. 需求概述

在 chat-service 中新增一个报表生成 Tool，作为 Spring AI 可调用的 Function Tool，由 AI 根据用户对话自动提取参数并调用。生成 HTML 报表并持久化存储，前端通过新标签页展示报表内容。

- **入参**：时间周期（起止日期）、险种类型
- **出参**：报表 ID、报表名称
- **存储**：新建 `reports` 表，存储 HTML 内容
- **展示**：前端聊天中展示可点击的报表卡片，点击在新标签页打开 HTML

---

## 2. 数据流

```
用户输入 "帮我生成2026年1月到3月的车险报表"
    │
    ▼
AI 分析提取参数
    insuranceType = "车险"
    startDate     = "2026-01-01"
    endDate       = "2026-03-31"
    │
    ▼
AI 调用 generate_report Tool
    │
    ▼
ReportGenerationTool.apply()
    → ReportService.generateReport()
        → 构建模拟数据的 HTML 字符串
        → 保存 ReportEntity 到 reports 表
    → 返回 Response { reportId, reportName, url }
    │
    ▼
AI 收到 Tool 返回结果，生成自然语言回复（包含报表链接）
    │
    ▼
RagChatService.chatStream()
    → 流式输出 AI 文本回复（SSE event:message）
    → 检测到本轮有报表生成 → 追加 SSE event:report（含结构化 JSON）
    → 发送 [DONE]
    │
    ▼
前端 handleSend() 处理 SSE 事件
    → event:message → 更新消息文本（markdown 渲染含报表链接）
    → event:report  → 将报表元数据挂到消息对象上
    │
    ▼
MessageBubble 渲染
    → 检测 msg.report 存在 → 渲染报表卡片（可点击）
    → 用户点击 → window.open(url, '_blank')
    → 浏览器请求 GET /api/reports/{id}/html → 展示 HTML
```

---

## 3. 后端设计

### 3.1 新建文件

| 文件 | 路径 | 职责 |
|------|------|------|
| `ReportEntity.java` | `entity/` | JPA 实体，映射 `reports` 表 |
| `ReportJpaRepository.java` | `repository/jpa/` | 数据访问层 |
| `ReportService.java` | `service/` | 核心业务：生成 HTML、保存、查询 |
| `ReportGenerationTool.java` | `service/` | Spring AI Tool 实现 |
| `ReportController.java` | `controller/` | REST 接口：获取 HTML 内容 |

### 3.2 修改文件

| 文件 | 改动 |
|------|------|
| `config/ReactAgentConfig.java` | 新增 `reportGenerationCallback` Bean |
| `service/RagChatService.java` | 流式响应结束后追加 `event:report` SSE 事件 |

### 3.3 数据库表设计

```sql
CREATE TABLE reports (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(255)  NOT NULL COMMENT '报表名称',
    insurance_type  VARCHAR(50)   NOT NULL COMMENT '险种',
    start_date      DATE          NOT NULL COMMENT '起始日期',
    end_date        DATE          NOT NULL COMMENT '截止日期',
    html_content    LONGTEXT      NOT NULL COMMENT 'HTML报表内容',
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
);
```

JPA `ddl-auto: update` 会自动建表。

### 3.4 ReportEntity

```java
@Entity
@Table(name = "reports")
public class ReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "insurance_type", nullable = false, length = 50)
    private String insuranceType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Lob
    @Column(name = "html_content", columnDefinition = "LONGTEXT", nullable = false)
    private String htmlContent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
```

### 3.5 ReportService

核心方法：

```
generateReport(insuranceType, startDate, endDate) → ReportEntity
  - 生成报表名称："车险报表-2026-01-01至2026-03-31"
  - 构建 HTML 字符串（内联 CSS）
      - 页头：标题 + 日期范围 + 险种
      - 指标卡片区（4 个）：保费总额、保单数量、理赔金额、理赔率
      - 按月汇总表格：月份 | 保费收入 | 保单数 | 理赔数 | 理赔金额
  - new ReportEntity → repository.save() → 返回实体

getReportById(Long id) → ReportEntity
  - repository.findById(id).orElseThrow(...)

getReportHtml(Long id) → String
  - getReportById(id).getHtmlContent()
```

模拟数据：随机生成 3 个月的数据，数值在一定范围内波动。

### 3.6 ReportGenerationTool

```java
public class ReportGenerationTool
        implements Function<ReportGenerationTool.Request, ReportGenerationTool.Response> {

    private final ReportService reportService;

    public record Request(
        @JsonProperty(required = true) String insuranceType,
        @JsonProperty(required = true) String startDate,
        @JsonProperty(required = true) String endDate
    ) {}

    public record Response(
        Long reportId,
        String reportName,
        String url
    ) {}
}
```

`apply()` 方法调用 `reportService.generateReport()`，返回结构化 Response。

### 3.7 注册 Tool（ReactAgentConfig.java）

```java
@Bean
public ToolCallback reportGenerationCallback() {
    ReportGenerationTool tool = new ReportGenerationTool(reportService);
    return FunctionToolCallback
            .builder("generate_report", tool)
            .description("""
                生成保险报表。当用户要求生成报表、查看报表、统计保险数据时调用。
                参数说明：
                - insuranceType: 险种，可选值：车险/健康险/寿险/意外险/财产险
                - startDate: 起始日期，格式 yyyy-MM-dd
                - endDate: 截止日期，格式 yyyy-MM-dd
                """)
            .inputType(ReportGenerationTool.Request.class)
            .build();
}
```

该 Bean 会自动被 `RagChatService` 的 `List<ToolCallback> builtinTools` 收集，无需额外配置。

### 3.8 ReportController

```
GET /api/reports/{id}/html
  → Content-Type: text/html;charset=UTF-8
  → 返回 reports 表中 html_content 字段
```

### 3.9 SSE 报表事件（RagChatService.java）

在 `chatStream()` 方法的流式响应结束后、`[DONE]` 之前，检查本轮对话是否触发了报表生成：

- `ReportGenerationTool` 中将生成的 report 元数据放入 `ConcurrentHashMap<String, ReportInfo>`
- `RagChatService` 在流结束后从 map 中取出，拼接为 `event:report` SSE 事件

```java
.concatWith(Flux.defer(() -> {
    ReportInfo info = reportGenerationTool.pollReport(conversationId);
    if (info != null) {
        return Flux.just(ServerSentEvent.builder(info.toJson()).event("report").build());
    }
    return Flux.empty();
}))
```

---

## 4. 前端设计

### 4.1 修改文件

| 文件 | 改动 |
|------|------|
| `src/services/api.js` | SSE 解析增加 `event:report` 分支 |
| `src/pages/ChatQA.jsx` | switch 增加 `case 'report'` 处理 |
| `src/components/chat/MessageBubble.jsx` | 新增报表卡片渲染 |
| `vite.config.js` | 新增 `/api/reports` 代理到 chat-service 端口 |

### 4.2 SSE 解析（api.js）

在 `processBlock()` 函数中新增对 `report` 事件类型的处理：

```javascript
if (eventType === 'reasoning') {
    onData({ type: 'reasoning', content: data });
} else if (eventType === 'report') {
    onData({ type: 'report', data: JSON.parse(data) });
} else {
    onData({ type: 'message', content: data });
}
```

### 4.3 聊天流处理（ChatQA.jsx）

在 `handleSend` 的 switch 中新增：

```javascript
case 'report':
    updateAssistant({ report: data });
    // data = { reportId: 1, reportName: "车险报表-...", url: "/api/reports/1/html" }
    break;
```

### 4.4 报表卡片渲染（MessageBubble.jsx）

当消息对象包含 `report` 字段时，在消息内容下方渲染可点击卡片：

```jsx
{msg.report && (
    <Card
        size="small"
        hoverable
        onClick={() => window.open(msg.report.url, '_blank')}
        style={{ marginTop: 12, borderLeft: '3px solid #1890ff' }}
    >
        <Space>
            <FileTextOutlined style={{ color: '#1890ff' }} />
            <span>{msg.report.reportName}</span>
        </Space>
    </Card>
)}
```

### 4.5 Vite 代理（vite.config.js）

新增代理规则，将 `/api/reports` 转发到 chat-service（端口 8083）：

```javascript
'/api/reports': {
    target: 'http://localhost:8083',
    changeOrigin: true,
}
```

---

## 5. 改动文件清单汇总

| # | 文件 | 操作 | 估计行数 |
|---|------|------|----------|
| 1 | `chat-service/.../entity/ReportEntity.java` | 新建 | ~45 |
| 2 | `chat-service/.../repository/jpa/ReportJpaRepository.java` | 新建 | ~10 |
| 3 | `chat-service/.../service/ReportService.java` | 新建 | ~120 |
| 4 | `chat-service/.../service/ReportGenerationTool.java` | 新建 | ~50 |
| 5 | `chat-service/.../controller/ReportController.java` | 新建 | ~25 |
| 6 | `chat-service/.../config/ReactAgentConfig.java` | 修改 | +20 |
| 7 | `chat-service/.../service/RagChatService.java` | 修改 | +15 |
| 8 | `frontend/src/services/api.js` | 修改 | +3 |
| 9 | `frontend/src/pages/ChatQA.jsx` | 修改 | +3 |
| 10 | `frontend/src/components/chat/MessageBubble.jsx` | 修改 | +15 |
| 11 | `frontend/vite.config.js` | 修改 | +5 |

---

## 6. 验证步骤

1. 启动 chat-service，检查 `reports` 表是否自动创建
2. 调用 `POST /api/chat/stream` 发送 "帮我生成2026年1月到3月的车险报表"
3. 确认 SSE 流中包含 `event:report` 事件
4. 前端聊天界面中出现报表卡片
5. 点击报表名称 → 新标签页打开 → HTML 报表正确展示
6. 直接访问 `GET /api/reports/{id}/html` 返回完整 HTML
