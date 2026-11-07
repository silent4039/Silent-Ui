// Local development server. The production editor is served by PackServer in the jar.
const http = require('node:http');
const fs = require('node:fs');
const path = require('node:path');
const root = path.join(__dirname, '../src/main/resources/editor');
const files = new Map([
  ['/', ['index.html', 'text/html']], ['/editor', ['index.html', 'text/html']], ['/editor/', ['index.html', 'text/html']],
  ...['editor.css', 'editor.js'].flatMap(name => [['/' + name, [name, name.endsWith('.css') ? 'text/css' : 'text/javascript']], ['/editor/' + name, [name, name.endsWith('.css') ? 'text/css' : 'text/javascript']]])
]);
http.createServer((req, res) => {
  const file = files.get(new URL(req.url, 'http://localhost').pathname);
  if (!file) { res.writeHead(404); res.end('Not found'); return; }
  res.writeHead(200, {'Content-Type': file[1] + '; charset=utf-8', 'Cache-Control': 'no-cache'});
  fs.createReadStream(path.join(root, file[0])).pipe(res);
}).listen(48163, '127.0.0.1', () => console.log('Editor preview: http://127.0.0.1:48163/editor/?offline=1'));
