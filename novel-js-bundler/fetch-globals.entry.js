/**
 * Web fetch API polyfills for QuickJS.
 *
 * Provides as globals:
 *   Headers, Request, Response, Blob, File, FormData,
 *   AbortController, AbortSignal, DOMException, fetch, performance,
 *   structuredClone.
 *
 * Loads after buffer.min.js (uses Buffer for fast base64) and before
 * fetch_bridge.js (which defines fetchApi() — global fetch resolves it lazily).
 */
const G = globalThis;

// ---- Fast base64 helpers (Buffer-backed when available) -------------------

const _hasBuffer = typeof G.Buffer !== 'undefined' && typeof G.Buffer.from === 'function';

function _bytesToBase64(u8) {
  if (!u8 || u8.length === 0) return '';
  if (_hasBuffer) return G.Buffer.from(u8.buffer, u8.byteOffset, u8.byteLength).toString('base64');
  let bin = '';
  const chunk = 0x8000;
  for (let i = 0; i < u8.length; i += chunk) {
    bin += String.fromCharCode.apply(null, u8.subarray(i, Math.min(u8.length, i + chunk)));
  }
  return btoa(bin);
}

function _base64ToBytes(b64) {
  if (!b64) return new Uint8Array(0);
  if (_hasBuffer) {
    const b = G.Buffer.from(b64, 'base64');
    return new Uint8Array(b.buffer, b.byteOffset, b.byteLength);
  }
  const bin = atob(b64);
  const out = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) out[i] = bin.charCodeAt(i);
  return out;
}

function _toBytes(part) {
  if (part == null) return new Uint8Array(0);
  if (part instanceof Uint8Array) return part;
  if (part instanceof ArrayBuffer) return new Uint8Array(part);
  if (ArrayBuffer.isView(part)) return new Uint8Array(part.buffer, part.byteOffset, part.byteLength);
  if (G.Blob && part instanceof G.Blob) return part.__bytes();
  if (typeof part === 'string') return new TextEncoder().encode(part);
  return new TextEncoder().encode(String(part));
}

function _concat(arrays) {
  let total = 0;
  for (const a of arrays) total += a.length;
  const out = new Uint8Array(total);
  let off = 0;
  for (const a of arrays) { out.set(a, off); off += a.length; }
  return out;
}

// ---- DOMException ---------------------------------------------------------

class DOMException extends Error {
  constructor(message, name) {
    super(message);
    this.name = name || 'Error';
    this.code = 0;
  }
}

// ---- Headers --------------------------------------------------------------

class Headers {
  constructor(init) {
    this._map = new Map();
    if (!init) return;
    if (init instanceof Headers) {
      init.forEach((v, k) => this.append(k, v));
    } else if (Array.isArray(init)) {
      for (const pair of init) {
        if (pair && pair.length === 2) this.append(pair[0], pair[1]);
      }
    } else if (typeof init === 'object') {
      for (const k of Object.keys(init)) this.append(k, init[k]);
    }
  }
  append(name, value) {
    const k = String(name).toLowerCase();
    const existing = this._map.get(k);
    this._map.set(k, existing ? existing + ', ' + String(value) : String(value));
  }
  set(name, value) { this._map.set(String(name).toLowerCase(), String(value)); }
  get(name) { const v = this._map.get(String(name).toLowerCase()); return v == null ? null : v; }
  has(name) { return this._map.has(String(name).toLowerCase()); }
  delete(name) { this._map.delete(String(name).toLowerCase()); }
  forEach(cb, thisArg) { for (const [k, v] of this._map) cb.call(thisArg, v, k, this); }
  *entries() { yield* this._map; }
  *keys() { yield* this._map.keys(); }
  *values() { yield* this._map.values(); }
  [Symbol.iterator]() { return this.entries(); }
}

// ---- Blob / File ----------------------------------------------------------

class Blob {
  constructor(parts, options) {
    const arr = Array.isArray(parts) ? parts : [];
    this._cachedBytes = _concat(arr.map(_toBytes));
    this.type = (options && options.type) ? String(options.type) : '';
    this.size = this._cachedBytes.length;
  }
  __bytes() { return this._cachedBytes; }
  arrayBuffer() {
    const b = this._cachedBytes;
    return Promise.resolve(b.buffer.slice(b.byteOffset, b.byteOffset + b.byteLength));
  }
  text() { return Promise.resolve(new TextDecoder().decode(this._cachedBytes)); }
  slice(start, end, contentType) {
    return new Blob(
      [this._cachedBytes.slice(start || 0, end == null ? this._cachedBytes.length : end)],
      { type: contentType || this.type }
    );
  }
  stream() { throw new DOMException('Blob.stream() not supported', 'NotSupportedError'); }
}

