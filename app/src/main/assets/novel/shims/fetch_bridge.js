/**
 * LNReader-compatible fetch bridge.
 * Matches: src/plugins/helpers/fetch.ts (app runtime)
 * + src/lib/fetch.ts (plugin repo)
 *
 * Bridges fetchApi/fetchText/fetchFile/fetchProto to Kotlin via __bridge.
 * Returns Response-like objects with .text(), .json(), .arrayBuffer() methods.
 */

var __userAgent = '';  // Set by Kotlin runtime

var __defaultHeaders = {
    'Connection': 'keep-alive',
    'Accept': '*/*',
    'Accept-Language': '*',
    'Sec-Fetch-Mode': 'cors',
    'Accept-Encoding': 'gzip, deflate',
    'Cache-Control': 'max-age=0'
};

function __makeInit(init) {
    if (!init) init = {};
    var merged = {};
    // Start with defaults
    var k;
    for (k in __defaultHeaders) {
        merged[k] = __defaultHeaders[k];
    }
    // Add User-Agent
    if (__userAgent) {
        merged['User-Agent'] = __userAgent;
    }
    // Merge custom headers (custom overrides defaults)
    if (init.headers) {
        if (typeof init.headers === 'object' && !(init.headers instanceof Array)) {
            for (k in init.headers) {
                if (init.headers[k] !== undefined) {
                    merged[k] = init.headers[k];
                }
            }
        }
    }
    init.headers = merged;
    return init;
}

function __base64ToArrayBuffer(base64) {
    if (!base64) return new ArrayBuffer(0);
    // QuickJS has atob
    try {
        var binaryString = atob(base64);
        var bytes = new Uint8Array(binaryString.length);
        for (var i = 0; i < binaryString.length; i++) {
            bytes[i] = binaryString.charCodeAt(i);
        }
        return bytes.buffer;
    } catch(e) {
        return new ArrayBuffer(0);
    }
}

function __createResponse(parsed, url) {
    var _body = parsed.body || '';
    var _bodyBase64 = parsed.bodyBase64 || '';
    var _status = parsed.status || 200;
    var _headers = parsed.headers || {};

    return {
        ok: _status >= 200 && _status < 300,
        status: _status,
        statusText: parsed.statusText || '',
        url: url || '',
        headers: {
            get: function(name) {
                if (!name) return null;
                var lower = name.toLowerCase();
                for (var k in _headers) {
                    if (k.toLowerCase() === lower) return _headers[k];
                }
                return null;
            },
            has: function(name) {
                return this.get(name) !== null;
            }
        },
        text: function() {
            return Promise.resolve(_body);
        },
        json: function() {
            try {
                return Promise.resolve(JSON.parse(_body));
            } catch(e) {
                return Promise.reject(e);
            }
        },
        arrayBuffer: function() {
            return Promise.resolve(__base64ToArrayBuffer(_bodyBase64));
        },
        blob: function() {
            var ab = __base64ToArrayBuffer(_bodyBase64);
            return Promise.resolve({
                size: ab.byteLength,
                type: (_headers['content-type'] || _headers['Content-Type'] || ''),
                arrayBuffer: function() { return Promise.resolve(ab); },
                text: function() { return Promise.resolve(_body); }
            });
        },
        clone: function() {
            return __createResponse(parsed, url);
        }
    };
}

/**
 * fetchApi - matches LNReader's fetchApi exactly.
 * Returns a Response-like object with .text(), .json(), .arrayBuffer(), .blob()
 */
async function fetchApi(url, init) {
    init = __makeInit(init);
    var raw = __bridge.fetch(url, JSON.stringify({
        method: init.method || 'GET',
        headers: init.headers,
        body: init.body ? String(init.body) : undefined
    }));
    var parsed = JSON.parse(raw);
    return __createResponse(parsed, url);
}

/**
 * fetchText - matches LNReader's fetchText.
 * Returns string, empty string on error.
 */
async function fetchText(url, init, encoding) {
    init = __makeInit(init);
    try {
        var body = __bridge.fetchText(url, JSON.stringify({
            method: init.method || 'GET',
            headers: init.headers,
            body: init.body ? String(init.body) : undefined
        }), encoding || 'utf-8');
        return body || '';
    } catch(e) {
        return '';
    }
}

/**
 * fetchFile - returns base64 encoded file content.
 */
async function fetchFile(url, init) {
    init = __makeInit(init);
    try {
        return __bridge.fetchFile(url, JSON.stringify({
            method: init.method || 'GET',
            headers: init.headers,
            body: init.body ? String(init.body) : undefined
        }));
    } catch(e) {
        return '';
    }
}

/**
 * fetchProto - protobuf support.
 * Delegates encoding/decoding to Kotlin side.
 */
async function fetchProto(protoInit, url, init) {
    init = __makeInit(init);
    try {
        var result = __bridge.fetchProto(
            JSON.stringify(protoInit),
            url,
            JSON.stringify({
                method: init.method || 'POST',
                headers: init.headers,
                body: init.body ? String(init.body) : undefined
            })
        );
        return JSON.parse(result);
    } catch(e) {
        throw e;
    }
}
