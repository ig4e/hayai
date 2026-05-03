/**
 * Async-aware setTimeout / setInterval / setImmediate implementations.
 *
 * Why this exists: the polyfill from `polyfills.min.js` expects __bridge.sleep to be
 * synchronous (the old Thread.sleep impl). Now that the bridge is async (Kotlin `delay()`),
 * the polyfill's call to `__bridge.sleep(waitMs)` returns an unawaited Promise and the
 * callback fires immediately with no delay — which silently breaks every plugin that uses
 * `await new Promise(r => setTimeout(r, ms))` for request throttling.
 *
 * This shim replaces those globals with versions that actually await the bridge sleep, so
 * `setTimeout(cb, ms)` runs the callback after ms have elapsed (off the QuickJS thread).
 * Loaded after polyfills.min.js so we override.
 */
(function () {
    var nextTimerId = 1;
    var pending = {};

    function asyncDelay(ms) {
        if (ms <= 0) return Promise.resolve();
        if (typeof __bridge !== 'undefined' && typeof __bridge.sleep === 'function') {
            return __bridge.sleep(ms);
        }
        return Promise.resolve();
    }

    globalThis.setTimeout = function (callback, delay) {
        var args = Array.prototype.slice.call(arguments, 2);
        var waitMs = Math.max(0, Number(delay) || 0);
        var entry = { cancelled: false };
        var timerId = nextTimerId++;
        pending[timerId] = entry;

        Promise.resolve()
            .then(function () { return asyncDelay(waitMs); })
            .then(function () {
                delete pending[timerId];
                if (entry.cancelled) return;
                if (typeof callback === 'function') {
                    callback.apply(null, args);
                } else if (typeof callback === 'string') {
                    (0, eval)(callback);
                }
            })
            .catch(function () {
                delete pending[timerId];
            });

        return timerId;
    };

    globalThis.clearTimeout = function (timerId) {
        var entry = pending[timerId];
        if (entry) entry.cancelled = true;
    };

    globalThis.setInterval = function (callback, delay) {
        var args = Array.prototype.slice.call(arguments, 2);
        var waitMs = Math.max(0, Number(delay) || 0);
        var entry = { cancelled: false };
        var timerId = nextTimerId++;
        pending[timerId] = entry;

        function tick() {
            if (entry.cancelled) {
                delete pending[timerId];
                return;
            }
            asyncDelay(waitMs)
                .then(function () {
                    if (entry.cancelled) {
                        delete pending[timerId];
                        return;
                    }
                    if (typeof callback === 'function') {
                        try { callback.apply(null, args); } catch (_) {}
                    }
                    tick();
                })
                .catch(function () {
                    delete pending[timerId];
                });
        }
        tick();
        return timerId;
    };

    globalThis.clearInterval = globalThis.clearTimeout;

    globalThis.setImmediate = function (callback) {
        var args = Array.prototype.slice.call(arguments, 1);
        return globalThis.setTimeout.apply(null, [callback, 0].concat(args));
    };

    globalThis.clearImmediate = globalThis.clearTimeout;
})();
