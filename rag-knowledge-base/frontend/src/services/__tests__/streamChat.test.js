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

  it('parses plain-text SSE blocks and emits fallback done with full response', async () => {
    const events = [];
    const errors = [];

    const streamPromise = chatApi.streamChat(
      { message: 'hello' },
      (event) => events.push(event),
      (error) => errors.push(error)
    );

    const xhr = MockXMLHttpRequest.instances[0];
    xhr.emitProgress('data: Hel');
    xhr.emitProgress('lo\n\n');
    xhr.emitProgress('\n');
    xhr.emitProgress('data: world\n\n');
    xhr.complete(200);

    const result = await streamPromise;

    expect(errors).toEqual([]);
    expect(events).toEqual([
      { type: 'chunk', content: 'Hello' },
      { type: 'chunk', content: 'world' },
      { type: 'done', fullResponse: 'Helloworld', sources: [], reason: 'end-without-done' },
    ]);
    expect(result).toEqual({ type: 'done', fullResponse: 'Helloworld', sources: [] });
    expect(xhr.method).toBe('POST');
    expect(xhr.url).toBe('/api/chat/stream');
    expect(xhr.headers['Content-Type']).toBe('application/json');
    expect(xhr.requestBody).toBe(JSON.stringify({ message: 'hello' }));
  });

  it('parses JSON blocks, ignores empty chunks, and respects explicit done', async () => {
    const events = [];

    const streamPromise = chatApi.streamChat({ message: 'hi' }, (event) => events.push(event), vi.fn());

    const xhr = MockXMLHttpRequest.instances[0];
    xhr.emitProgress('data: {"type":"chunk","content":"Hello"}\n\n');
    xhr.emitProgress('data: {"type":"chunk","content":"   "}\n\n');
    xhr.emitProgress('event: done\n');
    xhr.emitProgress('data: {"type":"done","fullResponse":"Hello there","sources":[{"fileName":"doc.txt"}]}\n\n');
    xhr.complete(200);

    const result = await streamPromise;

    expect(events).toEqual([
      { type: 'chunk', content: 'Hello' },
      { type: 'done', fullResponse: 'Hello there', sources: [{ fileName: 'doc.txt' }] },
    ]);
    expect(result).toEqual({ type: 'done', fullResponse: 'Hello there', sources: [{ fileName: 'doc.txt' }] });
  });

  it('supports [DONE] sentinel after JSON and plain-text chunks', async () => {
    const events = [];

    const streamPromise = chatApi.streamChat({ message: 'hi' }, (event) => events.push(event), vi.fn());

    const xhr = MockXMLHttpRequest.instances[0];
    xhr.emitProgress('data: {"type":"chunk","content":"Hi"}\n\n');
    xhr.emitProgress('data: there\n\n');
    xhr.emitProgress('data: [DONE]\n\n');
    xhr.complete(200);

    const result = await streamPromise;

    expect(events).toEqual([
      { type: 'chunk', content: 'Hi' },
      { type: 'chunk', content: 'there' },
      { type: 'done', fullResponse: 'Hithere', sources: [] },
    ]);
    expect(result).toEqual({ type: 'done', fullResponse: 'Hithere', sources: [] });
  });

  it('emits error exactly once when the server sends an error event', async () => {
    const events = [];
    const onError = vi.fn();

    const streamPromise = chatApi.streamChat({ message: 'hi' }, (event) => events.push(event), onError);

    const xhr = MockXMLHttpRequest.instances[0];
    xhr.emitProgress('data: {"type":"error","message":"boom"}\n\n');

    await expect(streamPromise).rejects.toThrow('boom');
    expect(events).toEqual([{ type: 'error', message: 'boom' }]);
    expect(onError).toHaveBeenCalledTimes(1);
    expect(onError.mock.calls[0][0].message).toBe('boom');
  });
});
