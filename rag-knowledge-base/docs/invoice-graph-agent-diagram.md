# InvoiceGraphAgent Graph Diagram

```mermaid
flowchart TD
    START(["__START__"])
    BEFORE["beforeAgent hooks\nChatHistorySync.before"]
    ROUTER["intent_router\nLLM intent classifier\nroute + policyNo"]

    BEFORE_MODEL["beforeModel hooks\nSummarization.beforeModel"]
    MODEL["_AGENT_MODEL_\nAgentLlmNode"]
    TOOL["_AGENT_TOOL_\nAgentToolNode"]
    AFTER["afterAgent hooks\nChatHistorySync.after"]
    END(["__END__"])

    INVOICE["invoice_flow\nsingle invoice branch\nuses invoiceAction + invoicePending"]
    START_INVOICE["START action\ncheck eligibility\nassemble preview if eligible\ninvoicePending=true"]
    CONFIRM_INVOICE["CONFIRM action\ngenerate invoice\ninvoicePending=false\ninvoiceResult=..."]
    REJECT_INVOICE["REJECT action\ncancel flow\ninvoicePending=false"]
    NOT_ELIGIBLE["not eligible path\nappend assistant message\ninvoicePending=false"]

    START --> BEFORE --> ROUTER

    ROUTER -- REACT_AGENT --> BEFORE_MODEL --> MODEL
    MODEL -- assistant has tool calls --> TOOL
    TOOL --> BEFORE_MODEL
    MODEL -- no tool calls --> AFTER

    ROUTER -- INVOICE_AGENT --> INVOICE
    INVOICE --> START_INVOICE
    INVOICE --> CONFIRM_INVOICE
    INVOICE --> REJECT_INVOICE
    START_INVOICE -- not eligible --> NOT_ELIGIBLE
    START_INVOICE --> AFTER
    CONFIRM_INVOICE --> AFTER
    REJECT_INVOICE --> AFTER
    NOT_ELIGIBLE --> AFTER

    AFTER --> END
```

## State Keys

- `messages`: appended through the same graph state as ReactAgent.
- `route`: replaced on every intent routing pass. Only `REACT_AGENT` or `INVOICE_AGENT`.
- `invoiceAction`: replaced on every invoice routing pass. `START`, `CONFIRM`, or `REJECT`.
- `policyNo`: extracted by the LLM router or carried from pending invoice state.
- `eligibility`: mocked policy invoice eligibility result.
- `invoicePending`: `true` after preview, `false` after generate/reject/not eligible.
- `invoicePreview`: mocked invoice preview data stored in graph state.
- `invoiceResult`: mocked generated invoice data stored in graph state.