class File extends Blob {
  constructor(parts, name, options) {
    super(parts, options);
    this.name = String(name);
    this.lastModified = (options && options.lastModified) || Date.now();
  }
}

// ---- FormData (with multipart serialization) ------------------------------

const _CRLF = '\r\n';

class FormData {
  constructor() { this._entries = []; }
  _normalize(value, filename) {
    if (value instanceof Blob) {
      return [value, filename || (value instanceof File ? value.name : 'blob')];
    }
    return [String(value), undefined];
  }
  append(name, value, filename) {
    const [v, f] = this._normalize(value, filename);
    this._entries.push([String(name), v, f]);
  }
  set(name, value, filename) {
    this.delete(name);
    this.append(name, value, filename);
  }
  get(name) { for (const e of this._entries) if (e[0] === name) return e[1]; return null; }
  getAll(name) { return this._entries.filter(e => e[0] === name).map(e => e[1]); }
  has(name) { return this._entries.some(e => e[0] === name); }
  delete(name) { this._entries = this._entries.filter(e => e[0] !== name); }
  forEach(cb, thisArg) { for (const e of this._entries) cb.call(thisArg, e[1], e[0], this); }
  *entries() { for (const e of this._entries) yield [e[0], e[1]]; }
  *keys() { for (const e of this._entries) yield e[0]; }
  *values() { for (const e of this._entries) yield e[1]; }
  [Symbol.iterator]() { return this.entries(); }

  __serialize() {
    const boundary = '----HayaiFormBoundary' +
      Math.random().toString(16).slice(2) + Date.now().toString(16);
    const enc = new TextEncoder();
    const chunks = [];
    for (const [name, value, filename] of this._entries) {
      chunks.push(enc.encode('--' + boundary + _CRLF));
      if (value instanceof Blob) {
        const fname = filename || (value instanceof File ? value.name : 'blob');
        const ctype = value.type || 'application/octet-stream';
        chunks.push(enc.encode(
          'Content-Disposition: form-data; name="' + _qpEscape(name) + '"; filename="' + _qpEscape(fname) + '"' + _CRLF +
          'Content-Type: ' + ctype + _CRLF + _CRLF
        ));
        chunks.push(value.__bytes());
        chunks.push(enc.encode(_CRLF));
      } else {
        chunks.push(enc.encode(
          'Content-Disposition: form-data; name="' + _qpEscape(name) + '"' + _CRLF + _CRLF +
          String(value) + _CRLF
        ));
      }
    }
    chunks.push(enc.encode('--' + boundary + '--' + _CRLF));
    return { bytes: _concat(chunks), contentType: 'multipart/form-data; boundary=' + boundary };
  }
}

