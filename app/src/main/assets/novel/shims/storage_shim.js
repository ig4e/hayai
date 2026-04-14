/**
 * LNReader-compatible Storage, LocalStorage, SessionStorage.
 * Matches: src/plugins/helpers/storage.ts (app runtime, MMKV-backed)
 * Bridge calls go to Kotlin SharedPreferences via __bridge.
 */

function __Storage(pluginId) {
    this._pluginId = pluginId;
}

__Storage.prototype.set = function(key, value, expires) {
    var expiresMs = 0;
    if (expires instanceof Date) {
        expiresMs = expires.getTime();
    } else if (typeof expires === 'number') {
        expiresMs = expires;
    }
    __bridge.storageSet(this._pluginId, key, JSON.stringify({
        created: new Date().toISOString(),
        value: value,
        expires: expiresMs || undefined
    }));
};

__Storage.prototype.get = function(key, raw) {
    var result = __bridge.storageGet(this._pluginId, key);
    if (result === null || result === undefined || result === '') return undefined;
    try {
        var item = JSON.parse(result);
        if (item.expires && Date.now() > item.expires) {
            this['delete'](key);
            return undefined;
        }
        if (raw) {
            if (item.expires) {
                item.expires = new Date(item.expires).getTime();
            }
            return item;
        }
        return item.value;
    } catch(e) {
        return undefined;
    }
};

__Storage.prototype['delete'] = function(key) {
    __bridge.storageDelete(this._pluginId, key);
};

__Storage.prototype.clearAll = function() {
    __bridge.storageClearAll(this._pluginId);
};

__Storage.prototype.getAllKeys = function() {
    var result = __bridge.storageGetAllKeys(this._pluginId);
    if (!result) return [];
    try {
        return JSON.parse(result);
    } catch(e) {
        return [];
    }
};

function __LocalStorage(pluginId) {
    this._pluginId = pluginId;
}

__LocalStorage.prototype.get = function() {
    var result = __bridge.storageGet(this._pluginId, '__webview_localStorage');
    if (!result) return undefined;
    try {
        var item = JSON.parse(result);
        return item.value !== undefined ? item.value : item;
    } catch(e) {
        return undefined;
    }
};

function __SessionStorage(pluginId) {
    this._pluginId = pluginId;
}

__SessionStorage.prototype.get = function() {
    var result = __bridge.storageGet(this._pluginId, '__webview_sessionStorage');
    if (!result) return undefined;
    try {
        var item = JSON.parse(result);
        return item.value !== undefined ? item.value : item;
    } catch(e) {
        return undefined;
    }
};
