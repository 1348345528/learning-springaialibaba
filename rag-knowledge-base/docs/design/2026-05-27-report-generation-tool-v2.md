# 报表生成 Tool 设计文档 v2 — AG-UI 风格流式事件方案

> 日期：2026-05-27
> 状态：设计中（替代 v1 AtomicReference side-channel 方案）

---

## 1. 为什么需要 v2

v1 方案（`2026-05-27-report-generation-tool-design.md`）使用 `AtomicReference<ReportInfo>` 作为 Tool → Service 的数据传递通道，存在三个问题：

| 问题 | 说明 |
|------|------|
| **多用户覆盖** | 单槽 `AtomicReference`，两个并发请求中后执行的会覆盖先执行的 |
| **多次调用覆盖** | 同一个 turn 内 AI 多次调用 `generate_report`，只保留最后一个 |
| **架构不标准** | side-channel 不是框架设计者预期的用法 |

---

## 2. 核心发现：`agent.streamMessages()` 原生支持工具结果

阅读 Spring AI Alibaba `1.1.2.0` 源码（`Agent.java` 第 602 行）发现：

```java
// Agent.java — isMessageOutputType()
private static boolean isMessageOutputType(OutputType type) {
    return type == OutputType.AGENT_MODEL_STREAMING 
        || type == OutputType.AGENT_TOOL_FINISHED;
}
```

`agent.streamMessages()` 过滤器同时接受 `AGENT_MODEL_STREAMING` 和 `AGENT_TOOL_FINISHED`，这意味着**工具执行结果（`ToolResponseMessage`）会作为流事件直接出现**，不需要任何外部 side-channel。

```
agent.streamMessages() → Flux<Message>
  ├─ AssistantMessage       ← LLM 文本输出（含 reasoning metadata）
  └─ ToolResponseMessage    ← 工具返回值（含 toolName + responseData JSON）
```

这完全符合 AG-UI 协议的设计思想：工具调用是前端可见的一等公民事件。

参考源码路径：
- `spring-ai-alibaba-agent-framework/.../Agent.java:474,507-510,587-605`
- `spring-ai-alibaba-agent-framework/.../node/AgentToolNode.java:154-252,530-578`
- `spring-ai-alibaba-graph-core/.../streaming/OutputType.java:22-30`

---

## 3. 改动概览

| # | 文件 | 操作 | 说明 |
|---|------|------|------|
| 1 | `ReportService.java` | 修改 | 分离 mock 数据生成与 HTML 构建，生成预览数据 |
| 2 | `ReportGenerationTool.java` | 修改 | 丰富 Response 字段，删除 AtomicReference/pollReport/peekReport/ReportInfo |
| 3 | `RagChatService.java` | 修改 | `agent.stream()` → `agent.streamMessages()`，内联处理 ToolResponseMessage |
| 4 | `ChatHistorySyncHook.java` | 修改 | 扫描 messages 列表中的 ToolResponseMessage，移除 peekReport 依赖 |
| 5 | `ReactAgentConfig.java` | 不变 | — |
| 6 | `api.js` | 修改 | SSE 解析新增 `tool_call_result` 事件类型 |
| 7 | `ChatQA.jsx` | 修改 | 新增 `tool_call_result` 分支，累积到消息的 toolResults 数组 |
| 8 | `MessageBubble.jsx` | 修改 | 改为 `displayType` 驱动的通用工具结果渲染 |

---

## 4. 详细设计

### 4.1 ReportGenerationTool.Response — 双用途返回值

```java
public record Response(
    @JsonProperty(required = true) String message,       // LLM 消费：自然语言摘要
    @JsonProperty(required = true) String displayType,   // 前端渲染 key："report_card"
    Long reportId,
    String reportName,
    String url,
    Map<String, Object> preview                          // 卡片预览数据
) {}
```

Tool 返回值被 `MessageToolCallResultConverter` 序列化为 JSON 后，同时服务于两条路径：

| 消费者 | 路径 | 用途 |
|--------|------|------|
| **LLM** | `ToolResponseMessage` → 注入 Agent state → LLM 上下文 | 读取 `message` 字段，生成自然语言回复 |
| **前端** | `ToolResponseMessage` → `streamMessages()` → SSE `tool_call_result` 事件 | 读取 `displayType`、`preview`、`url`，渲染对应组件 |

示例 JSON：
```json
{
  "message": "车险报表已生成，覆盖2026-01至2026-03共3个月。保费总额125万，保单342单，理赔28笔。",
  "displayType": "report_card",
  "reportId": 1,
  "reportName": "车险报表-2026-01-01至2026-03-31",
  "url": "/api/reports/1/html",
  "preview": {
    "totalPremium": 1250000,
    "totalPolicies": 342,
    "totalClaims": 28,
    "claimRate": "12.5%",
    "monthCount": 3
  }
}
```

