/**
 * Bundles all JS libraries needed for LNReader plugin compatibility in QuickJS.
 * Output goes to ../app/src/main/assets/novel/runtime/
 */
import * as esbuild from 'esbuild';
import { mkdirSync, statSync } from 'fs';

const outDir = '../app/src/main/assets/novel/runtime';
mkdirSync(outDir, { recursive: true });

await esbuild.build({
  entryPoints: ['./polyfills.entry.js'],
  bundle: true,
  minify: true,
  format: 'iife',
  platform: 'browser',
  target: 'es2020',
  outfile: `${outDir}/polyfills.min.js`,
});
console.log('polyfills bundled');

// Buffer polyfill — the npm `buffer` package (browserify-style).
await esbuild.build({
  entryPoints: ['./buffer.entry.js'],
  bundle: true,
  minify: true,
  format: 'iife',
  platform: 'browser',
  target: 'es2020',
  outfile: `${outDir}/buffer.min.js`,
});
console.log('buffer bundled');

// fetch globals: Headers, Request, Response, Blob, File, FormData,
// AbortController, fetch.
await esbuild.build({
  entryPoints: ['./fetch-globals.entry.js'],
  bundle: true,
  minify: true,
  format: 'iife',
  platform: 'browser',
  target: 'es2020',
  outfile: `${outDir}/fetch-globals.min.js`,
});
console.log('fetch-globals bundled');

// Full TextDecoder/TextEncoder including legacy encodings.
await esbuild.build({
  entryPoints: ['./text-encoding.entry.js'],
  bundle: true,
  minify: true,
  format: 'iife',
  platform: 'browser',
  target: 'es2020',
  outfile: `${outDir}/text-encoding.min.js`,
});
console.log('text-encoding bundled');

// Standalone htmlparser2 (cheerio's bundle no longer publishes __htmlparser2).
await esbuild.build({
  entryPoints: ['./htmlparser2.entry.js'],
  bundle: true,
  minify: true,
  format: 'iife',
  platform: 'browser',
  target: 'es2020',
  outfile: `${outDir}/htmlparser2.min.js`,
});
console.log('htmlparser2 bundled');

// Bundle cheerio (exports: { load }). htmlparser2 stays internal — only `load`
// is published; the standalone htmlparser2 bundle owns __htmlparser2.
//
// platform: 'browser' picks cheerio's `browser` package.json entry, which avoids the
// node:stream / node:string_decoder / node:events imports that the node entry pulls in
// (and which our QuickJS runtime has no module resolver for). Combined with the
// inject-empty stubs below for any remaining node-builtins the browser bundle still
// references, cheerio loads cleanly without falling through to require() at runtime.
await esbuild.build({
  stdin: {
    contents: `
      const { load } = require('cheerio');
      globalThis.__cheerio = { load };
    `,
    resolveDir: './node_modules',
  },
  bundle: true,
  minify: true,
  format: 'iife',
  platform: 'browser',
  target: 'es2020',
  outfile: `${outDir}/cheerio.min.js`,
  define: {
    'process.env.NODE_ENV': '"production"',
    // Don't replace `Buffer` with undefined — cheerio 1.x calls Buffer.from() at runtime
    // and we load buffer.min.js (sets globalThis.Buffer) BEFORE cheerio.min.js, so the
    // global is available. Replacing it produced `undefined.from()` TypeErrors during init.
  },
  // Map any node-builtin imports the browser entry might still reference to empty modules
  // bundled inline, so esbuild produces a zero-runtime-require output.
  alias: {
    'stream': './empty.js',
    'string_decoder': './empty.js',
    'events': './empty.js',
    'buffer': './empty.js',
    'node:stream': './empty.js',
    'node:string_decoder': './empty.js',
    'node:events': './empty.js',
    'node:buffer': './empty.js',
  },
});
console.log('cheerio bundled');

// Bundle dayjs
await esbuild.build({
  stdin: {
    contents: `
      const dayjs = require('dayjs');
      globalThis.__dayjs = dayjs;
    `,
    resolveDir: './node_modules',
  },
  bundle: true,
  minify: true,
  format: 'iife',
  platform: 'node',
  target: 'es2020',
  outfile: `${outDir}/dayjs.min.js`,
});
console.log('dayjs bundled');

// Bundle urlencode
await esbuild.build({
  stdin: {
    contents: `
      const urlencode = require('urlencode');
      globalThis.__urlencode = { encode: urlencode.encode || urlencode, decode: urlencode.decode };
    `,
    resolveDir: './node_modules',
  },
  bundle: true,
  minify: true,
  format: 'iife',
  platform: 'node',
  target: 'es2020',
  outfile: `${outDir}/urlencode.min.js`,
  external: ['iconv-lite'],
});
console.log('urlencode bundled');

// Bundle @noble/ciphers AES-GCM
await esbuild.build({
  stdin: {
    contents: `
      import { gcm } from '@noble/ciphers/aes.js';
      globalThis.__aes = { gcm };
    `,
    resolveDir: './node_modules',
  },
  bundle: true,
  minify: true,
  format: 'iife',
  platform: 'node',
  target: 'es2020',
  outfile: `${outDir}/noble-ciphers-aes.min.js`,
});
console.log('@noble/ciphers AES bundled');

// Bundle crypto-js
await esbuild.build({
  entryPoints: ['./crypto-js.entry.js'],
  bundle: true,
  minify: true,
  format: 'iife',
  platform: 'browser',
  target: 'es2020',
  outfile: `${outDir}/crypto-js.min.js`,
});
console.log('crypto-js bundled');

// Bundle pako
await esbuild.build({
  entryPoints: ['./pako.entry.js'],
  bundle: true,
  minify: true,
  format: 'iife',
  platform: 'browser',
  target: 'es2020',
  outfile: `${outDir}/pako.min.js`,
});
console.log('pako bundled');

// Bundle protobufjs (light build)
await esbuild.build({
  entryPoints: ['./protobufjs.entry.js'],
  bundle: true,
  minify: true,
  format: 'iife',
  platform: 'browser',
  target: 'es2020',
  outfile: `${outDir}/protobufjs.min.js`,
});
console.log('protobufjs bundled');

// Size summary
const files = [
  'polyfills.min.js',
  'buffer.min.js',
  'fetch-globals.min.js',
  'text-encoding.min.js',
  'htmlparser2.min.js',
  'cheerio.min.js',
  'dayjs.min.js',
  'urlencode.min.js',
  'noble-ciphers-aes.min.js',
  'crypto-js.min.js',
  'pako.min.js',
  'protobufjs.min.js',
];
let total = 0;
console.log('\nBundle sizes:');
for (const f of files) {
  const s = statSync(`${outDir}/${f}`).size;
  total += s;
  console.log(`  ${(s / 1024).toFixed(1).padStart(7)} KB  ${f}`);
}
console.log(`  ${'-'.repeat(7)}`);
console.log(`  ${(total / 1024).toFixed(1).padStart(7)} KB  total`);
