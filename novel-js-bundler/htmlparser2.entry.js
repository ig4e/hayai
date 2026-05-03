import * as htmlparser2 from 'htmlparser2';
import * as DomUtils from 'domutils';
import * as DomHandler from 'domhandler';

globalThis.__htmlparser2 = Object.assign({}, htmlparser2, {
  DomUtils,
  DomHandler,
  Parser: htmlparser2.Parser,
});
