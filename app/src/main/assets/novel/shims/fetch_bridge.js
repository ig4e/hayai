/**
 * LNReader-compatible fetch bridge.
 * Bridges fetchApi/fetchText/fetchFile/fetchProto to Kotlin via __bridge.
 *
 * Returns Response objects from fetch-globals.min.js (real class with
 * .text/.json/.arrayBuffer/.blob/.clone, instanceof Response works).
 *
 * Body handling: serializes FormData/Blob/ArrayBuffer/URLSearchParams to a
 * `bodyBase64` field; the Kotlin bridge decodes that into a binary OkHttp
 * RequestBody. Text bodies are passed through as `body`.
 *
 * Response decoding: passes the Kotlin-decoded text body straight through to
 * Response (fast text() path) and the raw base64 alongside it (lazy decode for
 * arrayBuffer()/blob() callers — avoids round-trip when only text is wanted).
 */

var __userAgent = '';  // Set by Kotlin runtime

// Mirror LNReader's makeInit defaults from src/lib/fetch.ts so plugin behavior matches the
// reference exactly. Notable: we don't add 'Cache-Control: max-age=0' (Hayai used to, which
// caused unnecessary origin hits on plugins that benefit from CDN caching). Accept-Encoding
// is allowed in the default but stripped by the Kotlin bridge — OkHttp negotiates compression
// itself, so leaking gzip framing into the JS layer would break body decoding.
var __defaultHeaders = {
    'Connection': 'keep-alive',
    'Accept': '*/*',
    'Accept-Language': '*',
    'Sec-Fetch-Mode': 'cors',
    'Accept-Encoding': 'gzip, deflate'
};

function __headersToObject(h) {
    if (!h) return {};
    if (typeof Headers !== 'undefined' && h instanceof Headers) {
        var obj = {};
        h.forEach(function (v, k) { obj[k] = v; });
        return obj;
    }
    if (Array.isArray(h)) {
        var arr = {};
        for (var i = 0; i < h.length; i++) {
            if (h[i] && h[i].length === 2) arr[h[i][0]] = h[i][1];
        }
        return arr;
    }
    if (typeof h === 'object') return h;
    return {};
}

function __hasHeader(headers, name) {
    var lower = name.toLowerCase();
    for (var k in headers) if (k.toLowerCase() === lower) return true;
    return false;
}

function __makeInit(init) {
    if (!init) init = {};
    var merged = {};
    var k;
    for (k in __defaultHeaders) merged[k] = __defaultHeaders[k];
    if (__userAgent) merged['User-Agent'] = __userAgent;
    var custom = __headersToObject(init.headers);
    for (k in custom) {
        if (custom[k] !== undefined) merged[k] = custom[k];
    }
    init.headers = merged;
    return init;
}

// Buffer-backed base64 (loaded before this file). Falls back to btoa if not.
var __hasBuffer = typeof Buffer !== 'undefined' && typeof Buffer.from === 'function';

function __bytesToBase64(u8) {
    if (!u8 || u8.length === 0) return '';
    if (__hasBuffer) return Buffer.from(u8.buffer, u8.byteOffset, u8.byteLength).toString('base64');
    var bin = '';
    var chunk = 0x8000;
    for (var i = 0; i < u8.length; i += chunk) {
        bin += String.fromCharCode.apply(null, u8.subarray(i, Math.min(u8.length, i + chunk)));
    }
    return btoa(bin);
}

/**
 * Serialize an init.body to either { body: string } or { bodyBase64: string }.
 * Sets Content-Type on `headers` if missing, when the body type implies one.
 */
function __serializeBody(body, headers) {
    if (body == null) return {};
    if (typeof body === 'string') return { body: body };

    if (typeof FormData !== 'undefined' && body instanceof FormData) {
        var ser = body.__serialize();
        if (!__hasHeader(headers, 'Content-Type')) headers['Content-Type'] = ser.contentType;
        return { bodyBase64: __bytesToBase64(ser.bytes) };
    }
    if (typeof URLSearchParams !== 'undefined' && body instanceof URLSearchParams) {
        if (!__hasHeader(headers, 'Content-Type')) {
            headers['Content-Type'] = 'application/x-www-form-urlencoded;charset=UTF-8';
        }
        return { body: body.toString() };
    }
    if (typeof Blob !== 'undefined' && body instanceof Blob) {
        if (!__hasHeader(headers, 'Content-Type') && body.type) headers['Content-Type'] = body.type;
        return { bodyBase64: __bytesToBase64(body.__bytes()) };
    }
    if (body instanceof ArrayBuffer) {
        return { bodyBase64: __bytesToBase64(new Uint8Array(body)) };
    }
    if (ArrayBuffer.isView(body)) {
        return { bodyBase64: __bytesToBase64(new Uint8Array(body.buffer, body.byteOffset, body.byteLength)) };
    }
    return { body: String(body) };
}