### 4.2 ReportService — 分离数据生成

当前 `buildHtml()` 方法内联生成 mock 数据并构建 HTML。重构后：

```
ReportService:
  generateReportData(insuranceType, startDate, endDate) → ReportData
    └─ 返回: { totalPremium, totalPolicies, totalClaims, claimRate, monthlyRows[] }

  generateReport(insuranceType, startDate, endDate) → GenerateResult
    ├─ 调用 generateReportData() 获取数据
    ├─ 调用 buildHtml(reportData) 构建 HTML
    ├─ 保存 ReportEntity 到 MySQL
    └─ 返回 GenerateResult { entity, reportData }
```

新增内部类型：
```java
/** 报表 mock 数据，供 HTML 构建和 preview 字段共用 */
public record ReportData(
    BigDecimal totalPremium,
    int totalPolicies,
    BigDecimal totalClaims,
    BigDecimal claimRate,
    List<MonthlyRow> monthlyRows
) {
    public record MonthlyRow(String month, BigDecimal premium, int policies, int claims, BigDecimal claimAmount) {}
}

/** generateReport() 返回值 */
public record GenerateResult(ReportEntity entity, ReportData data) {}
```

### 4.3 RagChatService — agent.streamMessages() 替换 agent.stream()

改动范围：`chatStream()` 方法

```
当前:
  agent.stream() → Flux<NodeOutput>
    ↓ flatMap(extractToken)  只处理 AGENT_MODEL_STREAMING
    ↓ .doOnNext(fullContent 累积)
    ↓ .concatWith([DONE])
    ↓ .concatWith(pollReport → event:report)  ← 删除

改为:
  agent.streamMessages() → Flux<Message>
    ↓ flatMap:
      ├─ ToolResponseMessage → event:tool_call_result
      ├─ AssistantMessage(有 reasoningContent) → event:reasoning
      └─ AssistantMessage(有 text) → event:message
    ↓ .doOnNext(event:message 时累积 fullContent)
    ↓ .concatWith([DONE] + autoTitle)
```

伪代码：
```java
Flux<Message> messageFlux = agent.streamMessages(request.getMessage(), config);

Flux<ServerSentEvent<String>> eventFlux = messageFlux
    .flatMap(msg -> {
        if (msg instanceof ToolResponseMessage toolMsg) {
            return Flux.fromIterable(toolMsg.getResponses())
                .filter(r -> r.responseData() != null)
                .map(r -> ServerSentEvent.builder(r.responseData())
                    .event("tool_call_result")
                    .build());
        }
        if (msg instanceof AssistantMessage assistantMsg) {
            return processAssistantMessage(assistantMsg);
        }
        return Flux.empty();
    });
```

`processAssistantMessage()` 保持原有逻辑：从 metadata 提取 reasoningContent → `event:reasoning`，从 text 提取内容 → `event:message`。

### 4.4 SSE 事件流变化

**v1 事件序列：**
```
event:reasoning  (思考过程)
event:message    (文本块 × N)
data:[DONE]
event:report     {reportId, reportName, url}    ← 自定义事件，在流末尾
```

**v2 事件序列（AG-UI 风格）：**
```
event:reasoning       (思考过程)
event:message         (文本块 × N：AI 分析用户需求)
event:tool_call_result {displayType:"report_card", ...}   ← 内联在流中
event:message         (文本块 × N：AI 总结报表内容)
data:[DONE]
```

`tool_call_result` 事件出现在 LLM 调用工具后、LLM 继续生成回复前的位置——这正是 AG-UI 协议中 `TOOL_CALL_RESULT` 的标准时序。

### 4.5 ChatHistorySyncHook — 扫描消息替代 peekReport

当前 `syncToMysql()` 通过 `reportGenerationTool.peekReport()` 获取报表信息。重构后在消息列表中直接扫描：

```java
// 从消息列表中提取所有 generate_report 的返回值
private List<ReportInfo> extractReportInfos(List<Message> messages) {
    List<ReportInfo> infos = new ArrayList<>();
    for (Message msg : messages) {
        if (msg instanceof ToolResponseMessage toolMsg) {
            for (ToolResponseMessage.ToolResponse r : toolMsg.getResponses()) {
                if ("generate_report".equals(r.name()) && r.responseData() != null) {
                    ReportInfo info = parseFromJson(r.responseData());
                    if (info != null) infos.add(info);
                }
            }
        }
    }
    return infos;
}
```

保存 assistant 消息时取最后一个 `ReportInfo` 的 url/name 写入 `ChatMessageEntity`。

移除对 `ReportGenerationTool` 的依赖注入。

### 4.6 前端变更

