/**
 * Provides full WHATWG TextDecoder/TextEncoder including legacy encodings
 * (gbk, big5, shift_jis, euc-kr, iso-8859-*).  QuickJS only ships UTF-8.
 *
 * The text-encoding npm package is large because of its encoding tables;
 * accept the cost — Chinese / Japanese plugins won't decode otherwise.
 */
import { TextDecoder, TextEncoder } from 'text-encoding';

globalThis.TextDecoder = TextDecoder;
globalThis.TextEncoder = TextEncoder;
