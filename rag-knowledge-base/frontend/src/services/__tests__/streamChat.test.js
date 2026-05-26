import { describe, expect, it, vi, beforeEach } from 'vitest';
import { chatApi } from '../api';

class MockXMLHttpRequest {
  static instances = [];

  constructor() {
    this.headers = {};
    this.responseText = '';
    this.status = 200;
    this.method = null;
    this.url = null;
    this.requestBody = null;
    this.onprogress = null;
    this.onload = null;
    this.onerror = null;
    MockXMLHttpRequest.instances.push(this);
  }

  open(method, url) {
    this.method = method;
    this.url = url;
  }

  setRequestHeader(name, value) {
    this.headers[name] = value;
  }

  send(body) {
    this.requestBody = body;
  }

  emitProgress(chunk) {
    this.responseText += chunk;
    this.onprogress?.();
  }

  complete(status = this.status) {
    this.status = status;
    this.onload?.();
  }

  fail() {
    this.onerror?.();
  }
}

describe('chatApi.streamChat', () => {
  beforeEach(() => {
    MockXMLHttpRequest.instances = [];
    global.XMLHttpRequest = MockXMLHttpRequest;
  });

  it('parses single-data SSE block as message', async () => {
    const events = [];

    const streamPromise = chatApi.streamChat(
      { message: 'hello' },
      (event) => events.push(event),
      vi.fn()
    );

    const xhr = MockXMLHttpRequest.instances[0];
    xhr.emitProgress('event:message\ndata:Hello World\n\n');
    xhr.complete(200);

    await streamPromise;

    expect(events).toEqual([
      { type: 'message', content: 'Hello World' },
    ]);
  });

  it('parses reasoning event correctly', async () => {
    const events = [];

    const streamPromise = chatApi.streamChat(
      { message: 'hello' },
      (event) => events.push(event),
      vi.fn()
    );

    const xhr = MockXMLHttpRequest.instances[0];
    xhr.emitProgress('event:reasoning\ndata:Let me think...\n\n');
    xhr.emitProgress('event:message\ndata:The answer is 42\n\n');
    xhr.complete(200);

    await streamPromise;

    expect(events).toEqual([
      { type: 'reasoning', content: 'Let me think...' },
      { type: 'message', content: 'The answer is 42' },
    ]);
  });

  it('joins multiple data lines within same block with newline', async () => {
    const events = [];

    const streamPromise = chatApi.streamChat(
      { message: 'hello' },
      (event) => events.push(event),
      vi.fn()
    );

    const xhr = MockXMLHttpRequest.instances[0];
    // 模拟多段落文本（第三条 data: 是空行，表示段落间距）
    xhr.emitProgress('event:message\ndata:段落一。\ndata:\ndata:段落二。\n\n');
    xhr.complete(200);

    await streamPromise;

    expect(events).toEqual([
      { type: 'message', content: '段落一。\n\n段落二。' },
    ]);
  });

  it('handles [DONE] sentinel', async () => {
    const events = [];

    const streamPromise = chatApi.streamChat(
      { message: 'hi' },
      (event) => events.push(event),
      vi.fn()
    );

    const xhr = MockXMLHttpRequest.instances[0];
    xhr.emitProgress('event:message\ndata:Hi\n\n');
    xhr.emitProgress('data:[DONE]\n\n');
    xhr.complete(200);

    await streamPromise;

    expect(events).toEqual([
      { type: 'message', content: 'Hi' },
      { type: 'done' },
    ]);
  });

  it('treats data without event as message type', async () => {
    const events = [];

    const streamPromise = chatApi.streamChat(
      { message: 'hi' },
      (event) => events.push(event),
      vi.fn()
    );

    const xhr = MockXMLHttpRequest.instances[0];
    xhr.emitProgress('data:plain text\n\n');
    xhr.complete(200);

    await streamPromise;

    expect(events).toEqual([
      { type: 'message', content: 'plain text' },
    ]);
  });

  it('handles block split across progress chunks', async () => {
    const events = [];

    const streamPromise = chatApi.streamChat(
      { message: 'hi' },
      (event) => events.push(event),
      vi.fn()
    );

    const xhr = MockXMLHttpRequest.instances[0];
    // 一个 SSE block 跨两个 progress 到达
    xhr.emitProgress('event:mess');
    xhr.emitProgress('age\ndata:partial chunk\n\n');
    xhr.complete(200);

    await streamPromise;

    expect(events).toEqual([
      { type: 'message', content: 'partial chunk' },
    ]);
  });

  it('strips optional space after data: prefix', async () => {
    const events = [];

    const streamPromise = chatApi.streamChat(
      { message: 'hi' },
      (event) => events.push(event),
      vi.fn()
    );

    const xhr = MockXMLHttpRequest.instances[0];
    // SSE 规范允许 data: 后面有一个空格
    xhr.emitProgress('event:message\ndata: with space\n\n');
    xhr.complete(200);

    await streamPromise;

    expect(events).toEqual([
      { type: 'message', content: 'with space' },
    ]);
  });

  it('reports errors via onError', () => {
    const onError = vi.fn();

    chatApi.streamChat({ message: 'hi' }, vi.fn(), onError);

    const xhr = MockXMLHttpRequest.instances[0];
    xhr.fail();

    expect(onError).toHaveBeenCalledWith('网络连接失败');
  });
});