// Per RFC 7578 — escape only quote, CR, LF in field names/filenames.
function _qpEscape(s) {
  return String(s).replace(/\r\n|\r|\n/g, '%0A').replace(/"/g, '%22');
}

// ---- Response -------------------------------------------------------------
// Supports dual-body init for the hayai bridge: pass a string body for the
// fast text path; pass `_bodyBase64` in init for lazy binary decode (only
// realized when arrayBuffer/blob is actually called).

class Response {
  constructor(body, init) {
    init = init || {};
    this.status = init.status != null ? init.status : 200;
    this.statusText = init.statusText || '';
    this.ok = this.status >= 200 && this.status < 300;
    this.url = init.url || '';
    this.redirected = !!init.redirected;
    this.type = 'basic';
    this.headers = init.headers instanceof Headers ? init.headers : new Headers(init.headers);

    this._text = null;
    this._bytes = null;
    this._base64 = init._bodyBase64 || null;

    if (body == null) {
      this._text = '';
    } else if (typeof body === 'string') {
      this._text = body;
    } else if (body instanceof Blob) {
      this._bytes = body.__bytes();
    } else if (body instanceof Uint8Array) {
      this._bytes = body;
    } else if (body instanceof ArrayBuffer) {
      this._bytes = new Uint8Array(body);
    } else if (ArrayBuffer.isView(body)) {
      this._bytes = new Uint8Array(body.buffer, body.byteOffset, body.byteLength);
    } else {
      this._text = String(body);
    }
    this.bodyUsed = false;
  }
  _materializeBytes() {
    if (this._bytes) return this._bytes;
    if (this._base64) { this._bytes = _base64ToBytes(this._base64); return this._bytes; }
    if (this._text != null) { this._bytes = new TextEncoder().encode(this._text); return this._bytes; }
    return new Uint8Array(0);
  }
  text() {
    this.bodyUsed = true;
    if (this._text != null) return Promise.resolve(this._text);
    return Promise.resolve(new TextDecoder().decode(this._materializeBytes()));
  }
  json() { return this.text().then(JSON.parse); }
  arrayBuffer() {
    this.bodyUsed = true;
    const b = this._materializeBytes();
    return Promise.resolve(b.buffer.slice(b.byteOffset, b.byteOffset + b.byteLength));
  }
  blob() {
    this.bodyUsed = true;
    return Promise.resolve(new Blob([this._materializeBytes()], {
      type: this.headers.get('content-type') || '',
    }));
  }
  formData() {
    return Promise.reject(new DOMException('Response.formData() not supported', 'NotSupportedError'));
  }
  clone() {
    const init = {
      status: this.status,
      statusText: this.statusText,
      headers: new Headers(this.headers),
      url: this.url,
      _bodyBase64: this._base64,
    };
    if (this._text != null && this._bytes == null) return new Response(this._text, init);
    if (this._bytes != null) return new Response(this._bytes.slice(), init);
    return new Response(null, init);
  }
}

// ---- Request --------------------------------------------------------------

class Request {
  constructor(input, init) {
    init = init || {};
    const inputIsRequest = input && typeof input === 'object' && 'url' in input;
    this.url = typeof input === 'string' ? input : (input && input.url) || '';
    this.method = (init.method || (inputIsRequest && input.method) || 'GET').toUpperCase();
    const baseHeaders = init.headers != null ? init.headers : (inputIsRequest ? input.headers : null);
    this.headers = baseHeaders instanceof Headers ? baseHeaders : new Headers(baseHeaders);
    this.body = init.body != null ? init.body : (inputIsRequest ? input.body : null);
    this.credentials = init.credentials || 'same-origin';
    this.mode = init.mode || 'cors';
    this.cache = init.cache || 'default';
    this.redirect = init.redirect || 'follow';
    this.referrer = init.referrer || '';
    this.signal = init.signal || (inputIsRequest ? input.signal : null) || null;
  }
  clone() { return new Request(this.url, this); }
}

// ---- AbortController / AbortSignal ----------------------------------------

class AbortSignal {
  constructor() {
    this.aborted = false;
    this.reason = undefined;
    this._listeners = [];
    this.onabort = null;
  }
  addEventListener(type, listener) {
    if (type === 'abort' && typeof listener === 'function') this._listeners.push(listener);
  }
  removeEventListener(type, listener) {
    if (type !== 'abort') return;
    const i = this._listeners.indexOf(listener);
    if (i >= 0) this._listeners.splice(i, 1);
  }
  dispatchEvent(event) {
    if (event && event.type === 'abort') {
      if (typeof this.onabort === 'function') {
        try { this.onabort(event); } catch (_) {}
      }
      for (const l of this._listeners) {
        try { l(event); } catch (_) {}
      }
    }
    return true;
  }
  throwIfAborted() { if (this.aborted) throw this.reason || new DOMException('aborted', 'AbortError'); }
  static abort(reason) {
    const s = new AbortSignal();
    s.aborted = true;
    s.reason = reason !== undefined ? reason : new DOMException('aborted', 'AbortError');
    return s;
  }
  static timeout() {
    // Real timeout would require an event loop; return a never-aborting signal.
    return new AbortSignal();
  }
}

class AbortController {
  constructor() { this.signal = new AbortSignal(); }
  abort(reason) {
    if (this.signal.aborted) return;
    this.signal.aborted = true;
    this.signal.reason = reason !== undefined ? reason : new DOMException('aborted', 'AbortError');
    this.signal.dispatchEvent({ type: 'abort' });
  }
}

// ---- Global fetch (lazy-resolves fetch_bridge.js) -------------------------

function fetch(input, init) {
  const f = G.fetchApi;
  if (typeof f !== 'function') {
    return Promise.reject(new Error('fetch_bridge not loaded yet'));
  }
  if (input instanceof Request) {
    const merged = init || {};
    if (merged.method == null) merged.method = input.method;
    if (merged.headers == null) merged.headers = input.headers;
    if (merged.body == null) merged.body = input.body;
    return f(input.url, merged);
  }
  return f(input, init);
}

// ---- performance.now ------------------------------------------------------

const _epoch = Date.now();
const _performance = {
  now() { return Date.now() - _epoch; },
  timeOrigin: _epoch,
};

// ---- structuredClone (JSON-roundtrip fallback) ----------------------------

function structuredClone(value) {
  if (value === null || typeof value !== 'object') return value;
  return JSON.parse(JSON.stringify(value));
}

// ---- Install on globalThis -------------------------------------------------

G.DOMException = DOMException;
G.Headers = Headers;
G.Blob = Blob;
G.File = File;
G.FormData = FormData;
G.Response = Response;
G.Request = Request;
G.AbortController = AbortController;
G.AbortSignal = AbortSignal;
G.fetch = fetch;
if (!G.performance) G.performance = _performance;
if (!G.structuredClone) G.structuredClone = structuredClone;
