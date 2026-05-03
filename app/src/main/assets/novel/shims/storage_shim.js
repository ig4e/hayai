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

// LocalStorage / SessionStorage mirror LNReader's src/lib/storage.ts: a per-instance
// in-memory object dict with a single get() returning the dict. LNReader keeps these as
// stubs at the plugin layer because the real localStorage only exists in the WebView
// runtime (where the browser provides it natively). Matching that surface keeps semantic
// parity — the previous Hayai impl returned a single SharedPreferences blob, which
// silently broke any plugin that tried to read individual keys off the returned object.
function __LocalStorage() {
    this._db = {};
}

__LocalStorage.prototype.get = function () {
    return this._db;
};

function __SessionStorage() {
    this._db = {};
}

__SessionStorage.prototype.get = function () {
    return this._db;
};
