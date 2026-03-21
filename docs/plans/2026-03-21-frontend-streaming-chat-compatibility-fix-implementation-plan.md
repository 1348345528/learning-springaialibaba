# Frontend Streaming Chat Compatibility Fix Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix the frontend chat page so streamed assistant output is rendered in real time, finalized correctly when the stream ends, and compatible with both plain-text SSE and structured JSON SSE.

**Architecture:** Keep the backend contract unchanged and strengthen the frontend stream consumer. Normalize all incoming SSE payloads into a small internal event model (`chunk`, `done`, `error`, `end-without-done`) inside the API layer, then let the page component render and finalize messages from that normalized stream.

**Tech Stack:** React 18, Vite 5, XMLHttpRequest, Server-Sent Events (SSE), Ant Design 5

---

## File Map

- Modify: `rag-knowledge-base/frontend/src/services/api.js` — parse raw SSE chunks, normalize plain-text and JSON payloads, and provide a reliable completion callback when the stream ends without an explicit `done` event.
- Modify: `rag-knowledge-base/frontend/src/pages/ChatTest.jsx` — manage transient streamed text separately from finalized messages and finalize the assistant message exactly once.
- Reference: `docs/plans/2026-03-21-frontend-streaming-chat-compatibility-fix-design.md` — approved design and expected compatibility behavior.
- Optional test helper (only if needed): `rag-knowledge-base/frontend/src/services/__tests__/streamChat.test.js` — regression tests for stream normalization.

## Chunk 1: Normalize incoming stream events

### Task 1: Add a failing regression test for plain-text SSE completion

**Files:**
- Create: `rag-knowledge-base/frontend/src/services/__tests__/streamChat.test.js`
- Modify: `rag-knowledge-base/frontend/package.json` (only if a test script or test dependency is required)

- [ ] **Step 1: Write the failing test**

Create a test that simulates an XHR stream delivering plain-text SSE chunks with no explicit `done` event:

```js
test('finalizes plain-text SSE when request ends without done event', async () => {
  const events = [];

  await streamChatWithFakeXhr([
    'data: Hello\n\n',
    'data: world\n\n'
  ], {
    onMessage: (event) => events.push(event)
  });

  expect(events).toEqual([
    { type: 'chunk', content: 'Hello' },
    { type: 'chunk', content: 'world' },
    { type: 'done', fullResponse: 'Helloworld', sources: [] }
  ]);
});
```

The exact whitespace expectations can reflect your parser design, but the test must prove that a `done`-equivalent event is emitted when the transport ends.

- [ ] **Step 2: Run the test to verify it fails**

Run from `rag-knowledge-base/frontend`:

```bash
npm test -- streamChat
```

Expected: FAIL because the current implementation does not emit a final completion event for plain-text SSE.

- [ ] **Step 3: Implement the minimal parser support in `api.js`**

Add a small internal parser that:
- accumulates raw `xhr.responseText`
- extracts complete SSE event blocks
- recognizes `data: ...` lines
- emits normalized events
- tracks the concatenated assistant text
- emits a final `done` event on `load` if the request succeeded and no explicit `done` arrived

Do not redesign the whole API client.

- [ ] **Step 4: Run the test to verify it passes**

Run:

```bash
npm test -- streamChat
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add rag-knowledge-base/frontend/src/services/__tests__/streamChat.test.js rag-knowledge-base/frontend/src/services/api.js rag-knowledge-base/frontend/package.json
git commit -m "fix: normalize plain text chat stream completion"
```

### Task 2: Add a failing regression test for structured JSON SSE

**Files:**
- Modify: `rag-knowledge-base/frontend/src/services/__tests__/streamChat.test.js`

- [ ] **Step 1: Write the failing test**

Add a second test for structured events:

```js
test('preserves structured JSON chunk and done SSE events', async () => {
  const events = [];

  await streamChatWithFakeXhr([
    'data: {"type":"chunk","content":"Hello"}\n\n',
    'data: {"type":"done","fullResponse":"Hello world","sources":[]}\n\n'
  ], {
    onMessage: (event) => events.push(event)
  });

  expect(events).toEqual([
    { type: 'chunk', content: 'Hello' },
    { type: 'done', fullResponse: 'Hello world', sources: [] }
  ]);
});
```

- [ ] **Step 2: Run the test to verify it fails**

Run:

```bash
npm test -- streamChat
```

Expected: FAIL if the parser does not preserve the JSON event shape correctly.

- [ ] **Step 3: Implement the minimal parser support**

Ensure the parser:
- tries JSON parsing first for `data:` payloads
- forwards valid structured events unchanged
- only falls back to plain-text chunk events when JSON parsing fails
- handles `[DONE]` as a completion signal

