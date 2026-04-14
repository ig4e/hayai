/**
 * Bundles all JS libraries needed for LNReader plugin compatibility in QuickJS.
 * Output goes to ../app/src/main/assets/novel/runtime/
 */
import * as esbuild from 'esbuild';
import { mkdirSync } from 'fs';

const outDir = '../app/src/main/assets/novel/runtime';
mkdirSync(outDir, { recursive: true });

// Bundle cheerio (exports: { load }) + htmlparser2 (exports: { Parser })
// cheerio includes htmlparser2 internally. Use platform 'node' for CommonJS compat,
// but mark node builtins external since QuickJS won't have them.
await esbuild.build({
  stdin: {
    contents: `
      const { load } = require('cheerio');
      const { Parser } = require('htmlparser2');
      globalThis.__cheerio = { load };
      globalThis.__htmlparser2 = { Parser };
    `,
    resolveDir: './node_modules',
  },
  bundle: true,
  minify: true,
  format: 'iife',
  platform: 'node',
  target: 'es2020',
  outfile: `${outDir}/cheerio.min.js`,
  define: {
    'process.env.NODE_ENV': '"production"',
    'Buffer': 'undefined',
  },
  external: ['buffer', 'stream', 'string_decoder', 'events'],
});
console.log('cheerio + htmlparser2 bundled');

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

console.log('\nAll libraries bundled to ' + outDir);
