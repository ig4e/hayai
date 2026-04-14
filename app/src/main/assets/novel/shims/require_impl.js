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

    // From fetch_bridge.js
    '@libs/fetch': { fetchApi: fetchApi, fetchText: fetchText, fetchProto: fetchProto, fetchFile: fetchFile },

    // From noble-ciphers-aes.min.js (sets globalThis.__aes = { gcm })
    '@libs/aes': (typeof __aes !== 'undefined') ? __aes : { gcm: function() { throw new Error('@noble/ciphers not loaded'); } },

    // From constants.js
    '@libs/utils': { utf8ToBytes: utf8ToBytes, bytesToUtf8: bytesToUtf8 }
};

/**
 * CommonJS require() - matches LNReader's _require function.
 * Storage objects are created per-plugin (matching LNReader's isolation).
 */
function require(packageName) {
    if (packageName === '@libs/storage') {
        return {
            storage: new __Storage(__pluginId),
            localStorage: new __LocalStorage(__pluginId),
            sessionStorage: new __SessionStorage(__pluginId)
        };
    }
    if (__packages[packageName] !== undefined) {
        return __packages[packageName];
    }
    // Some plugins may use 'cheerio' as { load } or import { load as parseHTML }
    // The compiled CommonJS code will call require('cheerio')
    throw new Error('Module not found: ' + packageName);
}
