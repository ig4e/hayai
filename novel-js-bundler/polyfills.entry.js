import packageJson from 'react-native-url-polyfill/package.json';
import {
  URL as WhatwgURL,
  URLSearchParams as WhatwgURLSearchParams,
} from 'whatwg-url-without-unicode';

const globalObject = globalThis;

globalObject.REACT_NATIVE_URL_POLYFILL =
  `${packageJson.name}@${packageJson.version}`;

globalObject.URL = WhatwgURL;
globalObject.URLSearchParams = WhatwgURLSearchParams;

if (typeof globalObject.URL.canParse !== 'function') {
  globalObject.URL.canParse = (url, base) => {
    try {
      // eslint-disable-next-line no-new
      new globalObject.URL(url, base);
      return true;
    } catch (_error) {
      return false;
    }
  };
}

if (typeof globalObject.global === 'undefined') {
  globalObject.global = globalObject;
}

if (typeof globalObject.window === 'undefined') {
  globalObject.window = globalObject;
}

if (typeof globalObject.self === 'undefined') {
  globalObject.self = globalObject;
}

if (typeof globalObject.queueMicrotask === 'undefined') {
  globalObject.queueMicrotask = callback => Promise.resolve().then(callback);
}

if (typeof globalObject.setTimeout === 'undefined') {
  let nextTimerId = 1;

  globalObject.setTimeout = (callback, delay, ...args) => {
    const timerId = nextTimerId++;
    const waitMs = Math.max(0, Number(delay) || 0);

    if (typeof globalObject.__bridge?.sleep === 'function') {
      globalObject.__bridge.sleep(waitMs);
    }

    if (typeof callback === 'function') {
      callback(...args);
    } else if (typeof callback === 'string') {
      (0, eval)(callback);
    }

    return timerId;
  };

  globalObject.clearTimeout = () => {};
}

if (typeof globalObject.setInterval === 'undefined') {
  globalObject.setInterval = globalObject.setTimeout;
}

if (typeof globalObject.clearInterval === 'undefined') {
  globalObject.clearInterval = globalObject.clearTimeout;
}

if (typeof globalObject.setImmediate === 'undefined') {
  globalObject.setImmediate = callback => globalObject.setTimeout(callback, 0);
}

if (typeof globalObject.clearImmediate === 'undefined') {
  globalObject.clearImmediate = globalObject.clearTimeout;
}