**api.js** — SSE 解析：
```javascript
// 在 processBlock() 函数的事件分发中新增：
else if (eventType === 'tool_call_result') {
    const parsed = JSON.parse(data);
    onData({ type: 'tool_call_result', data: parsed });
}
```

**ChatQA.jsx** — handleSend：
```javascript
case 'tool_call_result':
    updateAssistant((prev) => ({
        toolResults: [...(prev.toolResults || []), data]
    }));
    break;
```

**MessageBubble.jsx** — 通用工具结果渲染：
```jsx
{msg.toolResults?.map((result, i) => {
    switch (result.displayType) {
        case 'report_card':
            return <ReportCard key={i} data={result} />;
        default:
            return null;
    }
})}
```

`<ReportCard>` 组件逻辑与当前 `msg.report` 的 Card + Modal 渲染一致，数据来源从 `msg.report` 改为 `result`。

---

## 5. 数据流

```
用户: "帮我生成车险和健康险的报表"
    │
    ▼
RagChatService.chatStream()
    → ReactAgent 构建（与 v1 相同）
    → agent.streamMessages(message, config)
    │
    ▼  ReAct 循环
    │
    ├─ LLM 输出: AssistantMessage("好的，我来为您生成...")
    │     → SSE event:message
    │
    ├─ LLM 决定调用 generate_report(车险)
    │     → AgentToolNode 执行 ReportGenerationTool.apply()
    │     → 返回 Response{message, displayType, reportId, ...}
    │     → 框架自动序列化为 ToolResponseMessage
    │     → streamMessages() 发出
    │     → SSE event:tool_call_result {displayType:"report_card", reportId:1, ...}
    │
    ├─ LLM 决定调用 generate_report(健康险)
    │     → ...同上...
    │     → SSE event:tool_call_result {displayType:"report_card", reportId:2, ...}
    │
    ├─ LLM 输出: AssistantMessage("两份报表已生成...")
    │     → SSE event:message
    │
    └─ Agent 结束 → afterAgent Hook
          └─ ChatHistorySyncHook.syncToMysql()
               ├─ 扫描 messages，找到 2 个 generate_report 的 ToolResponseMessage
               ├─ 取最后一个的 reportUrl/reportName
               └─ 写入 ChatMessageEntity
```

---

## 6. 与 v1 方案对比

| 维度 | v1 | v2 |
|------|----|----|
| 数据传递 | `AtomicReference<ReportInfo>` side-channel | `agent.streamMessages()` 原生流事件 |
| 多用户安全 | ❌ 单槽覆盖 | ✅ 无共享状态，天然隔离 |
| 多工具调用 | ❌ 只保留最后一次 | ✅ 每次调用独立发出事件 |
| LLM 可见的返回值 | `{reportId, reportName, url}` | `{message, displayType, reportId, reportName, url, preview}` |
| 前端渲染数据来源 | 解析 `event:report` 自定义事件 | 解析 `event:tool_call_result` 通用事件 |
| 新增工具类型的改动 | 需新增 SSE 事件类型 + 前端分支 | 只需加一个 `displayType` → 组件映射 |
| 框架侵入性 | 绕过框架的 hack | 框架 API 的正常使用方式 |

---

## 7. 改动文件清单

| # | 文件 | 操作 | 估计行数 |
|---|------|------|----------|
| 1 | `chat-service/.../service/ReportService.java` | 修改 | +50 / -20 |
| 2 | `chat-service/.../service/ReportGenerationTool.java` | 修改 | +30 / -30 |
| 3 | `chat-service/.../service/RagChatService.java` | 修改 | +40 / -50 |
| 4 | `chat-service/.../hook/ChatHistorySyncHook.java` | 修改 | +25 / -15 |
| 5 | `frontend/src/services/api.js` | 修改 | +3 |
| 6 | `frontend/src/pages/ChatQA.jsx` | 修改 | +4 |
| 7 | `frontend/src/components/chat/MessageBubble.jsx` | 修改 | +15 / -20 |
| — | `chat-service/.../config/ReactAgentConfig.java` | 不变 | 0 |

---

## 8. 验证步骤

1. `mvn compile` — 后端编译通过
2. 启动全部服务，admin 用户登录
3. 发送 "帮我生成2026年1月到3月的车险报表"
   - 确认 SSE 流中 `tool_call_result` 事件出现在 LLM 文本之间
   - 确认前端渲染报表卡片，点击可查看 HTML
4. 发送 "帮我生成车险和健康险的报表"
   - 确认收到两个 `tool_call_result` 事件
   - 确认两个报表卡片都正确渲染
5. 刷新页面，查看历史消息
   - 确认历史消息中报表卡片仍然可见（从 MySQL 加载）
6. 两个不同浏览器用户同时发送报表请求
   - 确认各自收到正确的报表（无交叉污染）
