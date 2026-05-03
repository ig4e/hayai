// Empty stub for node-builtin aliases used by bundle.mjs. Cheerio (and possibly other
// browser-bundled libs) reference modules like `node:stream` defensively but don't actually
// invoke their methods at runtime. Aliasing those imports to this empty module keeps the
// resulting bundle self-contained — no runtime require() fall-through to QuickJS that has
// no module resolver for node-core modules.
module.exports = {};
