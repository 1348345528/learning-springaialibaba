# GraphAgent End-to-End Requests and Responses

- Test time: 2026-06-29 23:35:49 +0800
- Endpoint: POST http://localhost:8083/api/graph-agent/stream
- Scope: this file records only the actual request body I sent and the actual response body returned by the API.
- Covered flows: ReactAgent route, invoice preview + confirm generation, invoice preview + reject exit.

## 1. ReactAgent normal route

### Actual Sent Content

```json
{
  "conversationId": "rE2E11",
  "message": "你好，请用一句话介绍你自己，不要开票。",
  "stream": true,
  "toolNames": []
}
```

### Actual Returned Content

- HTTP status: 200
- Content-Type: text/event-stream;charset=UTF-8

#### Raw SSE Response

```text
event:reasoning
data:用户

event:reasoning
data:要求

event:reasoning
data:用一句话

event:reasoning
data:介绍自己

event:reasoning
data:，并且

event:reasoning
data:不要开

event:reasoning
data:票（

event:reasoning
data:不生成

event:reasoning
data:报表

event:reasoning
data:）。我

event:reasoning
data:简单介绍一下

event:reasoning
data:自己

event:reasoning
data:即可。

event:message
data:你好

event:message
data:！

event:message
data:我是智能

event:message
data:知识库

event:message
data:助手，

event:message
data:可以帮你

event:message
data:搜索知识

event:message
data:库

event:message
data:、查询

event:message
data:时间、

event:message
data:计算数学

event:message
data:题以及

event:message
data:生成保险

event:message
data:报表，

event:message
data:随时为你

event:message
data:提供准确

event:message
data:高效

event:message
data:的服务！

event:message
data:😊

event:message
data:

event:message
data:有什么需要

event:message
data:帮忙的吗

event:message
data:？

data:[DONE]
```

#### Aggregated SSE Events

```text
[reasoning]
用户

[reasoning]
要求

[reasoning]
用一句话

[reasoning]
介绍自己

[reasoning]
，并且

[reasoning]
不要开

[reasoning]
票（

[reasoning]
不生成

[reasoning]
报表

[reasoning]
）。我

[reasoning]
简单介绍一下

[reasoning]
自己

[reasoning]
即可。

[message]
你好

[message]
！

[message]
我是智能

[message]
知识库

[message]
助手，

[message]
可以帮你

[message]
搜索知识

[message]
库

[message]
、查询

[message]
时间、

[message]
计算数学

[message]
题以及

[message]
生成保险

[message]
报表，

[message]
随时为你

[message]
提供准确

[message]
高效

[message]
的服务！

[message]
😊

[message]


[message]
有什么需要

[message]
帮忙的吗

[message]
？

[message]
[DONE]

```

## 2. Invoice route A - start invoice and return preview for user confirmation

### Actual Sent Content

```json
{
  "conversationId": "iE2E11",
  "message": "请帮我给保单 P321101 开具发票。",
  "stream": true,
  "toolNames": []
}
```

### Actual Returned Content

- HTTP status: 200
- Content-Type: text/event-stream;charset=UTF-8

#### Raw SSE Response

```text
event:message
data:Invoice preview is ready. Please reply with confirm or reject.
data:Policy: P321101
data:Buyer: Zhang San
data:Tax No: 91310000MOCK12345X
data:Item: Insurance premium
data:Amount: 1288.00
data:

event:invoice_preview
data:{"policyNo":"P321101","policyHolder":"Zhang San","buyerName":"Zhang San","taxNo":"91310000MOCK12345X","amount":1288.0,"itemName":"Insurance premium"}

data:[DONE]
```

#### Aggregated SSE Events

```text
[message]
Invoice preview is ready. Please reply with confirm or reject.
Policy: P321101
Buyer: Zhang San
Tax No: 91310000MOCK12345X
Item: Insurance premium
Amount: 1288.00


[invoice_preview]
{"policyNo":"P321101","policyHolder":"Zhang San","buyerName":"Zhang San","taxNo":"91310000MOCK12345X","amount":1288.0,"itemName":"Insurance premium"}

[message]
[DONE]

```

## 3. Invoice route A - user confirms and invoice is generated

### Actual Sent Content

```json
{
  "conversationId": "iE2E11",
  "message": "确认开票。",
  "stream": true,
  "toolNames": []
}
```

### Actual Returned Content

- HTTP status: 200
- Content-Type: text/event-stream;charset=UTF-8

#### Raw SSE Response

```text
event:message
data:Invoice generated: INV-P321101

event:invoice_result
data:{"invoiceNo":"INV-P321101","policyNo":"P321101","status":"GENERATED","downloadUrl":"/api/mock-invoices/INV-P321101.pdf"}

data:[DONE]
```

#### Aggregated SSE Events

```text
[message]
Invoice generated: INV-P321101

[invoice_result]
{"invoiceNo":"INV-P321101","policyNo":"P321101","status":"GENERATED","downloadUrl":"/api/mock-invoices/INV-P321101.pdf"}

[message]
[DONE]

```

## 4. Invoice route B - start invoice and return preview for user confirmation

### Actual Sent Content

```json
{
  "conversationId": "iE2E12",
  "message": "请帮我给保单 P321102 开具发票。",
  "stream": true,
  "toolNames": []
}
```

### Actual Returned Content

- HTTP status: 200
- Content-Type: text/event-stream;charset=UTF-8

#### Raw SSE Response

```text
event:message
data:Invoice preview is ready. Please reply with confirm or reject.
data:Policy: P321102
data:Buyer: Zhang San
data:Tax No: 91310000MOCK12345X
data:Item: Insurance premium
data:Amount: 1288.00
data:

event:invoice_preview
data:{"policyNo":"P321102","policyHolder":"Zhang San","buyerName":"Zhang San","taxNo":"91310000MOCK12345X","amount":1288.0,"itemName":"Insurance premium"}

data:[DONE]
```

#### Aggregated SSE Events

```text
[message]
Invoice preview is ready. Please reply with confirm or reject.
Policy: P321102
Buyer: Zhang San
Tax No: 91310000MOCK12345X
Item: Insurance premium
Amount: 1288.00


[invoice_preview]
{"policyNo":"P321102","policyHolder":"Zhang San","buyerName":"Zhang San","taxNo":"91310000MOCK12345X","amount":1288.0,"itemName":"Insurance premium"}

[message]
[DONE]

```

## 5. Invoice route B - user rejects and invoice flow exits

### Actual Sent Content

```json
{
  "conversationId": "iE2E12",
  "message": "驳回，退出开票流程。",
  "stream": true,
  "toolNames": []
}
```

### Actual Returned Content

- HTTP status: 200
- Content-Type: text/event-stream;charset=UTF-8

#### Raw SSE Response

```text
event:message
data:Invoice flow cancelled.

data:[DONE]
```

#### Aggregated SSE Events

```text
[message]
Invoice flow cancelled.

[message]
[DONE]

```

## Verification Summary

- ReactAgent normal route: HTTP 200
- Invoice route A - start invoice and return preview for user confirmation: HTTP 200 (returned invoice_preview)
- Invoice route A - user confirms and invoice is generated: HTTP 200 (returned invoice_result)
- Invoice route B - start invoice and return preview for user confirmation: HTTP 200 (returned invoice_preview)
- Invoice route B - user rejects and invoice flow exits: HTTP 200
