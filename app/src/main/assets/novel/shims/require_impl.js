/**
 * LNReader-compatible require() implementation.
 * Matches: pluginManager.ts _require + packages map exactly.
 *
 * This file is evaluated AFTER all runtime libraries and shims are loaded.
 * It wires the pre-evaluated globals into the packages map that require() resolves from.
 */

var __pluginId = '';  // Set by Kotlin before each plugin init

var __packages = {
    // cheerio.min.js sets globalThis.__cheerio = { load }
    'cheerio': (typeof __cheerio !== 'undefined') ? __cheerio : { load: function() { throw new Error('cheerio not loaded'); } },

    // cheerio.min.js also sets globalThis.__htmlparser2 = { Parser }
    'htmlparser2': (typeof __htmlparser2 !== 'undefined') ? __htmlparser2 : { Parser: function() { throw new Error('htmlparser2 not loaded'); } },

    // dayjs.min.js sets globalThis.__dayjs
    'dayjs': (typeof __dayjs !== 'undefined') ? __dayjs : function() { return { format: function() { return ''; } }; },

    // urlencode.min.js sets globalThis.__urlencode = { encode, decode }
    'urlencode': (typeof __urlencode !== 'undefined') ? __urlencode : { encode: encodeURIComponent, decode: decodeURIComponent },

    // From constants.js
    '@libs/novelStatus': { NovelStatus: NovelStatus },
    '@libs/filterInputs': { FilterTypes: FilterTypes },
    '@libs/defaultCover': { defaultCover: defaultCover },
    '@libs/isAbsoluteUrl': { isUrlAbsolute: isUrlAbsolute },

    // Some plugins (e.g. novelfire) import from `@/types/constants` directly instead of the
    // `@libs/*` aliases. tsc preserves these path-alias requires verbatim in the compiled JS,
    // so we have to resolve them here too. Mirror src/types/constants.ts in the LNReader repo.
    '@/types/constants': {
        NovelStatus: NovelStatus,
        defaultCover: defaultCover,
    },

    // From fetch_bridge.js
    '@libs/fetch': { fetchApi: fetchApi, fetchText: fetchText, fetchProto: fetchProto, fetchFile: fetchFile },

    // From noble-ciphers-aes.min.js (sets globalThis.__aes = { gcm })
    '@libs/aes': (typeof __aes !== 'undefined') ? __aes : { gcm: function() { throw new Error('@noble/ciphers not loaded'); } },

    // From constants.js
    '@libs/utils': { utf8ToBytes: utf8ToBytes, bytesToUtf8: bytesToUtf8 },

    // crypto-js.min.js sets globalThis.__cryptoJs
    'crypto-js': (typeof __cryptoJs !== 'undefined') ? __cryptoJs : {},

    // pako.min.js sets globalThis.__pako
    'pako': (typeof __pako !== 'undefined') ? __pako : {},

    // protobufjs.min.js sets globalThis.__protobuf
    'protobufjs': (typeof __protobuf !== 'undefined') ? __protobuf : {},
    'protobufjs/light': (typeof __protobuf !== 'undefined') ? __protobuf : {},

    // buffer.min.js sets globalThis.Buffer
    'buffer': (typeof Buffer !== 'undefined') ? { Buffer: Buffer } : {}
};

/**
 * CommonJS require() - matches LNReader's _require function.
 * Storage objects are created per-plugin (matching LNReader's isolation).
 */
function require(packageName) {
    if (packageName === '@libs/storage') {
        // localStorage/sessionStorage take no constructor args — they're per-instance dicts
        // matching LNReader's stub implementation.
        return {
            storage: new __Storage(__pluginId),
            localStorage: new __LocalStorage(),
            sessionStorage: new __SessionStorage()
        };
    }
    if (__packages[packageName] !== undefined) {
        return __packages[packageName];
    }
    // Some plugins may use 'cheerio' as { load } or import { load as parseHTML }
    // The compiled CommonJS code will call require('cheerio')
    throw new Error('Module not found: ' + packageName);
}