- [ ] **Step 4: Run the test to verify it passes**

Run:

```bash
npm test -- streamChat
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add rag-knowledge-base/frontend/src/services/__tests__/streamChat.test.js rag-knowledge-base/frontend/src/services/api.js
git commit -m "fix: support structured chat stream events"
```

## Chunk 2: Finalize assistant messages exactly once in the page

### Task 3: Add a failing UI regression test or minimal reproduction harness for finalization

**Files:**
- Modify: `rag-knowledge-base/frontend/src/pages/ChatTest.jsx`
- Optional Test: `rag-knowledge-base/frontend/src/pages/__tests__/ChatTest.test.jsx`

- [ ] **Step 1: Write the failing test (preferred) or minimal reproduction harness**

Preferred behavior to test:

```js
test('writes streamed assistant text into messages when the stream completes without explicit done', async () => {
  render(<ChatTest />);

  sendMessage('test question');
  emitNormalizedStreamEvent({ type: 'chunk', content: 'Hello' });
  emitNormalizedStreamEvent({ type: 'chunk', content: ' world' });
  completeStreamWithoutDone();

  expect(screen.getByText('Hello world')).toBeInTheDocument();
  expect(screen.queryByTestId('streaming-message')).not.toBeInTheDocument();
});
```

If the current frontend test setup is missing, create the smallest practical harness that still proves the bug before fixing it.

- [ ] **Step 2: Run the test to verify it fails**

Run the targeted frontend test command appropriate for the project setup.

Expected: FAIL because the current page keeps streamed text transient and does not finalize it reliably.

- [ ] **Step 3: Implement the minimal page-state fix**

In `ChatTest.jsx`:
- keep transient streamed text in state for live rendering
- keep a stable buffer/ref for the authoritative accumulated assistant text
- finalize exactly once when receiving `done` or stream completion fallback
- avoid relying on a stale `streamResponse` closure inside async callbacks
- ensure loading is cleared on success and error

Use stable state transitions and functional state updates to avoid React stale closure issues.

- [ ] **Step 4: Run the test to verify it passes**

Run the targeted test command.

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add rag-knowledge-base/frontend/src/pages/ChatTest.jsx rag-knowledge-base/frontend/src/pages/__tests__/ChatTest.test.jsx
git commit -m "fix: finalize streamed assistant messages reliably"
```

### Task 4: Verify real app behavior against the running local backend

**Files:**
- Modify: none
- Verify: running frontend and backend processes already started locally

- [ ] **Step 1: Start or confirm the frontend dev server**

Run from `rag-knowledge-base/frontend` if needed:

```bash
npm run dev
```

Expected: Vite serves the app on `http://localhost:3000`.

- [ ] **Step 2: Verify the backend stream endpoint produces SSE output**

Run:

```bash
curl -i -N -H "Content-Type: application/json" -X POST "http://localhost:8083/api/chat/stream" --data "{\"message\":\"ping\"}"
```

Expected: `HTTP/1.1 200` and one or more `data:` lines.

- [ ] **Step 3: Verify frontend live rendering and finalization**

In the browser:
1. Open `http://localhost:3000`
2. Go to the chat test page
3. Send a short prompt like `ping`
4. Observe the assistant output appears incrementally
5. Confirm the final assistant message remains in the message history after the network request completes

Expected:
- streaming text appears live
- no stuck loading spinner
- final message remains visible
- no duplicate assistant message

- [ ] **Step 4: Verify compatibility with both stream styles**

At minimum verify:
- current real backend plain-text SSE works in the browser
- the structured JSON SSE path is covered by automated tests from Tasks 1-2

- [ ] **Step 5: Commit**

```bash
git add rag-knowledge-base/frontend/src/services/api.js rag-knowledge-base/frontend/src/pages/ChatTest.jsx rag-knowledge-base/frontend/src/services/__tests__/streamChat.test.js rag-knowledge-base/frontend/src/pages/__tests__/ChatTest.test.jsx
git commit -m "fix: support compatible streaming chat rendering"
```

## Completion criteria

- Plain-text SSE is rendered incrementally in the chat UI
- Structured JSON SSE remains supported
- Stream completion without explicit `done` still finalizes the assistant message
- Final assistant output format is stable and stored exactly once
- No stuck loading state remains after success or error
- Targeted tests pass and real local verification succeeds

## Notes for the implementing agent

- Keep the frontend contract narrow: normalize transport differences in `api.js`, not in the page component.
- Prefer small helper functions over large inlined parsing branches.
- Avoid expensive React state churn: use a ref for the authoritative accumulated text when appropriate, and keep state updates minimal.
- Do not change the backend stream format as part of this plan.
