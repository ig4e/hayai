/**
 * LNReader-compatible constants.
 * Matches: src/types/constants.ts + src/types/filters.ts (FilterTypes enum)
 * + src/lib/utils.ts (isUrlAbsolute) + @libs/utils (utf8ToBytes, bytesToUtf8)
 */

var NovelStatus = {
    Unknown: 'Unknown',
    Ongoing: 'Ongoing',
    Completed: 'Completed',
    Licensed: 'Licensed',
    PublishingFinished: 'Publishing Finished',
    Cancelled: 'Cancelled',
    OnHiatus: 'On Hiatus'
};

var FilterTypes = {
    TextInput: 'Text',
    Picker: 'Picker',
    CheckboxGroup: 'Checkbox',
    Switch: 'Switch',
    ExcludableCheckboxGroup: 'XCheckbox'
};

var defaultCover = 'https://github.com/LNReader/lnreader-plugins/blob/main/icons/src/coverNotAvailable.jpg?raw=true';

function isUrlAbsolute(url) {
    if (url) {
        if (url.indexOf('//') === 0) return true;
        if (url.indexOf('://') === -1) return false;
        if (url.indexOf('.') === -1) return false;
        if (url.indexOf('/') === -1) return false;
        if (url.indexOf(':') > url.indexOf('/')) return false;
        if (url.indexOf('://') < url.indexOf('.')) return true;
    }
    return false;
}

function utf8ToBytes(str) {
    var encoder = new TextEncoder();
    return encoder.encode(str);
}

function bytesToUtf8(bytes) {
    var decoder = new TextDecoder('utf-8');
    return decoder.decode(bytes instanceof Uint8Array ? bytes : new Uint8Array(bytes));
}