function __requestPayload(init) {
    var ser = __serializeBody(init.body, init.headers);
    return JSON.stringify({
        method: init.method || 'GET',
        headers: init.headers,
        body: ser.body,
        bodyBase64: ser.bodyBase64,
    });
}

function __responseFromParsed(parsed, url) {
    // Construct Response with the text body for the fast .text() path,
    // plus the raw base64 (lazy-decoded only by .arrayBuffer()/.blob()).
    return new Response(parsed.body || '', {
        status: parsed.status || 200,
        statusText: parsed.statusText || '',
        headers: parsed.headers || {},
        url: url || '',
        _bodyBase64: parsed.bodyBase64 || '',
    });
}

/**
 * fetchApi - matches LNReader's fetchApi exactly. Returns a Response object.
 *
 * The __bridge.fetch* methods are bound on the Kotlin side as `asyncFunction`s, so they
 * return Promises here. We must await them — calling without await would resolve to a
 * Promise object instead of the JSON string and break every plugin's parsing.
 */
async function fetchApi(url, init) {
    init = __makeInit(init);
    var raw = await __bridge.fetch(url, __requestPayload(init));
    var parsed = JSON.parse(raw);
    return __responseFromParsed(parsed, url);
}

/**
 * fetchText - returns string, empty string on error. `encoding` is honored
 * Kotlin-side via Charset.forName.
 */
async function fetchText(url, init, encoding) {
    init = __makeInit(init);
    try {
        var body = await __bridge.fetchText(url, __requestPayload(init), encoding || 'utf-8');
        return body || '';
    } catch (e) {
        return '';
    }
}

/**
 * fetchFile - returns base64-encoded file content.
 */
async function fetchFile(url, init) {
    init = __makeInit(init);
    try {
        return await __bridge.fetchFile(url, __requestPayload(init));
    } catch (e) {
        return '';
    }
}

/**
 * fetchProto - gRPC-Web request via protobufjs (loaded as `__protobuf`). Mirrors LNReader's
 * src/lib/fetch.ts:fetchProto exactly: parse the .proto, encode the request, prepend the
 * 5-byte gRPC-Web length-prefix frame, POST, then strip the response frame and decode.
 *
 * Doing this in JS (not via __bridge) means we can use the same protobufjs that LNReader uses,
 * keep the contract identical for plugin authors, and avoid having to round-trip arbitrary
 * proto schemas through Kotlin.
 */
async function fetchProto(protoInit, url, init) {
    if (typeof __protobuf === 'undefined' || !__protobuf || !__protobuf.parse) {
        throw new Error('fetchProto called but protobufjs is not loaded');
    }

    var protoRoot = __protobuf.parse(protoInit.proto).root;
    var RequestMessage = protoRoot.lookupType(protoInit.requestType);
    var ResponseMessage = protoRoot.lookupType(protoInit.responseType);

    var verifyError = RequestMessage.verify(protoInit.requestData || {});
    if (verifyError) throw new Error('Invalid Proto: ' + verifyError);

    var encodedRequest = RequestMessage.encode(
        RequestMessage.create(protoInit.requestData || {}),
    ).finish();

    // 5-byte gRPC-Web frame header: [compressed?:1byte] + [length:4 bytes big-endian]
    var len = encodedRequest.length;
    var framed = new Uint8Array(5 + len);
    framed[0] = 0;
    framed[1] = (len >>> 24) & 0xff;
    framed[2] = (len >>> 16) & 0xff;
    framed[3] = (len >>> 8) & 0xff;
    framed[4] = len & 0xff;
    framed.set(encodedRequest, 5);

    init = __makeInit(init);
    init.method = 'POST';
    init.body = framed;
    if (!__hasHeader(init.headers, 'Content-Type')) {
        init.headers['Content-Type'] = 'application/grpc-web+proto';
    }

    var response = await fetchApi(url, init);
    var buf = await response.arrayBuffer();
    var bytes = new Uint8Array(buf);

    // Strip the same 5-byte frame from the response, then decode the protobuf payload.
    var payload = bytes.length > 5 ? bytes.subarray(5) : bytes;
    var decoded = ResponseMessage.decode(payload);
    return ResponseMessage.toObject(decoded, { defaults: true, longs: String, enums: String });
}

// Make fetch helpers visible to fetch-globals' lazy fetch() wrapper.
globalThis.fetchApi = fetchApi;
globalThis.fetchText = fetchText;
globalThis.fetchFile = fetchFile;
globalThis.fetchProto = fetchProto;
