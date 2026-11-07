"use strict";
/* ============================================================== utilities */
const $ = (s, r = document) => r.querySelector(s);
const $$ = (s, r = document) => [...r.querySelectorAll(s)];
const clamp = (v, a, b) => Math.max(a, Math.min(b, v));
const clone = o => JSON.parse(JSON.stringify(o));
const rid = (p = 'el') => p + Math.random().toString(36).slice(2, 8);
const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const round = (v, d = 2) => Math.round(v * 10 ** d) / 10 ** d;
function h(tag, attrs = {}, ...kids) {
  const e = document.createElement(tag);
  for (const [k, v] of Object.entries(attrs || {})) {
    if (v == null || v === false) continue;
    if (k === 'class') e.className = v;
    else if (k === 'style' && typeof v === 'object') Object.assign(e.style, v);
    else if (k.startsWith('on')) e.addEventListener(k.slice(2), v);
    else if (k === 'html') e.innerHTML = v;
    else e.setAttribute(k, v === true ? '' : v);
  }
  for (const k of kids.flat()) if (k != null && k !== false) e.append(k.nodeType ? k : document.createTextNode(k));
  return e;
}
function toast(msg, kind = '') {
  const t = h('div', {class: 'toast ' + kind}, msg);
  $('#toasts').append(t);
  setTimeout(() => t.remove(), 3200);
}
const store = {
  get(k, d) { try { const v = localStorage.getItem('silentui.' + k); return v == null ? d : JSON.parse(v); } catch { return d; } },
  set(k, v) { try { localStorage.setItem('silentui.' + k, JSON.stringify(v)); return true; } catch { return false; } }
};
const ICON = {
  select: '<path d="M5 3l14 8-6 2-3 6z"/>',
  rect: '<rect x="4" y="6" width="16" height="12" rx="2"/>',
  text: '<path d="M5 6V4h14v2M12 4v16M9 20h6"/>',
  button: '<rect x="3" y="7" width="18" height="10" rx="5"/><path d="M8 12h8"/>',
  image: '<rect x="3" y="4" width="18" height="16" rx="2"/><circle cx="9" cy="10" r="2"/><path d="m21 16-5-5-9 9"/>',
  input: '<rect x="3" y="7" width="18" height="10" rx="2"/><path d="M7 10v4"/>',
  progress: '<rect x="3" y="9" width="18" height="6" rx="3"/><path d="M5 12h7" stroke-width="3"/>',
  item: '<path d="M12 3 4 7.5v9L12 21l8-4.5v-9z"/><path d="m4 7.5 8 4.5 8-4.5M12 12v9"/>',
  head: '<rect x="5" y="5" width="14" height="14" rx="1"/><path d="M9 10h.01M15 10h.01M9 15h6"/>',
  sprites: '<path d="M4 4h7v7H4zM13 4h7v7h-7zM4 13h7v7H4z"/><path d="m14 17 2 2 4-4"/>',
  timeline: '<path d="M4 6h16M4 12h16M4 18h16"/><path d="M8 4v4M15 10v4M11 16v4"/>',
  script: '<circle cx="6" cy="6" r="2.5"/><circle cx="18" cy="18" r="2.5"/><path d="M8.5 6H13a3 3 0 0 1 3 3v6.5"/>',
  code: '<path d="m8 8-4 4 4 4M16 8l4 4-4 4"/>',
  eye: '<path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7S2 12 2 12z"/><circle cx="12" cy="12" r="3"/>',
  eyeoff: '<path d="M3 3l18 18M10.6 5.1A10 10 0 0 1 12 5c6.4 0 10 7 10 7a17 17 0 0 1-3.2 4.1M6.6 6.6A17 17 0 0 0 2 12s3.6 7 10 7a9.6 9.6 0 0 0 5.4-1.6"/>',
  lock: '<rect x="5" y="11" width="14" height="10" rx="2"/><path d="M8 11V7a4 4 0 0 1 8 0v4"/>',
  unlock: '<rect x="5" y="11" width="14" height="10" rx="2"/><path d="M8 11V7a4 4 0 0 1 7.5-2"/>',
  doc: '<path d="M6 3h8l4 4v14H6z"/><path d="M14 3v4h4"/>',
  model: '<circle cx="12" cy="5" r="2.5"/><path d="M9 9h6l1 6h-2l-1 6h-2l-1-6H8z"/>',
  assets: '<rect x="3" y="3" width="18" height="18" rx="3"/><path d="m3 16 5-5 4 4 3-3 6 6"/><circle cx="15.5" cy="8.5" r="1.5"/>',
  presets: '<rect x="3" y="3" width="8" height="8" rx="1.5"/><rect x="13" y="3" width="8" height="8" rx="1.5"/><rect x="3" y="13" width="8" height="8" rx="1.5"/><path d="M17 14v6M14 17h6"/>',
  commands: '<path d="m5 8 4 4-4 4M11 17h8"/>',
  library: '<path d="M4 4h4v16H4zM10 4h4v16h-4z"/><path d="m16 5 3.5-.8 3 15.6-3.5.8z"/>',
  braces: '<path d="M8 4c-2 0-2 1-2 3v2c0 1.5-1 2.5-2 3 1 .5 2 1.5 2 3v2c0 2 0 3 2 3M16 4c2 0 2 1 2 3v2c0 1.5 1 2.5 2 3-1 .5-2 1.5-2 3v2c0 2 0 3-2 3"/>'
};
const svg = (name, extra = '') => `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" ${extra}>${ICON[name] || ''}</svg>`;

/* ============================================================== model */
const TYPES = ['rect', 'text', 'button', 'image', 'input', 'progress', 'item', 'head', 'model'];
const ANIM_PROPS = ['x', 'y', 'w', 'h', 'scale', 'rotation', 'opacity', 'turn', 'fill', 'textColor'];
const COLOR_PROPS = ['color', 'fill', 'textColor'];
function defaultsFor(type) {
  const base = {id: '', name: 'Element', type, x: 0, y: 0, w: 64, h: 32, rotation: 0, opacity: 1, scale: 1, visible: true, blocked: false,
    anchor: 'center', fill: '#1e1e1e', fillEnabled: true, sprite: '', text: '', textColor: '#ffffff', textSize: 1, align: 'center', shadow: true,
    hoverFill: '', hoverSprite: '', hoverTextColor: '', hoverScale: 1, item: 'minecraft:diamond', headOwner: '', value: '50', max: '100',
    barColor: '#ffffff', barSprite: '', placeholder: 'Type...', group: '', tabFor: '', activeFill: '', activeSprite: '', activeTextColor: '',
    itemModel: '', glint: false, spin: 0, rotatable: false};
  const per = {
    rect: {name: 'Block', w: 80, h: 40},
    text: {name: 'Text', w: 120, h: 12, text: 'Hello <white>{player}</white>', fillEnabled: false, textColor: '#d9d9d9'},
    button: {name: 'Button', w: 90, h: 22, fill: '#ffffff', text: 'Click me', textColor: '#000000', shadow: false, hoverFill: '#d9d9d9', hoverScale: 1.05},
    image: {name: 'Image', w: 32, h: 32, fillEnabled: false},
    input: {name: 'Input', w: 130, h: 18, fill: '#1c1a22', text: '', align: 'left'},
    progress: {name: 'Progress', w: 120, h: 6},
    item: {name: 'Item', w: 24, h: 24, fillEnabled: false},
    head: {name: 'Head', w: 32, h: 32, fillEnabled: false},
    model: {name: 'Player model', w: 40, h: 80, fillEnabled: false, rotatable: true}
  };
  return Object.assign(base, per[type] || {});
}
function newDoc(type = 'screen', name = 'Untitled') {
  const dims = type === 'chest' ? [176, 222] : type === 'world' ? [400, 240] : [480, 270];
  return {format: 1, id: slug(name) || 'ui', name, type, canvas: {width: dims[0], height: dims[1]},
    settings: {command: '', permission: '', freeze: true, closeOnSneak: true, clampCursor: true, dim: type === 'screen' ? 0.3 : 0,
      cursorSprite: '', screenSize: 1, autoShow: false, worldScale: 1, billboard: 'fixed', perPlayer: false, viewRange: 48, tilt: 0, rows: 6, guiTop: 0, title: ''},
    sprites: [], elements: [], animations: [], script: {nodes: []}, tabs: {}, commands: [], slots: []};
}
function slug(s) { return String(s || '').toLowerCase().replace(/[^a-z0-9_]+/g, '_').replace(/^_+|_+$/g, '').slice(0, 48); }
function normalize(d) {
  if (!d || typeof d !== 'object' || Array.isArray(d)) throw new Error('A UI must be a JSON object.');
  for (const k of ['elements', 'sprites', 'animations', 'commands', 'slots']) if (d[k] != null && !Array.isArray(d[k])) throw new Error(k + ' must be an array.');
  if (d.script?.nodes != null && !Array.isArray(d.script.nodes)) throw new Error('script.nodes must be an array.');
  const n = newDoc(d.type || 'screen', d.name || 'UI');
  const out = Object.assign(n, d);
  out.canvas = Object.assign(n.canvas, d.canvas || {});
  out.settings = Object.assign(newDoc().settings, d.settings || {});
  out.sprites = (d.sprites || []).filter(s => s && s.id && s.data);
  out.elements = (d.elements || []).filter(Boolean).map(e => Object.assign(defaultsFor(e.type || 'rect'), e));
  out.animations = (d.animations || []).filter(Boolean).map(a => Object.assign({id: rid('an'), name: 'Animation', length: 20, loop: false, autoplay: false}, a, {tracks: (a.tracks || []).filter(Boolean).map(t => ({...t, keys: (t.keys || []).filter(Boolean).sort((a, b) => a.t - b.t)}))}));
  out.script = {nodes: ((d.script && d.script.nodes) || []).filter(Boolean).map(nd => Object.assign({}, nd, {props: nd.props || {}, next: nd.next || {}}))};
  out.tabs = Object.assign({}, d.tabs || {});
  out.commands = (d.commands || []).map(c => Object.assign({name: 'cmd', aliases: [], permission: '', description: '', args: [], function: '', open: true}, c,
    {args: (c.args || []).map(a => Object.assign({name: 'arg', type: 'word', choices: [], optional: false, def: ''}, a))}));
  out.id = slug(out.id) || slug(out.name) || 'ui';
  out.canvas.width = clamp(Number(out.canvas.width) || 480, 16, 2048);
  out.canvas.height = clamp(Number(out.canvas.height) || 270, 16, 2048);
  out.slots = (d.slots || []).filter(s => s && Number.isInteger(s.slot) && s.slot >= 0 && s.slot < 54).map(s => ({...slotDefaults(s.slot), ...s}));
  if (out.type === 'chest') resizeChest(out);
  return out;
}

/* ============================================================== state */
const S = {
  docs: [], cur: 0, sel: [], tool: 'select', zoom: 1, panX: 40, panY: 40, side: 'props',
  dock: false, scriptOpen: false, anim: {id: null, t: 0, playing: false, auto: false, selKey: null},
  undo: [], redo: [], sessions: [], closedTabs: [], clip: null, hover: null, guides: [], spaceDown: false, imgs: {}, nav: 'layers', layerQuery: '', saveState: 'saved', grid: false
};
const doc = () => S.docs[S.cur];
const els = () => doc().elements;
const byId = id => els().find(e => e.id === id);
const selected = () => S.sel.map(byId).filter(Boolean);

function snapshot() { return JSON.stringify(doc()); }
function pushUndo(snap) {
  S.undo.push(snap ?? snapshot());
  if (S.undo.length > 120) S.undo.shift();
  S.redo = [];
}
/** Run a change with undo support. */
function mutate(fn, opts = {}) {
  pushUndo();
  fn(doc());
  afterChange(opts);
}
let saveTimer = 0;
function afterChange(opts = {}) {
  S.saveState = 'pending';
  render(opts);
  clearTimeout(saveTimer);
  saveTimer = setTimeout(saveLocal, 400);
}
function undo() {
  if (!S.undo.length) return toast('Nothing to undo');
  S.redo.push(snapshot());
  S.docs[S.cur] = JSON.parse(S.undo.pop());
  S.sel = S.sel.filter(id => byId(id));
  afterChange();
}
function redo() {
  if (!S.redo.length) return toast('Nothing to redo');
  S.undo.push(snapshot());
  S.docs[S.cur] = JSON.parse(S.redo.pop());
  S.sel = S.sel.filter(id => byId(id));
  afterChange();
}
function saveLocal() {
  clearTimeout(saveTimer);
  const ok = store.set('docs', {docs: S.docs, cur: S.cur});
  S.saveState = ok ? 'saved' : 'error';
  renderSaveState();
  return ok;
}

/* ============================================================== sprites */
function spriteUrl(id) {
  const s = doc().sprites.find(x => x.id === id);
  return s ? s.data : '';
}
function loadImage(src) {
  return new Promise((res, rej) => { const i = new Image(); i.onload = () => res(i); i.onerror = rej; i.src = src; });
}
async function addSpriteFromDataUrl(dataUrl, name) {
  let img = await loadImage(dataUrl);
  let w = img.naturalWidth, hh = img.naturalHeight;
  if (w > 256 || hh > 256) {
    const f = 256 / Math.max(w, hh);
    const c = document.createElement('canvas');
    c.width = Math.max(1, Math.round(w * f)); c.height = Math.max(1, Math.round(hh * f));
    const g = c.getContext('2d'); g.imageSmoothingEnabled = false; g.drawImage(img, 0, 0, c.width, c.height);
    dataUrl = c.toDataURL('image/png'); w = c.width; hh = c.height;
    toast('Large image scaled down to ' + w + '×' + hh);
  } else if (!dataUrl.startsWith('data:image/png')) {
    const c = document.createElement('canvas'); c.width = w; c.height = hh;
    c.getContext('2d').drawImage(img, 0, 0); dataUrl = c.toDataURL('image/png');
  }
  let id = slug(name) || 'sprite';
  const taken = new Set(doc().sprites.map(s => s.id));
  let n = 2, base = id;
  while (taken.has(id)) id = base + '_' + n++;
  mutate(d => d.sprites.push({id, data: dataUrl, width: w, height: hh}));
  return id;
}
/** Pixel-art friendly shape generator (rounded panels, circles, bars, icons). */
function genShape(o) {
  const c = document.createElement('canvas');
  c.width = o.w; c.height = o.h;
  const g = c.getContext('2d');
  const r = Math.min(o.r || 0, Math.floor(Math.min(o.w, o.h) / 2));
  const inside = (x, y, inset) => {
    const w = o.w - inset * 2, hh = o.h - inset * 2, rr = Math.max(0, r - inset);
    const px = x - inset + 0.5, py = y - inset + 0.5;
    if (px < 0 || py < 0 || px > w || py > hh) return false;
    if (o.kind === 'circle') { const cx = w / 2, cy = hh / 2; return ((px - cx) / (w / 2)) ** 2 + ((py - cy) / (hh / 2)) ** 2 <= 1; }
    const cx = clamp(px, rr, w - rr), cy = clamp(py, rr, hh - rr);
    return (px - cx) ** 2 + (py - cy) ** 2 <= rr * rr + 0.25;
  };
  const hex = (s, a) => { const v = parseInt((s || '#000').slice(1), 16); return [(v >> 16) & 255, (v >> 8) & 255, v & 255, Math.round(a * 255)]; };
  const fill = hex(o.fill, o.alpha ?? 1), fill2 = hex(o.fill2 || o.fill, o.alpha ?? 1), border = hex(o.border, o.borderAlpha ?? 1);
  const bw = o.bw || 0;
  const im = g.createImageData(o.w, o.h);
  for (let y = 0; y < o.h; y++) for (let x = 0; x < o.w; x++) {
    let col = null;
    if (inside(x, y, 0)) {
      if (bw > 0 && !inside(x, y, bw)) col = border;
      else {
        const t = o.kind === 'gradient' ? x / Math.max(1, o.w - 1) : (o.vgrad ? y / Math.max(1, o.h - 1) : 0);
        col = fill.map((v, i) => Math.round(v + (fill2[i] - v) * t));
      }
    }
    if (col) { const i = (y * o.w + x) * 4; im.data.set(col, i); }
  }
  g.putImageData(im, 0, 0);
  if (o.icon) drawIcon(g, o.icon, o.w, o.h, o.iconColor || '#ffffff');
  return c.toDataURL('image/png');
}
function drawIcon(g, kind, w, hh, color) {
  g.fillStyle = color;
  const P = (x, y, ww = 1, h2 = 1) => g.fillRect(x, y, ww, h2);
  const s = Math.floor(Math.min(w, hh) / 8) || 1;
  const ox = Math.floor((w - 8 * s) / 2), oy = Math.floor((hh - 8 * s) / 2);
  const maps = {
    close: ['X......X', '.X....X.', '..X..X..', '...XX...', '...XX...', '..X..X..', '.X....X.', 'X......X'],
    gem: ['..XXXX..', '.X....X.', 'XXXXXXXX', 'X.X..X.X', '.X.XX.X.', '..X..X..', '...XX...', '........'],
    clock: ['..XXXX..', '.X....X.', 'X...X..X', 'X...X..X', 'X...XXXX', 'X......X', '.X....X.', '..XXXX..'],
    user: ['...XX...', '..XXXX..', '..XXXX..', '...XX...', '.XXXXXX.', 'XXXXXXXX', 'XXXXXXXX', '........'],
    star: ['...XX...', '...XX...', 'XXXXXXXX', '.XXXXXX.', '..XXXX..', '.XX..XX.', 'XX....XX', '........'],
    home: ['...XX...', '..XXXX..', '.XXXXXX.', 'XXXXXXXX', '.X....X.', '.X.XX.X.', '.X.XX.X.', '.XXXXXX.'],
    cart: ['X.......', 'XXXXXXXX', '.X.....X', '.X....X.', '.XXXXXX.', '........', '.XX..XX.', '.XX..XX.'],
    gear: ['...XX...', '.X.XX.X.', '..XXXX..', 'XXX..XXX', 'XXX..XXX', '..XXXX..', '.X.XX.X.', '...XX...'],
    portal: ['.XXXXXX.', 'X......X', 'X.XXXX.X', 'X.X..X.X', 'X.X..X.X', 'X.XXXX.X', 'X......X', '.XXXXXX.'],
    chart: ['.......X', '......XX', '.....X.X', 'X...X..X', 'XX.X...X', 'X.X....X', 'X......X', 'XXXXXXXX']
  };
  const m = maps[kind]; if (!m) return;
  m.forEach((row, y) => [...row].forEach((ch, x) => { if (ch === 'X') P(ox + x * s, oy + y * s, s, s); }));
}

/* ============================================================== templates */
function el(type, props) { const e = Object.assign(defaultsFor(type), {id: rid()}, props); return e; }
function node(type, x, y, props = {}, next = {}) { return {id: props.__id || rid('n'), type, x, y, props: Object.fromEntries(Object.entries(props).filter(([k]) => k !== '__id')), next}; }
function tplMenu() {
  const d = newDoc('screen', 'Profile Menu');
  d.id = 'example_menu';
  d.settings.command = 'menu';
  d.settings.dim = 0.35;
  d.sprites = [
    {id: 'panel', data: genShape({w: 200, h: 236, r: 9, fill: '#141414', fill2: '#0b0b0b', vgrad: true, alpha: 0.94, border: '#ffffff', bw: 1, borderAlpha: 0.55}), width: 200, height: 236},
    {id: 'tile', data: genShape({w: 52, h: 46, r: 6, fill: '#1e1e1e', alpha: 0.95}), width: 52, height: 46},
    {id: 'tile_hover', data: genShape({w: 52, h: 46, r: 6, fill: '#2a2a2a', alpha: 1, border: '#ffffff', bw: 1}), width: 52, height: 46},
    {id: 'close', data: genShape({w: 12, h: 12, r: 3, fill: '#1e1e1e', icon: 'close', iconColor: '#ffffff'}), width: 12, height: 12},
    {id: 'bar', data: genShape({w: 120, h: 6, r: 3, fill: '#1e1e1e'}), width: 120, height: 6},
    {id: 'ic_home', data: genShape({w: 16, h: 16, icon: 'home', iconColor: '#ffffff'}), width: 16, height: 16},
    {id: 'ic_cart', data: genShape({w: 16, h: 16, icon: 'cart', iconColor: '#e6e6e6'}), width: 16, height: 16},
    {id: 'ic_portal', data: genShape({w: 16, h: 16, icon: 'portal', iconColor: '#cfcfcf'}), width: 16, height: 16},
    {id: 'ic_user', data: genShape({w: 16, h: 16, icon: 'user', iconColor: '#f2f2f2'}), width: 16, height: 16},
    {id: 'ic_chart', data: genShape({w: 16, h: 16, icon: 'chart', iconColor: '#ffffff'}), width: 16, height: 16},
    {id: 'ic_gear', data: genShape({w: 16, h: 16, icon: 'gear', iconColor: '#bdbdbd'}), width: 16, height: 16}
  ];
  const P = {x: 140, y: 17};
  const panel = el('image', {id: 'panel', name: 'Panel', x: P.x, y: P.y, w: 200, h: 236, sprite: 'panel', blocked: true});
  const title = el('text', {id: 'title', name: 'Title', x: P.x + 12, y: P.y + 10, w: 150, h: 12, text: '<bold>Your Profile</bold>', align: 'left', blocked: true});
  const close = el('image', {id: 'close', name: 'Close', x: P.x + 176, y: P.y + 10, w: 12, h: 12, sprite: 'close', hoverScale: 1.2});
  const head = el('head', {id: 'head', name: 'Head', x: P.x + 80, y: P.y + 32, w: 40, h: 40, blocked: true});
  const name = el('text', {id: 'name', name: 'Player name', x: P.x + 10, y: P.y + 78, w: 180, h: 12, text: '{player}', blocked: true});
  const lvl = el('text', {id: 'level', name: 'Level', x: P.x + 10, y: P.y + 91, w: 180, h: 10, text: '<gray>Level</gray> <white>{level}</white>', textSize: 0.8, blocked: true});
  const bar = el('progress', {id: 'xp', name: 'XP bar', x: P.x + 40, y: P.y + 106, w: 120, h: 6, sprite: 'bar', fillEnabled: true, value: '{var:xp}', max: '100', barColor: '#ffffff', blocked: true});
  const out = [panel, title, close, head, name, lvl, bar];
  const tiles = [['spawn', 'Spawn', 'ic_home', 'spawn'], ['shop', 'Shop', 'ic_cart', 'shop'], ['warps', 'Warps', 'ic_portal', 'warps'],
    ['friends', 'Friends', 'ic_user', 'friends'], ['stats', 'Stats', 'ic_chart', 'stats'], ['settings', 'Settings', 'ic_gear', 'settings']];
  tiles.forEach(([id, label, icon], i) => {
    const cx = P.x + 15 + (i % 3) * 59, cy = P.y + 124 + Math.floor(i / 3) * 52;
    out.push(el('button', {id: 't_' + id, name: label + ' tile', x: cx, y: cy, w: 52, h: 46, sprite: 'tile', hoverSprite: 'tile_hover', fillEnabled: false, text: '', hoverScale: 1.06}));
    out.push(el('image', {id: 'i_' + id, name: label + ' icon', x: cx + 18, y: cy + 8, w: 16, h: 16, sprite: icon, blocked: true}));
    out.push(el('text', {id: 'l_' + id, name: label + ' label', x: cx, y: cy + 30, w: 52, h: 10, text: label, textSize: 0.75, blocked: true}));
  });
  d.elements = out;
  d.animations = [{id: 'intro', name: 'intro', length: 10, loop: false, autoplay: true, tracks: [
    {element: 'panel', property: 'opacity', keys: [{t: 0, v: 0, ease: 'ease-out'}, {t: 6, v: 1, ease: 'linear'}]},
    {element: 'panel', property: 'y', keys: [{t: 0, v: P.y + 14, ease: 'ease-out'}, {t: 8, v: P.y, ease: 'linear'}]},
    {element: 'head', property: 'scale', keys: [{t: 0, v: 0.4, ease: 'back'}, {t: 10, v: 1, ease: 'linear'}]}
  ]}];
  const n = [];
  n.push(node('on_open', 40, 40, {__id: 'n_open'}, {then: ['n_xp']}));
  n.push(node('set_var', 320, 40, {__id: 'n_xp', name: 'xp', value: '64'}));
  n.push(node('on_click', 40, 220, {__id: 'n_close', element: 'close', button: 'any'}, {then: ['n_closeui']}));
  n.push(node('close_ui', 320, 220, {__id: 'n_closeui'}));
  n.push(node('on_hover_enter', 40, 360, {__id: 'n_hov'}, {then: ['n_snd']}));
  n.push(node('sound', 320, 360, {__id: 'n_snd', sound: 'minecraft:block.note_block.hat', volume: '0.4', pitch: '1.6'}));
  tiles.forEach(([id, label, , cmd], i) => {
    const y = 560 + i * 170;
    n.push(node('on_click', 40, y, {__id: 'c_' + id, element: 't_' + id, button: 'any'}, {then: ['r_' + id]}));
    n.push(node('run_command', 320, y, {__id: 'r_' + id, command: cmd, as: 'player'}, {then: ['x_' + id]}));
    n.push(node('close_ui', 600, y, {__id: 'x_' + id}));
  });
  d.script.nodes = n;
  return d;
}
function tplHud() {
  const d = newDoc('hud', 'Stats HUD');
  d.id = 'example_hud';
  d.settings.autoShow = true;
  d.sprites = [
    {id: 'chip', data: genShape({w: 64, h: 15, r: 4, fill: '#000000', alpha: 0.62}), width: 64, height: 15},
    {id: 'chip_wide', data: genShape({w: 150, h: 13, r: 4, fill: '#000000', alpha: 0.5}), width: 150, height: 13},
    {id: 'ic_clock', data: genShape({w: 8, h: 8, icon: 'clock', iconColor: '#ffffff'}), width: 8, height: 8},
    {id: 'ic_gem', data: genShape({w: 8, h: 8, icon: 'gem', iconColor: '#e6e6e6'}), width: 8, height: 8},
    {id: 'ic_user', data: genShape({w: 8, h: 8, icon: 'user', iconColor: '#f2f2f2'}), width: 8, height: 8}
  ];
  const cx = 240, y = 22;
  const chips = [['clock', '{time}', 'ic_clock'], ['gems', '{var:gems}', 'ic_gem'], ['online', '{online} online', 'ic_user']];
  const out = [];
  chips.forEach(([id, txt, ic], i) => {
    const x = cx - 104 + i * 70;
    out.push(el('image', {id: 'chip_' + id, name: id + ' chip', x, y, w: 64, h: 15, sprite: 'chip'}));
    out.push(el('image', {id: 'ic_' + id, name: id + ' icon', x: x + 5, y: y + 4, w: 8, h: 8, sprite: ic}));
    out.push(el('text', {id: 'tx_' + id, name: id + ' text', x: x + 15, y: y + 4, w: 46, h: 8, text: txt, align: 'left'}));
  });
  out.push(el('image', {id: 'obj_bg', name: 'Objective bg', x: cx - 75, y: y + 19, w: 150, h: 13, sprite: 'chip_wide'}));
  out.push(el('text', {id: 'obj', name: 'Objective', x: cx - 75, y: y + 21, w: 150, h: 9, text: '<white>Objective:</white> find the shrine'}));
  out.push(el('progress', {id: 'xpbar', name: 'Level bar', x: cx - 60, y: y + 36, w: 120, h: 3, fill: '#1e1e1e', value: '{level}', max: '50', barColor: '#ffffff'}));
  d.elements = out;
  d.script.nodes = [
    node('on_open', 40, 40, {__id: 'h_open'}, {then: ['h_set']}),
    node('set_var', 320, 40, {__id: 'h_set', name: 'gems', value: '250'})
  ];
  return d;
}
function tplBoard() {
  const d = newDoc('world', 'Welcome Board');
  d.id = 'example_board';
  d.canvas = {width: 320, height: 180};
  d.settings.worldScale = 1.2;
  d.sprites = [
    {id: 'board', data: genShape({w: 160, h: 90, r: 6, fill: '#161616', fill2: '#080808', vgrad: true, border: '#ffffff', bw: 1}), width: 160, height: 90},
    {id: 'btn', data: genShape({w: 64, h: 20, r: 5, fill: '#ffffff'}), width: 64, height: 20},
    {id: 'btn_hover', data: genShape({w: 64, h: 20, r: 5, fill: '#d9d9d9'}), width: 64, height: 20},
    {id: 'star', data: genShape({w: 16, h: 16, icon: 'star', iconColor: '#ffffff'}), width: 16, height: 16}
  ];
  d.elements = [
    el('image', {id: 'bg', name: 'Board', x: 0, y: 0, w: 320, h: 180, sprite: 'board', blocked: true}),
    el('image', {id: 'star', name: 'Star', x: 152, y: 18, w: 16, h: 16, sprite: 'star', blocked: true}),
    el('text', {id: 'title', name: 'Title', x: 20, y: 40, w: 280, h: 22, text: '<bold>Welcome!</bold>', textSize: 2, blocked: true}),
    el('text', {id: 'sub', name: 'Subtitle', x: 20, y: 70, w: 280, h: 12, text: '<gray>Look at a button and click it</gray>', blocked: true}),
    el('button', {id: 'b_rules', name: 'Rules', x: 40, y: 110, w: 70, h: 22, sprite: 'btn', hoverSprite: 'btn_hover', fillEnabled: false, text: 'Rules'}),
    el('button', {id: 'b_spawn', name: 'Spawn', x: 125, y: 110, w: 70, h: 22, sprite: 'btn', hoverSprite: 'btn_hover', fillEnabled: false, text: 'Spawn'}),
    el('button', {id: 'b_hi', name: 'Wave', x: 210, y: 110, w: 70, h: 22, sprite: 'btn', hoverSprite: 'btn_hover', fillEnabled: false, text: 'Wave'}),
    el('text', {id: 'last', name: 'Last action', x: 20, y: 148, w: 280, h: 10, text: '<dark_gray>Nobody has clicked yet</dark_gray>', textSize: 0.8, blocked: true})
  ];
  d.elements.filter(e => e.type === 'button').forEach(b => { b.textColor = '#000000'; b.shadow = false; });
  d.animations = [{id: 'spin', name: 'spin', length: 40, loop: true, autoplay: true, tracks: [
    {element: 'star', property: 'rotation', keys: [{t: 0, v: 0, ease: 'linear'}, {t: 40, v: 360, ease: 'linear'}]}]}];
  d.script.nodes = [
    node('on_click', 40, 40, {__id: 'w1', element: 'b_rules'}, {then: ['w2']}),
    node('message', 320, 40, {__id: 'w2', text: '<white>Rules:</white> be kind, no griefing, have fun.'}),
    node('on_click', 40, 220, {__id: 'w3', element: 'b_spawn'}, {then: ['w4']}),
    node('run_command', 320, 220, {__id: 'w4', command: 'spawn', as: 'player'}),
    node('on_click', 40, 400, {__id: 'w5', element: 'b_hi'}, {then: ['w6']}),
    node('set_text', 320, 400, {__id: 'w6', element: 'last', text: '<gray>{player} waved hello!</gray>'}, {then: ['w7']}),
    node('play_animation', 600, 400, {__id: 'w7', animation: 'spin'})
  ];
  return d;
}
/* ============================================================== script node catalogue (shared contract with the plugin) */
const KEYS = ['any', 'W', 'A', 'S', 'D', 'JUMP', 'SNEAK', 'SPRINT', 'DROP', 'SWAP', 'SCROLL_UP', 'SCROLL_DOWN', 'SLOT_1', 'SLOT_2', 'SLOT_3', 'SLOT_4', 'SLOT_5', 'SLOT_6', 'SLOT_7', 'SLOT_8', 'SLOT_9'];
const OPS = ['==', '!=', '>', '>=', '<', '<=', 'contains'];
const F = {
  el: (k = 'element', label = 'Element', any = false) => ({k, label, kind: 'element', any}),
  txt: (k, label, ph = '') => ({k, label, kind: 'text', ph}),
  num: (k, label, def = '') => ({k, label, kind: 'number', def}),
  col: (k, label) => ({k, label, kind: 'color'}),
  sel: (k, label, opts) => ({k, label, kind: 'select', opts}),
  spr: (k = 'sprite', label = 'Sprite') => ({k, label, kind: 'sprite'}),
  anim: () => ({k: 'animation', label: 'Animation', kind: 'anim'}),
  ui: () => ({k: 'ui', label: 'UI id', kind: 'text', ph: 'example_menu'}),
  tabset: () => ({k: 'tabset', label: 'Tab set', kind: 'tabset'}),
  group: () => ({k: 'group', label: 'Group', kind: 'group'}),
  fn: (k = 'name', label = 'Function') => ({k, label, kind: 'fn'}),
  item: (k = 'item', label = 'Item') => ({k, label, kind: 'item'})
};
const NODE_TYPES = {
  on_slot_click: {cat: 'event', label: 'When inventory slot is clicked', fields: [F.txt('slot', 'Slots', '0, 1, 2 or 0-8'), F.sel('button', 'Button', ['any', 'left', 'right', 'shift'])]},
  set_slot: {cat: 'element', label: 'Set inventory slot', fields: [F.num('slot', 'Slot', '0'), F.item(), F.num('amount', 'Amount', '1'), F.txt('name', 'Name'), F.txt('lore', 'Lore')]},
  next_tab: {cat: 'element', label: 'Next / previous tab', fields: [F.tabset(), F.sel('direction', 'Direction', ['next', 'previous'])]},
  // events
  on_open: {cat: 'event', label: 'When page opens', fields: []},
  on_close: {cat: 'event', label: 'When page closes', fields: []},
  on_click: {cat: 'event', label: 'When clicked', fields: [F.el('element', 'Element', true), F.sel('button', 'Button', ['any', 'left', 'right'])]},
  on_hover_enter: {cat: 'event', label: 'When cursor enters', fields: [F.el('element', 'Element', true)]},
  on_hover_leave: {cat: 'event', label: 'When cursor leaves', fields: [F.el('element', 'Element', true)]},
  on_timer: {cat: 'event', label: 'Every few seconds', fields: [F.num('seconds', 'Seconds', '1')]},
  on_key: {cat: 'event', label: 'When key pressed', fields: [F.sel('key', 'Key', KEYS)]},
  on_text: {cat: 'event', label: 'When text is typed', fields: [F.el('element', 'Input', true)]},
  on_command: {cat: 'event', label: 'When command is used', fields: [F.txt('command', 'Command', 'menu'), F.txt('permission', 'Permission', 'optional')]},
  on_function: {cat: 'event', label: 'When function is called', fields: [F.txt('name', 'Name', 'buy_item')]},
  // elements
  show: {cat: 'element', label: 'Show', fields: [F.el()]},
  hide: {cat: 'element', label: 'Hide', fields: [F.el()]},
  toggle: {cat: 'element', label: 'Toggle visibility', fields: [F.el()]},
  set_text: {cat: 'element', label: 'Set text', fields: [F.el(), F.txt('text', 'Text', 'Hello {player}')]},
  set_color: {cat: 'element', label: 'Set colour', fields: [F.el(), F.col('color', 'Colour')]},
  set_text_color: {cat: 'element', label: 'Set text colour', fields: [F.el(), F.col('color', 'Colour')]},
  set_sprite: {cat: 'element', label: 'Set sprite', fields: [F.el(), F.spr()]},
  set_progress: {cat: 'element', label: 'Set progress', fields: [F.el(), F.txt('value', 'Value', '{var:xp}'), F.txt('max', 'Max', '100')]},
  move: {cat: 'element', label: 'Move element', fields: [F.el(), F.num('x', 'X'), F.num('y', 'Y'), F.num('ticks', 'Ticks', '10')]},
  play_animation: {cat: 'element', label: 'Play animation', fields: [F.anim()]},
  stop_animation: {cat: 'element', label: 'Stop animation', fields: [F.anim()]},
  focus_input: {cat: 'element', label: 'Ask for text', fields: [F.el('element', 'Input')]},
  switch_tab: {cat: 'element', label: 'Switch tab', fields: [F.tabset(), F.txt('page', 'Page', 'shop')]},
  show_group: {cat: 'element', label: 'Show group', fields: [F.group()]},
  hide_group: {cat: 'element', label: 'Hide group', fields: [F.group()]},
  toggle_group: {cat: 'element', label: 'Toggle group', fields: [F.group()]},
  set_item: {cat: 'element', label: 'Set item', fields: [F.el(), F.item()]},
  set_head: {cat: 'element', label: 'Set head / model skin', fields: [F.el(), F.txt('owner', 'Player', '{player}')]},
  rotate: {cat: 'element', label: 'Rotate 3D', fields: [F.el(), F.num('degrees', 'Degrees', '90'), F.num('ticks', 'Ticks', '10')]},
  // player / server
  open_ui: {cat: 'action', label: 'Open UI', fields: [F.ui()]},
  close_ui: {cat: 'action', label: 'Close UI', fields: []},
  give_item: {cat: 'action', label: 'Give item', fields: [F.item(), F.num('amount', 'Amount', '1'), F.txt('name', 'Name', 'optional'), F.txt('lore', 'Lore (a|b)', 'optional'), F.txt('model', 'Item model', 'optional')]},
  run_command: {cat: 'action', label: 'Run command', fields: [F.txt('command', 'Command', 'spawn'), F.sel('as', 'As', ['player', 'console'])]},
  message: {cat: 'action', label: 'Send message', fields: [F.txt('text', 'Text', '<#ff8fd1>Hi {player}!')]},
  broadcast: {cat: 'action', label: 'Broadcast', fields: [F.txt('text', 'Text')]},
  actionbar: {cat: 'action', label: 'Action bar', fields: [F.txt('text', 'Text')]},
  title: {cat: 'action', label: 'Show title', fields: [F.txt('title', 'Title'), F.txt('subtitle', 'Subtitle'), F.num('in', 'Fade in', '10'), F.num('stay', 'Stay', '40'), F.num('out', 'Fade out', '10')]},
  sound: {cat: 'action', label: 'Play sound', fields: [F.txt('sound', 'Sound', 'minecraft:ui.button.click'), F.num('volume', 'Volume', '1'), F.num('pitch', 'Pitch', '1')]},
  screen_fade: {cat: 'action', label: 'Screen fade', fields: [F.col('color', 'Colour'), F.num('in', 'In', '10'), F.num('stay', 'Stay', '10'), F.num('out', 'Out', '10')]},
  // logic
  wait: {cat: 'logic', label: 'Wait', fields: [F.num('ticks', 'Ticks', '20')]},
  call_function: {cat: 'logic', label: 'Call function', fields: [F.fn(), F.txt('args', 'Args', 'item=diamond, price=10'), F.txt('ui', 'In UI', 'this UI')]},
  set_var: {cat: 'logic', label: 'Set variable', fields: [F.txt('name', 'Name', 'coins'), F.txt('value', 'Value', '0')]},
  add_var: {cat: 'logic', label: 'Add to variable', fields: [F.txt('name', 'Name', 'coins'), F.txt('amount', 'Amount', '1')]},
  if_var: {cat: 'logic', label: 'If variable', branch: true, fields: [F.txt('name', 'Name', 'coins'), F.sel('op', 'Is', OPS), F.txt('value', 'Value', '10')]},
  if_compare: {cat: 'logic', label: 'If compare', branch: true, fields: [F.txt('left', 'Left', '%player_level%'), F.sel('op', 'Is', OPS), F.txt('right', 'Right', '10')]},
  if_permission: {cat: 'logic', label: 'If permission', branch: true, fields: [F.txt('permission', 'Permission', 'rank.vip')]},
  random: {cat: 'logic', label: 'Random chance', branch: true, fields: [F.num('chance', 'Chance %', '50')]}
};
const CAT = {event: ['Events', '#ffffff'], element: ['Elements', '#b5b5b5'], action: ['Player & server', '#7a7a7a'], logic: ['Logic', '#4a4a4a']};

/* ============================================================== MiniMessage-ish preview */
const NAMED = {black: '#000000', dark_blue: '#0000aa', dark_green: '#00aa00', dark_aqua: '#00aaaa', dark_red: '#aa0000', dark_purple: '#aa00aa', gold: '#ffaa00',
  gray: '#aaaaaa', grey: '#aaaaaa', dark_gray: '#555555', dark_grey: '#555555', blue: '#5555ff', green: '#55ff55', aqua: '#55ffff', red: '#ff5555', light_purple: '#ff55ff', yellow: '#ffff55', white: '#ffffff'};
const LEGACY = {'0': 'black', '1': 'dark_blue', '2': 'dark_green', '3': 'dark_aqua', '4': 'dark_red', '5': 'dark_purple', '6': 'gold', '7': 'gray', '8': 'dark_gray', '9': 'blue', a: 'green', b: 'aqua', c: 'red', d: 'light_purple', e: 'yellow', f: 'white'};
function mmToHtml(src) {
  let s = String(src ?? '');
  if (s.includes('&') && !s.includes('<')) {
    s = s.replace(/&([0-9a-fk-or])/gi, (_, c) => { c = c.toLowerCase(); return LEGACY[c] ? `<${LEGACY[c]}>` : c === 'l' ? '<bold>' : c === 'o' ? '<italic>' : c === 'n' ? '<underlined>' : c === 'r' ? '<reset>' : ''; });
  }
  let out = '', stack = 0;
  const re = /<(\/?)([#a-z_0-9:]+)>/gi;
  let last = 0, m;
  while ((m = re.exec(s))) {
    out += esc(s.slice(last, m.index));
    last = re.lastIndex;
    const close = m[1] === '/', tag = m[2].toLowerCase();
    if (close) { if (stack > 0) { out += '</span>'; stack--; } continue; }
    let style = null;
    if (/^#[0-9a-f]{6}$/.test(tag)) style = 'color:' + tag;
    else if (tag.startsWith('color:')) style = 'color:' + (NAMED[tag.slice(6)] || tag.slice(6));
    else if (NAMED[tag]) style = 'color:' + NAMED[tag];
    else if (tag === 'bold' || tag === 'b') style = 'font-weight:700';
    else if (tag === 'italic' || tag === 'i' || tag === 'em') style = 'font-style:italic';
    else if (tag === 'underlined' || tag === 'u') style = 'text-decoration:underline';
    else if (tag === 'strikethrough' || tag === 'st') style = 'text-decoration:line-through';
    else if (tag === 'reset') { out += '</span>'.repeat(stack); stack = 0; continue; }
    else if (tag === 'newline' || tag === 'br') { out += '\n'; continue; }
    if (style) { out += `<span style="${style}">`; stack++; }
  }
  out += esc(s.slice(last)) + '</span>'.repeat(stack);
  return out;
}
const PV_VARS = {};
function resolve(s, vars = PV_VARS) {
  const now = new Date();
  return String(s ?? '')
    .replace(/\{player}/g, 'Silent').replace(/\{online}/g, '12').replace(/\{max_players}/g, '100').replace(/\{level}/g, '27')
    .replace(/\{health}/g, '20').replace(/\{food}/g, '20').replace(/\{ping}/g, '24').replace(/\{world}/g, 'world')
    .replace(/\{x}/g, '128').replace(/\{y}/g, '64').replace(/\{z}/g, '-40').replace(/\{uuid}/g, '0000-…')
    .replace(/\{time}/g, now.toTimeString().slice(0, 8)).replace(/\{date}/g, now.toISOString().slice(0, 10))
    .replace(/\{var:([A-Za-z0-9_.-]+)}/g, (_, k) => vars[k] ?? '')
    .replace(/\{arg:([A-Za-z0-9_]+)}/g, (_, k) => vars['arg:' + k] ?? '')
    .replace(/\{(arg\d|args|text|key|button|element)}/g, '');
}

/* ============================================================== element values (base + animation preview) */
function sampleColor(keys, t) {
  const channel = shift => sampleKeys(keys.map(k => ({...k, v: (Math.round(k.v) >> shift) & 255})), t);
  return '#' + [16, 8, 0].map(s => clamp(Math.round(channel(s)), 0, 255).toString(16).padStart(2, '0')).join('');
}
function sampleKeys(keys, t) {
  if (!keys.length) return 0;
  if (t <= keys[0].t) return keys[0].v;
  for (let i = 0; i < keys.length - 1; i++) {
    const a = keys[i], b = keys[i + 1];
    if (t >= a.t && t <= b.t) {
      if (b.t === a.t) return b.v;
      if (a.ease === 'step') return t >= b.t ? b.v : a.v;
      return a.v + (b.v - a.v) * ease(a.ease, (t - a.t) / (b.t - a.t));
    }
  }
  return keys[keys.length - 1].v;
}
function ease(type, f) {
  switch (type) {
    case 'ease-in': return f * f;
    case 'ease-out': return 1 - (1 - f) * (1 - f);
    case 'ease-in-out': return f < .5 ? 2 * f * f : 1 - Math.pow(-2 * f + 2, 2) / 2;
    case 'back': { const c1 = 1.70158, c3 = c1 + 1; return 1 + c3 * Math.pow(f - 1, 3) + c1 * Math.pow(f - 1, 2); }
    case 'bounce': { const n1 = 7.5625, d1 = 2.75; let x = f; if (x < 1 / d1) return n1 * x * x; if (x < 2 / d1) return n1 * (x -= 1.5 / d1) * x + .75; if (x < 2.5 / d1) return n1 * (x -= 2.25 / d1) * x + .9375; return n1 * (x -= 2.625 / d1) * x + .984375; }
    default: return f;
  }
}
const curAnim = () => doc().animations.find(a => a.id === S.anim.id) || null;
function track(a, elId, prop, create = false) {
  let t = a.tracks.find(t => t.element === elId && t.property === prop);
  if (!t && create) { t = {element: elId, property: prop, keys: []}; a.tracks.push(t); }
  return t;
}
/** Value of an element as shown in the editor right now. */
function vals(e) {
  const v = {...e};
  const a = S.dock ? curAnim() : null;
  if (a) for (const t of a.tracks.filter(t => t.element === e.id && t.keys.length)) {
    const p = t.property === 'color' ? (['text', 'button', 'input'].includes(e.type) ? 'textColor' : 'fill') : t.property;
    v[p] = COLOR_PROPS.includes(t.property) ? sampleColor(t.keys, S.anim.t) : sampleKeys(t.keys, S.anim.t);
  }
  return v;
}
/** Sets a property; with auto-key on and an animation selected, writes a keyframe instead. */
function setProp(e, prop, value) {
  const a = curAnim();
  if (S.dock && a && S.anim.auto && ANIM_PROPS.includes(prop)) {
    if (COLOR_PROPS.includes(prop)) value = parseInt(String(value).replace('#', ''), 16) || 0;
    const t = track(a, e.id, prop, true);
    const k = t.keys.find(k => k.t === Math.round(S.anim.t));
    if (k) k.v = value; else { t.keys.push({t: Math.round(S.anim.t), v: value, ease: 'linear'}); t.keys.sort((x, y) => x.t - y.t); }
  } else e[prop] = value;
}
function isKeyed(e, prop) {
  const a = S.dock ? curAnim() : null;
  const t = a && track(a, e.id, prop);
  return !!(t && t.keys.length);
}

/* ============================================================== canvas rendering */
const stage = $('#stage'), board = $('#board');
function anchorOrigin(a) {
  const x = a.includes('left') ? '0%' : a.includes('right') ? '100%' : '50%';
  const y = a.startsWith('top') ? '0%' : a.startsWith('bottom') ? '100%' : '50%';
  return x + ' ' + y;
}
function elNode(e, v, opts = {}) {
  const hov = opts.hover;
  const d = h('div', {class: 'el', 'data-id': e.id});
  const sc = (v.scale ?? 1) * (hov ? e.hoverScale || 1 : 1);
  Object.assign(d.style, {left: v.x + 'px', top: v.y + 'px', width: Math.max(0, v.w) + 'px', height: Math.max(0, v.h) + 'px',
    opacity: clamp(v.opacity, 0, 1), transformOrigin: anchorOrigin(e.anchor || 'center'),
    transform: `rotate(${v.rotation || 0}deg) scale(${sc})`, transition: opts.preview ? 'transform .12s, opacity .12s' : ''});
  if (!v.visible && !opts.preview) d.classList.add('hidden-el');
  if (!v.visible && opts.preview) d.style.display = 'none';
  const act = opts.preview ? !!v.active : isActiveTab(e);
  const sprite = hov && e.hoverSprite ? e.hoverSprite : act && e.activeSprite ? e.activeSprite : (v.sprite ?? e.sprite);
  const fill = hov && e.hoverFill ? e.hoverFill : act && e.activeFill ? e.activeFill : (v.fill ?? e.fill);
  const surl = id => (opts.sprites && opts.sprites[id]) || spriteUrl(id);
  if (e.type === 'item' || e.type === 'head' || e.type === 'model') {
    if (e.fillEnabled) d.append(h('div', {class: 'bg', style: {background: fill}}));
    const owner = resolve(v.headOwner ?? e.headOwner, opts.vars || PV_VARS) || (opts.preview ? 'Silent' : '');
    const itemId = String(v.item ?? e.item ?? '');
    const url = e.type === 'item' ? (/^\{/.test(itemId) ? '' : iconUrl(itemId, 64)) : e.type === 'head' ? headUrl(owner || 'Steve', 64) : bodyUrl(owner || 'Steve', 128);
    if (url) {
      const img = h('img', {src: url, alt: '', draggable: 'false', style: {position: 'absolute', inset: 0, width: '100%', height: '100%', objectFit: 'contain', objectPosition: e.type === 'model' ? 'center bottom' : 'center', imageRendering: 'pixelated', pointerEvents: 'none', filter: e.glint ? 'drop-shadow(0 0 3px rgba(190,140,255,.9))' : ''}});
      d.append(img);
    } else {
      d.append(h('div', {class: 'ph', html: e.type === 'item' ? `${svg('item', 'style="width:60%;height:60%"')}` : e.type === 'model' ? svg('model', 'style="width:70%;height:70%"') : headSvg(),
        title: e.type === 'item' ? itemId + (itemId.startsWith('{') ? ' (from the viewer)' : ' (connect to a server for icons)') : 'Player'}));
    }
    return d;
  }
  if (sprite && surl(sprite)) d.append(h('div', {class: 'bg', style: {backgroundImage: `url("${surl(sprite)}")`}}));
  else if (e.fillEnabled) d.append(h('div', {class: 'bg', style: {background: fill}}));
  if (e.type === 'progress') {
    const vars = opts.vars || PV_VARS;
    const rawV = resolve(v.value ?? e.value, vars), mx = Number(resolve(v.max ?? e.max, vars)) || 100;
    const vv = rawV.trim() === '' || isNaN(Number(rawV)) ? (opts.preview ? 0 : mx * 0.6) : Number(rawV);
    const f = clamp(vv / mx, 0, 1);
    const fb = h('div', {class: 'fillbar', style: {width: (f * 100) + '%'}});
    if (e.barSprite && surl(e.barSprite)) fb.style.backgroundImage = `url("${surl(e.barSprite)}")`;
    else fb.style.background = e.barColor;
    d.append(fb);
  }
  let text = v.text ?? e.text;
  let ph = false;
  if (e.type === 'input' && !text) { text = e.placeholder; ph = true; }
  if (text && e.type !== 'image') {
    const tc = ph ? '#9a9a9a' : (hov && e.hoverTextColor ? e.hoverTextColor : act && e.activeTextColor ? e.activeTextColor : (v.textColor ?? e.textColor));
    const pad = e.type === 'text' ? 0 : 6;
    const l = h('div', {class: 'lbl'}, h('span', {html: mmToHtml(resolve(text, opts.vars || PV_VARS))}));
    if (doc().type === 'hud' || (PV && PV.d.type === 'hud')) l.style.whiteSpace = 'pre';
    Object.assign(l.style, {color: tc, fontSize: (9 * e.textSize) + 'px', lineHeight: (10 * e.textSize) + 'px',
      justifyContent: e.align === 'left' ? 'flex-start' : e.align === 'right' ? 'flex-end' : 'center',
      textAlign: e.align, padding: `0 ${pad}px`, textShadow: e.shadow ? `${e.textSize}px ${e.textSize}px 0 rgba(0,0,0,.55)` : 'none'});
    d.append(l);
  }
  return d;
}
function headSvg() {
  return `<svg viewBox="0 0 8 8" style="width:100%;height:100%;image-rendering:pixelated" shape-rendering="crispEdges">
  <rect width="8" height="8" fill="#6b4a32"/><rect y="0" width="8" height="3" fill="#3b2a1e"/><rect y="3" width="8" height="5" fill="#c99f7c"/>
  <rect x="1" y="4" width="2" height="1" fill="#fff"/><rect x="2" y="4" width="1" height="1" fill="#4b3bbf"/><rect x="5" y="4" width="2" height="1" fill="#fff"/><rect x="5" y="4" width="1" height="1" fill="#4b3bbf"/>
  <rect x="3" y="6" width="2" height="1" fill="#8a5a44"/></svg>`;
}
function renderCanvas() {
  const d = doc();
  board.style.transform = `translate(${S.panX}px,${S.panY}px) scale(${S.zoom})`;
  board.style.setProperty('--inv', 1 / S.zoom);
  const art = h('div', {class: 'artboard', style: {width: d.canvas.width + 'px', height: d.canvas.height + 'px'}});
  const lbl = h('div', {class: 'artboard-label', style: {transform: `scale(${1 / S.zoom})`, transformOrigin: '0 100%'}},
    `${d.name} · ${d.type.toUpperCase()} · ${d.canvas.width}×${d.canvas.height}`);
  art.append(lbl);
  if (d.type === 'hud') art.append(h('div', {class: 'hudmid'}));
  if (d.type === 'logic') art.append(h('div', {class: 'logic-note'}, h('div', {}, h('b', {}, 'Logic file'), 'Nothing is drawn. Open Commands to add commands, and Scripts for functions.')));
  for (const e of d.elements) if (!offPage(e)) art.append(elNode(e, vals(e)));
  if (d.type === 'chest') renderChestSlots(d, art, false);
  // hover outline
  if (S.hover && !S.sel.includes(S.hover)) {
    const e = byId(S.hover);
    if (e) art.append(boxFor(e, 'hoverbox'));
  }
  // selection
  const sel = selected();
  if (sel.length === 1) {
    const e = sel[0], b = boxFor(e, 'sel');
    if (!e.locked) {
      for (const [hx, hy] of [[0, 0], [.5, 0], [1, 0], [0, .5], [1, .5], [0, 1], [.5, 1], [1, 1]]) {
        b.append(h('div', {class: 'handle', 'data-h': hx + ',' + hy, style: {left: hx * 100 + '%', top: hy * 100 + '%', cursor: cursorFor(hx, hy, vals(e).rotation)}}));
      }
      b.append(h('div', {class: 'rotline'}));
      b.append(h('div', {class: 'handle rot', 'data-h': 'rot', style: {left: '50%', top: `calc(-18px * var(--inv))`, cursor: 'grab'}}));
    }
    art.append(b);
  } else if (sel.length > 1) {
    for (const e of sel) art.append(boxFor(e, 'sel multi'));
  }
  for (const g of S.guides) {
    art.append(h('div', {class: 'guide', style: g.v ? {left: g.p + 'px', top: '-2000px', width: `calc(1px * var(--inv))`, height: '6000px'} :
        {top: g.p + 'px', left: '-2000px', height: `calc(1px * var(--inv))`, width: '6000px'}}));
  }
  if (S.marquee) {
    const m = S.marquee;
    art.append(h('div', {class: 'marquee', style: {left: Math.min(m.x0, m.x1) + 'px', top: Math.min(m.y0, m.y1) + 'px', width: Math.abs(m.x1 - m.x0) + 'px', height: Math.abs(m.y1 - m.y0) + 'px'}}));
  }
  board.replaceChildren(art);
  $('#zoomLbl').textContent = Math.round(S.zoom * 100) + '%';
}
function boxFor(e, cls) {
  const v = vals(e);
  return h('div', {class: cls, style: {left: v.x + 'px', top: v.y + 'px', width: v.w + 'px', height: v.h + 'px', transformOrigin: anchorOrigin(e.anchor),
    transform: `rotate(${v.rotation}deg) scale(${v.scale})`}});
}
function cursorFor(hx, hy, rot) {
  if (hx === .5 && hy === .5) return 'move';
  let ang = Math.atan2(hy - .5, hx - .5) * 180 / Math.PI + (rot || 0);
  ang = ((ang % 180) + 180) % 180;
  return ang < 22.5 || ang >= 157.5 ? 'ew-resize' : ang < 67.5 ? 'nwse-resize' : ang < 112.5 ? 'ns-resize' : 'nesw-resize';
}
function toCanvas(ev) {
  const r = stage.getBoundingClientRect();
  return {x: (ev.clientX - r.left - S.panX) / S.zoom, y: (ev.clientY - r.top - S.panY) / S.zoom};
}
function hitTest(p) {
  const list = doc().elements;
  for (let i = list.length - 1; i >= 0; i--) {
    const e = list[i];
    if (e.locked || offPage(e)) continue;
    const v = vals(e);
    if (!v.visible && !S.showHidden) { /* hidden elements are still selectable via outline */ }
    const pv = pivotOf(v, e.anchor), sc = v.scale || 1;
    let dx = p.x - pv[0], dy = p.y - pv[1];
    if (v.rotation) { const r = -v.rotation * Math.PI / 180, c = Math.cos(r), s = Math.sin(r); [dx, dy] = [dx * c - dy * s, dx * s + dy * c]; }
    const lx = pv[0] + dx / sc, ly = pv[1] + dy / sc;
    if (lx >= v.x && lx <= v.x + v.w && ly >= v.y && ly <= v.y + v.h) return e;
  }
  return null;
}
function pivotOf(v, a = 'center') {
  let px = v.x + v.w / 2, py = v.y + v.h / 2;
  if (a.includes('left')) px = v.x; if (a.includes('right')) px = v.x + v.w;
  if (a.startsWith('top')) py = v.y; if (a.startsWith('bottom')) py = v.y + v.h;
  return [px, py];
}
function fit() {
  S.autoFit = true;
  const r = stage.getBoundingClientRect(), d = doc();
  const z = Math.min((r.width - 80) / d.canvas.width, (r.height - 80) / d.canvas.height);
  S.zoom = clamp(z, .1, 8);
  S.panX = (r.width - d.canvas.width * S.zoom) / 2;
  S.panY = (r.height - d.canvas.height * S.zoom) / 2;
  renderCanvas();
  renderStatus();
}
function zoomAt(f, cx, cy) {
  S.autoFit = false;
  const nz = clamp(S.zoom * f, .1, 12);
  const r = stage.getBoundingClientRect();
  if (cx == null) { cx = r.width / 2; cy = r.height / 2; }
  S.panX = cx - (cx - S.panX) * (nz / S.zoom);
  S.panY = cy - (cy - S.panY) * (nz / S.zoom);
  S.zoom = nz;
  renderCanvas();
  renderStatus();
}

/* ============================================================== canvas interaction */
let drag = null;
stage.addEventListener('wheel', ev => {
  ev.preventDefault();
  const r = stage.getBoundingClientRect();
  if (ev.ctrlKey || ev.metaKey) zoomAt(Math.exp(-ev.deltaY * 0.0022), ev.clientX - r.left, ev.clientY - r.top);
  else { S.panX -= ev.deltaX; S.panY -= ev.deltaY; renderCanvas(); }
}, {passive: false});
stage.addEventListener('pointermove', ev => {
  if (drag) return;
  const e = hitTest(toCanvas(ev));
  const id = e ? e.id : null;
  if (id !== S.hover) { S.hover = id; renderCanvas(); }
});
stage.addEventListener('pointerleave', () => { if (S.hover) { S.hover = null; renderCanvas(); } });
stage.addEventListener('pointerdown', ev => {
  if (ev.target.closest('.pages,.zoomctl,#emptyCanvas,button,input,select')) return;
  if (ev.button === 1 || (ev.button === 0 && S.spaceDown)) {
    S.autoFit = false;
    drag = {kind: 'pan', sx: ev.clientX, sy: ev.clientY, px: S.panX, py: S.panY};
    stage.classList.add('panning');
    stage.setPointerCapture(ev.pointerId);
    return;
  }
  if (ev.button !== 0) return;
  const p = toCanvas(ev);
  const handle = ev.target.closest('.handle');
  stage.setPointerCapture(ev.pointerId);
  if (handle && selected().length === 1) {
    const e = selected()[0];
    const kind = handle.dataset.h === 'rot' ? 'rotate' : 'resize';
    const [hx, hy] = kind === 'resize' ? handle.dataset.h.split(',').map(Number) : [0, 0];
    drag = {kind, e, hx, hy, start: p, orig: {...vals(e)}, snap: snapshot()};
    return;
  }
  if (S.tool !== 'select' && TYPES.includes(S.tool)) {
    drag = {kind: 'create', type: S.tool, start: p, cur: p};
    return;
  }
  const e = hitTest(p);
  if (e) {
    const now = performance.now();
    const dbl = !ev.shiftKey && S._lastDown && S._lastDown.id === e.id && now - S._lastDown.t < 400;
    S._lastDown = {id: e.id, t: now};
    if (dbl) { S._lastDown = null; stage.releasePointerCapture?.(ev.pointerId); handleDouble(e); return; }
    const g = selGroup(e);
    const members = g && !(ev.ctrlKey || ev.metaKey) ? groupMembers(g).filter(x => !x.locked).map(x => x.id) : [e.id];
    if (ev.shiftKey) S.sel = S.sel.includes(e.id) ? S.sel.filter(i => !members.includes(i)) : [...new Set([...S.sel, ...members])];
    else if (!S.sel.includes(e.id)) S.sel = members;
    if (ev.altKey) { // alt-drag duplicates
      pushUndo();
      const source = selected(), copies = source.map(o => Object.assign(clone(o), {id: rid(), name: o.name + ' copy'}));
      doc().script.nodes.push(...cloneQuickActions(source, copies));
      doc().elements.push(...copies);
      S.sel = copies.map(c => c.id);
      drag = {kind: 'move', start: p, origs: selected().map(o => ({e: o, ...vals(o)})), snap: null};
    } else {
      drag = {kind: 'move', start: p, origs: selected().map(o => ({e: o, ...vals(o)})), snap: snapshot()};
    }
    render();
  } else {
    if (!ev.shiftKey) S.sel = [];
    S.marquee = {x0: p.x, y0: p.y, x1: p.x, y1: p.y};
    drag = {kind: 'marquee'};
    render();
  }
});
stage.addEventListener('pointermove', ev => {
  if (!drag) return;
  if (drag.kind === 'pan') {
    S.panX = drag.px + ev.clientX - drag.sx; S.panY = drag.py + ev.clientY - drag.sy;
    renderCanvas(); return;
  }
  const p = toCanvas(ev);
  if (drag.kind === 'marquee') {
    S.marquee.x1 = p.x; S.marquee.y1 = p.y;
    const m = S.marquee, x0 = Math.min(m.x0, m.x1), x1 = Math.max(m.x0, m.x1), y0 = Math.min(m.y0, m.y1), y1 = Math.max(m.y0, m.y1);
    S.sel = els().filter(e => !e.locked && e.x < x1 && e.x + e.w > x0 && e.y < y1 && e.y + e.h > y0).map(e => e.id);
    renderCanvas(); return;
  }
  if (drag.kind === 'create') { drag.cur = p; S.marquee = {x0: drag.start.x, y0: drag.start.y, x1: p.x, y1: p.y}; renderCanvas(); return; }
  if (drag.kind === 'move') {
    let dx = p.x - drag.start.x, dy = p.y - drag.start.y;
    if (ev.shiftKey) { if (Math.abs(dx) > Math.abs(dy)) dy = 0; else dx = 0; }
    const bb = bbox(drag.origs.map(o => ({x: o.x + dx, y: o.y + dy, w: o.w, h: o.h})));
    const sn = snapBox(bb, drag.origs.map(o => o.e.id));
    dx += sn.dx; dy += sn.dy; S.guides = sn.guides;
    for (const o of drag.origs) { setProp(o.e, 'x', Math.round(o.x + dx)); setProp(o.e, 'y', Math.round(o.y + dy)); }
    renderCanvas(); renderPanelLive(); return;
  }
  if (drag.kind === 'resize') {
    const o = drag.orig, e = drag.e;
    let dx = p.x - drag.start.x, dy = p.y - drag.start.y;
    if (o.rotation) { const r = -o.rotation * Math.PI / 180, c = Math.cos(r), s = Math.sin(r); [dx, dy] = [dx * c - dy * s, dx * s + dy * c]; }
    dx /= o.scale || 1; dy /= o.scale || 1;
    let x = o.x, y = o.y, w = o.w, hh = o.h;
    if (drag.hx === 0) { x = o.x + dx; w = o.w - dx; } else if (drag.hx === 1) w = o.w + dx;
    if (drag.hy === 0) { y = o.y + dy; hh = o.h - dy; } else if (drag.hy === 1) hh = o.h + dy;
    if (ev.shiftKey && drag.hx !== .5 && drag.hy !== .5) { // keep ratio
      const ratio = o.w / o.h;
      if (Math.abs(w / o.w) > Math.abs(hh / o.h)) { const nh = w / ratio; if (drag.hy === 0) y -= nh - hh; hh = nh; }
      else { const nw = hh * ratio; if (drag.hx === 0) x -= nw - w; w = nw; }
    }
    if (w < 1) { w = 1; } if (hh < 1) { hh = 1; }
    setProp(e, 'x', Math.round(x)); setProp(e, 'y', Math.round(y)); setProp(e, 'w', Math.round(w)); setProp(e, 'h', Math.round(hh));
    renderCanvas(); renderPanelLive(); return;
  }
  if (drag.kind === 'rotate') {
    const o = drag.orig, pv = pivotOf(o, drag.e.anchor);
    let ang = Math.atan2(p.y - pv[1], p.x - pv[0]) * 180 / Math.PI + 90;
    if (ev.shiftKey) ang = Math.round(ang / 15) * 15;
    ang = Math.round(((ang + 540) % 360) - 180);
    setProp(drag.e, 'rotation', ang);
    renderCanvas(); renderPanelLive();
  }
});
const endDrag = ev => {
  if (!drag) return;
  const d = drag; drag = null;
  stage.classList.remove('panning');
  S.guides = [];
  if (d.kind === 'create') {
    const a = d.start, b = d.cur || d.start;
    const def = defaultsFor(d.type);
    let x = Math.round(Math.min(a.x, b.x)), y = Math.round(Math.min(a.y, b.y)), w = Math.round(Math.abs(b.x - a.x)), hh = Math.round(Math.abs(b.y - a.y));
    if (w < 4 || hh < 4) { w = def.w; hh = def.h; x = Math.round(a.x - w / 2); y = Math.round(a.y - hh / 2); }
    S.marquee = null;
    addElement(d.type, {x, y, w, h: hh});
    if (!ev.shiftKey) setTool('select');
    return;
  }
  if (d.kind === 'marquee') { S.marquee = null; render(); return; }
  if ((d.kind === 'move' || d.kind === 'resize' || d.kind === 'rotate')) {
    const changed = snapshot() !== d.snap;
    if (d.snap && changed) pushUndo(d.snap);
    afterChange();
  }
  renderCanvas();
};
stage.addEventListener('pointerup', endDrag);
stage.addEventListener('pointercancel', endDrag);
/** Double click: step into a group, or jump to the text box of a text element. */
function handleDouble(e) {
  if (selGroup(e) && S.sel.length > 1 && S.sel.includes(e.id)) { S.sel = [e.id]; render(); return; }
  if (e.type !== 'image' && e.type !== 'item' && e.type !== 'head' && e.type !== 'rect' && e.type !== 'model') {
    S.sel = [e.id]; S.side = 'design'; render();
    setTimeout(() => { const t = $('#f_text'); if (t) { t.focus(); t.select(); } }, 0);
  } else { S.sel = [e.id]; render(); }
}
function bbox(list) {
  const x0 = Math.min(...list.map(b => b.x)), y0 = Math.min(...list.map(b => b.y));
  const x1 = Math.max(...list.map(b => b.x + b.w)), y1 = Math.max(...list.map(b => b.y + b.h));
  return {x: x0, y: y0, w: x1 - x0, h: y1 - y0};
}
function snapBox(bb, ignore) {
  const d = doc(), th = 5 / S.zoom;
  const xs = [0, d.canvas.width / 2, d.canvas.width], ys = [0, d.canvas.height / 2, d.canvas.height];
  for (const e of d.elements) if (!ignore.includes(e.id) && e.visible) { const v = vals(e); xs.push(v.x, v.x + v.w / 2, v.x + v.w); ys.push(v.y, v.y + v.h / 2, v.y + v.h); }
  let best = {dx: 0, gx: null, ax: th + 1}, bestY = {dy: 0, gy: null, ay: th + 1};
  for (const cand of [bb.x, bb.x + bb.w / 2, bb.x + bb.w]) for (const t of xs) { const a = Math.abs(t - cand); if (a < best.ax) best = {dx: t - cand, gx: t, ax: a}; }
  for (const cand of [bb.y, bb.y + bb.h / 2, bb.y + bb.h]) for (const t of ys) { const a = Math.abs(t - cand); if (a < bestY.ay) bestY = {dy: t - cand, gy: t, ay: a}; }
  const guides = [];
  if (best.gx != null) guides.push({v: true, p: best.gx});
  if (bestY.gy != null) guides.push({v: false, p: bestY.gy});
  return {dx: best.gx != null ? best.dx : 0, dy: bestY.gy != null ? bestY.dy : 0, guides};
}
function addElement(type, props = {}) {
  const e = Object.assign(defaultsFor(type), {id: rid()}, props);
  const count = els().filter(x => x.type === type).length + 1;
  e.name = defaultsFor(type).name + (count > 1 ? ' ' + count : '');
  if (type === 'image' && !e.sprite && doc().sprites.length) e.sprite = doc().sprites[0].id;
  mutate(d => d.elements.push(e));
  S.sel = [e.id];
  render();
  return e;
}
/* ============================================================== inspector panel */
const panel = $('#panel');
function field(label, input) {
  const control = input.matches?.('input,select,textarea') ? input : input.querySelector?.('input,select,textarea');
  if (control && !control.id) control.id = rid('field');
  if (control && !control.hasAttribute('aria-label')) control.setAttribute('aria-label', label);
  return h('div', {class: 'row'}, h('label', {for: control?.id}, label), input);
}
/** Numeric field with drag-to-scrub label. */
function numInput(label, get, set, opts = {}) {
  const wrap = h('div', {class: 'num' + (opts.keyed ? ' keyed' : '')});
  const b = h('b', {title: 'Drag to change'}, label);
  const inp = h('input', {type: 'text', inputmode: 'decimal', value: round(get(), 2), id: opts.id});
  const commit = v => { const n = Number(v); if (!Number.isFinite(n)) return; mutate(() => set(opts.min != null ? Math.max(opts.min, n) : n)); };
  inp.addEventListener('change', () => commit(inp.value));
  inp.addEventListener('keydown', ev => {
    if (ev.key === 'ArrowUp' || ev.key === 'ArrowDown') {
      ev.preventDefault();
      const step = (opts.step || 1) * (ev.shiftKey ? 10 : 1) * (ev.key === 'ArrowUp' ? 1 : -1);
      commit(round(Number(inp.value) + step, 3));
    }
  });
  b.addEventListener('pointerdown', ev => {
    const sx = ev.clientX, start = Number(get()) || 0, snap = snapshot();
    b.setPointerCapture(ev.pointerId);
    const mv = e2 => { const n = round(start + (e2.clientX - sx) * (opts.step || 1), 3); set(opts.min != null ? Math.max(opts.min, n) : n); inp.value = round(get(), 2); renderCanvas(); };
    const up = () => { b.removeEventListener('pointermove', mv); b.removeEventListener('pointerup', up); if (snapshot() !== snap) pushUndo(snap); afterChange(); };
    b.addEventListener('pointermove', mv); b.addEventListener('pointerup', up);
  });
  wrap.append(b, inp);
  return wrap;
}
function textInput(get, set, opts = {}) {
  const tag = opts.area ? 'textarea' : 'input';
  const i = h(tag, {class: 'inp', value: get() ?? '', placeholder: opts.ph || '', id: opts.id, spellcheck: 'false'});
  if (opts.area) i.value = get() ?? '';
  i.addEventListener('change', () => mutate(() => set(i.value)));
  if (opts.live) i.addEventListener('input', () => { set(i.value); renderCanvas(); });
  return i;
}
function colorInput(get, set, allowEmpty = false) {
  const val = get() || '';
  const c = h('input', {type: 'color', value: /^#[0-9a-f]{6}$/i.test(val) ? val : '#ffffff'});
  const t = h('input', {class: 'inp', value: val, placeholder: allowEmpty ? 'none' : '#ffffff', spellcheck: 'false'});
  let before = null;
  c.addEventListener('input', () => { before ??= snapshot(); t.value = c.value; set(c.value); renderCanvas(); });
  c.addEventListener('change', () => { pushUndo(before ?? snapshot()); before = null; set(c.value); afterChange(); });
  t.addEventListener('change', () => mutate(() => set(t.value.trim())));
  return h('div', {class: 'color'}, c, t);
}
function checkInput(label, get, set) {
  const i = h('input', {type: 'checkbox'}); i.checked = !!get();
  i.addEventListener('change', () => mutate(() => set(i.checked)));
  return h('label', {class: 'check'}, i, label);
}
function selectInput(opts, get, set, id) {
  const s = h('select', {class: 'inp', id});
  for (const o of opts) { const [v, l] = Array.isArray(o) ? o : [o, o]; const op = h('option', {value: v}, l); if (String(get()) === String(v)) op.selected = true; s.append(op); }
  s.addEventListener('change', () => mutate(() => set(s.value)));
  return s;
}
function spriteSelect(get, set, id) {
  return selectInput([['', '— none —'], ...doc().sprites.map(s => [s.id, `${s.id} (${s.width}×${s.height})`])], get, set, id);
}
function seg(opts, get, set) {
  const s = h('div', {class: 'seg'});
  for (const [v, l] of opts) s.append(h('button', {class: get() === v ? 'on' : '', onclick: () => mutate(() => set(v))}, l));
  return s;
}
function sec(title, ...kids) { return h('div', {class: 'sec'}, h('h4', {}, title), ...kids); }

function renderPanel() {
  if (S.side === 'layers') { S.nav = 'layers'; S.side = 'props'; }
  if (S.side === 'design') S.side = 'props';
  $$('#sidetabs button').forEach(b => b.classList.toggle('on', b.dataset.t === S.side));
  $('#sidetabs [data-t="slots"]').hidden = doc().type !== 'chest';
  const sel = selected();
  let content;
  if (S.side === 'slots' && doc().type === 'chest') content = chestPanel();
  else if (S.side === 'settings') content = docPanel();
  else if (!sel.length) content = docPanel();
  else if (sel.length > 1) content = multiPanel(sel);
  else content = inspectElement(sel[0]);
  const scroll = panel.scrollTop;
  panel.replaceChildren(content);
  panel.scrollTop = scroll;
  if (doc().type === 'chest') renderCanvas();
}
/** Cheap refresh of numeric fields during drags. */
function renderPanelLive() {
  const e = selected()[0];
  if (!e || S.side !== 'props') return;
  const v = vals(e);
  for (const k of ['x', 'y', 'w', 'h', 'rotation', 'scale', 'opacity']) { const i = $('#p_' + k); if (i && document.activeElement !== i) i.value = round(v[k], 2); }
}
function propsPanel(e) {
  const v = vals(e);
  const N = (k, l, o = {}) => numInput(l, () => vals(e)[k], n => setProp(e, k, n), {id: 'p_' + k, keyed: isKeyed(e, k), ...o});
  const anchorGrid = h('div', {class: 'anchor'});
  for (const a of ['top-left', 'top', 'top-right', 'left', 'center', 'right', 'bottom-left', 'bottom', 'bottom-right']) {
    anchorGrid.append(h('button', {class: e.anchor === a ? 'on' : '', title: a, onclick: () => mutate(() => e.anchor = a)}));
  }
  return h('div', {},
    sec('Layout',
      h('div', {class: 'row2'}, N('x', 'X'), N('y', 'Y')),
      h('div', {class: 'row2'}, N('w', 'W', {min: 1}), N('h', 'H', {min: 1})),
      h('div', {class: 'row2'}, N('rotation', '↻'), N('scale', 'S', {step: .01, min: 0})),
      h('div', {class: 'row2'}, N('opacity', 'α', {step: .01, min: 0}), h('div'))),
    sec('General',
      field('Name', textInput(() => e.name, x => e.name = x)),
      field('Type', h('div', {class: 'inp', style: {opacity: .7}}, e.type)),
      field('ID', textInput(() => e.id, x => renameId(e, x)))),
    sec('Behavior',
      checkInput('Visible', () => e.visible, x => e.visible = x),
      checkInput('Blocked (ignores the cursor)', () => e.blocked, x => e.blocked = x),
      checkInput('Locked in editor', () => e.locked, x => e.locked = x)),
    sec('Anchor', h('div', {style: {display: 'flex', gap: '14px', alignItems: 'center'}}, anchorGrid,
      h('p', {style: {margin: 0, color: 'var(--faint)', fontSize: '12px'}}, 'Pivot for rotation and scale. ', h('br'), 'Now: ', e.anchor))),
    sec('Arrange', h('div', {style: {display: 'flex', gap: '6px', flexWrap: 'wrap'}},
      h('button', {class: 'btn sm', onclick: () => arrange('top')}, 'To front'),
      h('button', {class: 'btn sm', onclick: () => arrange('up')}, 'Forward'),
      h('button', {class: 'btn sm', onclick: () => arrange('down')}, 'Backward'),
      h('button', {class: 'btn sm', onclick: () => arrange('bottom')}, 'To back'),
      h('button', {class: 'btn sm', onclick: () => alignSel('cx')}, 'Centre X'),
      h('button', {class: 'btn sm', onclick: () => alignSel('cy')}, 'Centre Y'),
      h('button', {class: 'btn sm', onclick: duplicate}, 'Duplicate'),
      h('button', {class: 'btn sm danger', onclick: removeSel}, 'Delete')))
  );
}
function renameId(e, raw) {
  const id = slug(raw);
  if (!id || id === e.id) return;
  if (byId(id)) return toast('That ID is already used', 'err');
  const old = e.id;
  e.id = id;
  for (const a of doc().animations) for (const t of a.tracks) if (t.element === old) t.element = id;
  for (const n of doc().script.nodes) if (n.props.element === old) n.props.element = id;
  const remap = {[`quick_event_${old}`]: `quick_event_${id}`, [`quick_action_${old}`]: `quick_action_${id}`};
  for (const n of doc().script.nodes) {
    if (remap[n.id]) n.id = remap[n.id];
    for (const port of Object.keys(n.next || {})) n.next[port] = n.next[port].map(target => remap[target] || target);
  }
  S.sel = S.sel.map(s => s === old ? id : s);
}
function designPanel(e) {
  const parts = [];
  const hasText = !['image', 'item', 'head', 'progress', 'model'].includes(e.type);
  const is3d = ['item', 'head', 'model'].includes(e.type);
  if (e.type === 'item') parts.push(sec('Item',
    h('div', {style: {display: 'flex', gap: '10px', alignItems: 'center', marginBottom: '8px'}},
      h('div', {style: {width: '48px', height: '48px', background: 'var(--stage)', borderRadius: '8px', display: 'grid', placeItems: 'center', flex: 'none'}},
        iconUrl(e.item) && !String(e.item).startsWith('{') ? h('img', {src: iconUrl(e.item, 64), width: 40, height: 40, style: {imageRendering: 'pixelated'}}) : h('span', {class: 'lab'}, '?')),
      h('div', {style: {flex: 1, minWidth: 0}}, textInput(() => e.item, x => e.item = x.trim(), {ph: 'minecraft:diamond_sword'}))),
    h('div', {class: 'toolbar', style: {marginBottom: '8px'}},
      h('button', {class: 'btn sm', onclick: () => pickItem(id => mutate(() => e.item = id))}, 'Pick item…'),
      ...[['{hand}', 'Held item'], ['{helmet}', 'Helmet'], ['{chestplate}', 'Chestplate']].map(([v, l]) => h('button', {class: 'btn sm ghost', title: "Shows the viewer's " + l.toLowerCase(), onclick: () => mutate(() => e.item = v)}, l))),
    field('Item model', textInput(() => e.itemModel, x => e.itemModel = x.trim(), {ph: 'optional, e.g. myserver:ruby'})),
    checkInput('Enchantment glint', () => e.glint, x => e.glint = x)));
  if (e.type === 'head' || e.type === 'model') parts.push(sec(e.type === 'head' ? 'Head' : 'Player model',
    h('div', {style: {display: 'flex', gap: '10px', alignItems: 'center', marginBottom: '8px'}},
      connected() ? h('img', {src: e.type === 'head' ? headUrl(e.headOwner || 'Steve', 64) : bodyUrl(e.headOwner || 'Steve', 96), height: 56, style: {imageRendering: 'pixelated', background: 'var(--stage)', borderRadius: '8px', padding: '4px'}}) : null,
      h('div', {style: {flex: 1, minWidth: 0}}, textInput(() => e.headOwner, x => e.headOwner = x.trim(), {ph: 'blank = the viewer'}))),
    e.type === 'model' ? h('p', {style: {margin: 0, color: 'var(--faint)', fontSize: '11.5px'}}, 'A full 3D player (mannequin, Minecraft 1.21.9+). Older servers show a head instead.') : null));
  if (is3d) parts.push(sec('Motion',
    h('div', {class: 'row2'}, numInput('Spin', () => e.spin, x => e.spin = clamp(x, -340, 340), {step: 1}), h('span', {class: 'lab'}, '° per second')),
    checkInput('Scroll over it to rotate', () => e.rotatable, x => e.rotatable = x)));
  if (!is3d || e.fillEnabled) parts.push(sec('Fill',
    checkInput('Fill colour', () => e.fillEnabled, x => e.fillEnabled = x),
    field('Colour', colorInput(() => vals(e).fill, x => setProp(e, 'fill', x))),
    field('Sprite', spriteSelect(() => e.sprite, x => e.sprite = x, 'f_sprite')),
    h('p', {style: {margin: '2px 0 0', color: 'var(--faint)', fontSize: '11.5px'}}, 'A sprite replaces the colour. Make rounded panels in Sprites → Generate.')));
  if (hasText) parts.push(sec('Text',
    field('Text', textInput(() => e.text, x => e.text = x, {area: true, id: 'f_text', ph: 'Supports <white>colours</white>, <bold>, {player}, %papi%'})),
    h('div', {class: 'row'}, h('span'), h('button', {class: 'btn sm', style: {justifySelf: 'start'}, onclick: () => { S.lastField = $('#f_text'); openPlaceholders(); }}, '{ } Insert placeholder')),
    field('Colour', colorInput(() => vals(e).textColor, x => setProp(e, 'textColor', x))),
    h('div', {class: 'row2'}, numInput('Size', () => e.textSize, x => e.textSize = x, {step: .05, min: .1}), h('div')),
    field('Align', seg([['left', 'Left'], ['center', 'Centre'], ['right', 'Right']], () => e.align, x => e.align = x)),
    checkInput('Text shadow', () => e.shadow, x => e.shadow = x),
    e.type === 'input' ? field('Placeholder', textInput(() => e.placeholder, x => e.placeholder = x)) : null));
  if (e.type === 'progress') parts.push(sec('Progress',
    field('Value', textInput(() => e.value, x => e.value = x, {ph: '{var:xp} or %papi%', id: 'f_value'})),
    h('div', {class: 'row'}, h('span'), h('button', {class: 'btn sm', style: {justifySelf: 'start'}, onclick: () => { S.lastField = $('#f_value'); openPlaceholders(); }}, '{ } Insert placeholder')),
    field('Max', textInput(() => e.max, x => e.max = x)),
    field('Bar colour', colorInput(() => e.barColor, x => e.barColor = x)),
    field('Bar sprite', spriteSelect(() => e.barSprite, x => e.barSprite = x))));
  const sets = tabsets();
  const pageOpts = [['', '— always visible —']];
  for (const [set, pages] of Object.entries(sets)) for (const pg of pages) pageOpts.push([set + '/' + pg, set + ' › ' + pg]);
  const tabSec = h('details', {class: 'adv', open: !!(e.group || e.tabFor)}, h('summary', {}, 'Tabs & group'), h('div', {class: 'sec'},
    field('On page', selectInput(pageOpts.some(o => o[0] === e.group) ? pageOpts : [...pageOpts, [e.group, e.group]], () => e.group, x => e.group = x)),
    field('Group', textInput(() => e.group, x => e.group = x.trim(), {ph: 'name, or tabset/page'})),
    field('Tab button', selectInput([['', '— not a tab button —'], ...pageOpts.slice(1)], () => e.tabFor, x => e.tabFor = x)),
    e.tabFor ? field('Active fill', colorInput(() => e.activeFill, x => e.activeFill = x, true)) : null,
    e.tabFor ? field('Active text', colorInput(() => e.activeTextColor, x => e.activeTextColor = x, true)) : null,
    e.tabFor ? field('Active sprite', spriteSelect(() => e.activeSprite, x => e.activeSprite = x)) : null,
    h('p', {style: {margin: 0, color: 'var(--faint)', fontSize: '11.5px'}}, 'Tab buttons switch pages by themselves. Make a tab set in the Layers tab.')));
  parts.push(tabSec);
  const hov = h('details', {class: 'adv', open: !!(e.hoverFill || e.hoverSprite || e.hoverScale !== 1)}, h('summary', {}, 'Hover'));
  parts.push(hov);
  hov.append(sec('Hover',
    field('Fill', colorInput(() => e.hoverFill, x => e.hoverFill = x, true)),
    field('Sprite', spriteSelect(() => e.hoverSprite, x => e.hoverSprite = x)),
    hasText ? field('Text', colorInput(() => e.hoverTextColor, x => e.hoverTextColor = x, true)) : null,
    h('div', {class: 'row2'}, numInput('Scale', () => e.hoverScale, x => e.hoverScale = x, {step: .01, min: .1}), h('div')),
    doc().type === 'hud' ? h('p', {style: {margin: 0, color: 'var(--warn)', fontSize: '11.5px'}}, 'HUDs have no cursor, hover settings are ignored.') : null));
  hov.querySelector('.sec h4')?.remove();
  return h('div', {}, ...parts);
}
function multiPanel(list) {
  return h('div', {},
    sec(list.length + ' elements selected',
      h('div', {style: {display: 'flex', gap: '6px', flexWrap: 'wrap'}},
        ...[['left', 'Left'], ['cx', 'Centre X'], ['right', 'Right'], ['top', 'Top'], ['cy', 'Centre Y'], ['bottom', 'Bottom']].map(([k, l]) =>
          h('button', {class: 'btn sm', onclick: () => alignSel(k)}, l)),
        h('button', {class: 'btn sm', onclick: () => distribute('x')}, 'Space evenly ↔'),
        h('button', {class: 'btn sm', onclick: () => distribute('y')}, 'Space evenly ↕'),
        list.some(e => selGroup(e)) ? h('button', {class: 'btn sm', onclick: ungroupSel}, 'Ungroup') : null,
        h('button', {class: 'btn sm primary', onclick: groupSel}, 'Group (Ctrl+G)'),
        h('button', {class: 'btn sm', onclick: duplicate}, 'Duplicate'),
        h('button', {class: 'btn sm danger', onclick: removeSel}, 'Delete'))),
    sec('Batch',
      checkInput('Visible', () => list.every(e => e.visible), x => list.forEach(e => e.visible = x)),
      checkInput('Blocked', () => list.every(e => e.blocked), x => list.forEach(e => e.blocked = x)),
      field('Fill', colorInput(() => list[0].fill, x => list.forEach(e => e.fill = x)))));
}
function docPanel() {
  const d = doc(), st = d.settings;
  const typeHelp = {screen: 'A menu in front of the player, controlled with their crosshair.', hud: 'An overlay while playing. X starts at screen centre; Y starts at the top.', world: 'An interactive display placed in the world.', chest: 'A real inventory menu. Use Slots to configure items and click actions.', logic: 'Reusable commands and functions, without a visual interface.'};
  return h('div', {},
    sec('UI',
      field('Name', textInput(() => d.name, x => d.name = x)),
      field('ID', textInput(() => d.id, x => d.id = slug(x) || d.id)),
      field('Type', selectInput([['screen', 'Screen menu'], ['chest', 'Chest inventory'], ['hud', 'HUD overlay'], ['world', 'World display'], ['logic', 'Logic file']], () => d.type, x => { d.type = x; if (x === 'chest') resizeChest(d); })),
      h('p', {style: {margin: '0', color: 'var(--faint)', fontSize: '12px'}}, typeHelp[d.type])),
    d.type !== 'chest' ? sec('Canvas',
      h('div', {class: 'row2'},
        numInput('W', () => d.canvas.width, x => d.canvas.width = Math.round(clamp(x, 16, 2048)), {min: 16}),
        numInput('H', () => d.canvas.height, x => d.canvas.height = Math.round(clamp(x, 16, 2048)), {min: 16})),
      h('div', {style: {display: 'flex', gap: '6px', flexWrap: 'wrap'}},
        ...[[480, 270], [400, 240], [320, 180], [256, 256]].map(([w, hh]) => h('button', {class: 'btn sm', onclick: () => mutate(() => { d.canvas.width = w; d.canvas.height = hh; })}, w + '×' + hh)))) : sec('Inventory',
      field('Rows', selectInput([1, 2, 3, 4, 5, 6].map(n => [String(n), n + ' rows · ' + n * 9 + ' slots']), () => String(st.rows), x => { st.rows = +x; resizeChest(d); })),
      field('Title', textInput(() => st.title, x => st.title = x, {ph: 'Inventory title'})),
      field('Header', numInput('px', () => st.guiTop, x => { st.guiTop = clamp(Math.round(x), 0, 64); resizeChest(d); }, {min: 0, max: 64})),
      h('button', {class: 'btn', onclick: () => { S.side = 'slots'; renderPanel(); }}, 'Edit inventory slots')),
    sec('Opening',
      field('Command', textInput(() => st.command, x => st.command = x.replace(/^\//, ''), {ph: 'menu (opens this UI)'})),
      field('Permission', textInput(() => st.permission, x => st.permission = x, {ph: 'optional'}))),
    d.type === 'screen' ? sec('Screen',
      checkInput('Freeze movement while open', () => st.freeze, x => st.freeze = x),
      checkInput('Sneak (Shift) closes it', () => st.closeOnSneak, x => st.closeOnSneak = x),
      checkInput('Keep cursor inside the canvas', () => st.clampCursor, x => st.clampCursor = x),
      h('div', {class: 'row2'}, numInput('Dim', () => st.dim, x => st.dim = clamp(x, 0, 1), {step: .01, min: 0}), numInput('Size', () => st.screenSize, x => st.screenSize = clamp(x, .1, 2.5), {step: .01, min: .1})),
      field('Cursor', spriteSelect(() => st.cursorSprite, x => st.cursorSprite = x))) : null,
    d.type === 'hud' ? sec('HUD',
      checkInput('Show automatically on join', () => st.autoShow, x => st.autoShow = x),
      h('p', {style: {margin: 0, color: 'var(--faint)', fontSize: '12px'}}, 'HUD text supports plain ASCII. Item and head elements are not available on HUDs.')) : null,
    d.type === 'world' ? sec('World',
      h('div', {class: 'row2'}, numInput('Scale', () => st.worldScale, x => st.worldScale = clamp(x, .05, 20), {step: .01, min: .05}), h('div')),
      field('Faces', selectInput([['fixed', 'Fixed direction'], ['center', 'Always faces viewer'], ['vertical', 'Turns left / right']], () => st.billboard, x => st.billboard = x)),
      field('View range', numInput('m', () => st.viewRange, x => st.viewRange = clamp(x, 1, 256), {min: 1, max: 256})),
      field('Tilt', numInput('°', () => st.tilt, x => st.tilt = clamp(x, -90, 90), {min: -90, max: 90})),
      checkInput('Separate display for each player', () => st.perPlayer, x => st.perPlayer = x),
      h('p', {style: {margin: 0, color: 'var(--faint)', fontSize: '12px'}}, 'Blocks per 100 pixels. ' + round(d.canvas.width * st.worldScale / 100, 2) + ' × ' + round(d.canvas.height * st.worldScale / 100, 2) + ' blocks.')) : null,
    sec('Commands', h('p', {style: {margin: '0 0 8px', color: 'var(--muted)'}}, (d.commands || []).length ? (d.commands || []).map(c => '/' + c.name).join('  ') : 'No custom commands yet.'),
      h('button', {class: 'btn sm', onclick: openCommands}, 'Commands & functions…')),
    sec('Stats', h('p', {style: {margin: 0, color: 'var(--muted)'}},
      `${d.elements.length} elements · ${d.sprites.length} sprites · ${d.animations.length} animations · ${d.script.nodes.length} script nodes`))
  );
}
function layersPanel() {
  const list = h('ul', {class: 'layers'});
  const query = S.layerQuery.trim().toLowerCase();
  const items = [...doc().elements].reverse().filter(e => !query || [e.name, e.id, e.type, e.group].some(v => String(v || '').toLowerCase().includes(query)));
  if (!items.length) return h('div', {}, h('div', {class: 'empty'}, 'No layers yet. Pick a tool on the left and drag on the canvas, or insert a preset.'), h('div', {style: {textAlign: 'center'}}, h('button', {class: 'btn', onclick: openPresets}, 'Browse presets')));
  let dragId = null;
  const makeRow = (e, indent) => {
    const li = h('li', {class: 'layer' + (S.sel.includes(e.id) ? ' on' : '') + (indent ? ' child' : ''), draggable: 'true', 'data-id': e.id, tabindex: '0', role: 'option', 'aria-selected': S.sel.includes(e.id)},
      h('button', {class: 'mini' + (e.visible ? '' : ' off'), title: e.visible ? 'Hide' : 'Show', html: svg(e.visible ? 'eye' : 'eyeoff'),
        onclick: ev => { ev.stopPropagation(); mutate(() => e.visible = !e.visible); }}),
      h('span', {class: 'layer-type', html: svg(e.type), 'aria-hidden': true}),
      h('span', {class: 'nm', style: {opacity: offPage(e) ? .45 : 1}}, e.name),
      e.group && !selGroup(e) ? h('span', {class: 'chip', title: 'Tab page'}, e.group) : null,
      h('span', {class: 'ty'}, e.type),
      h('button', {class: 'mini' + (e.locked ? ' off' : ''), title: e.locked ? 'Unlock' : 'Lock', html: svg(e.locked ? 'lock' : 'unlock'),
        onclick: ev => { ev.stopPropagation(); mutate(() => e.locked = !e.locked); }}));
    li.addEventListener('click', ev => {
      S.side = 'props';
      if (ev.shiftKey) S.sel = S.sel.includes(e.id) ? S.sel.filter(i => i !== e.id) : [...S.sel, e.id];
      else S.sel = [e.id];
      render();
    });
    li.addEventListener('keydown', ev => { if (ev.key === 'Enter') { ev.preventDefault(); li.click(); } });
    li.addEventListener('dblclick', () => {
      const nm = li.querySelector('.nm');
      const inp = h('input', {class: 'inp', value: e.name});
      nm.replaceWith(inp); inp.focus(); inp.select();
      const done = () => mutate(() => e.name = inp.value || e.name);
      inp.addEventListener('blur', done); inp.addEventListener('keydown', k => { if (k.key === 'Enter') inp.blur(); });
    });
    li.addEventListener('dragstart', ev => { dragId = e.id; ev.dataTransfer.effectAllowed = 'move'; });
    li.addEventListener('dragover', ev => {
      ev.preventDefault();
      const r = li.getBoundingClientRect(), above = ev.clientY < r.top + r.height / 2;
      li.classList.toggle('drop-above', above); li.classList.toggle('drop-below', !above);
    });
    li.addEventListener('dragleave', () => li.classList.remove('drop-above', 'drop-below'));
    li.addEventListener('drop', ev => {
      ev.preventDefault();
      const r = li.getBoundingClientRect(), above = ev.clientY < r.top + r.height / 2;
      if (!dragId || dragId === e.id) return render();
      mutate(d => {
        const moving = d.elements.find(x => x.id === dragId);
        d.elements = d.elements.filter(x => x.id !== dragId);
        const idx = d.elements.findIndex(x => x.id === e.id);
        // list is reversed: "above" in the list = higher layer = after in array
        d.elements.splice(above ? idx + 1 : idx, 0, moving);
      });
    });
    return li;
  };
  // groups become folders (shown where their top-most member is)
  const done = new Set();
  S.collapsed = S.collapsed || {};
  for (const e of items) {
    const g = selGroup(e);
    if (!g) { list.append(makeRow(e, false)); continue; }
    if (done.has(g)) continue;
    done.add(g);
    const members = items.filter(x => x.group === g);
    const key = doc().id + ':' + g, closed = !!S.collapsed[key];
    const allOn = members.every(m => S.sel.includes(m.id)), anyVisible = members.some(m => m.visible);
    const name = h('span', {class: 'nm'}, groupName(g));
    const folder = h('li', {class: 'layer folder' + (allOn ? ' on' : '')},
      h('button', {class: 'mini', title: closed ? 'Expand' : 'Collapse', onclick: ev => { ev.stopPropagation(); S.collapsed[key] = !closed; renderNavigator(); }}, closed ? '▸' : '▾'),
      h('button', {class: 'mini' + (anyVisible ? '' : ' off'), title: anyVisible ? 'Hide group' : 'Show group', html: svg(anyVisible ? 'eye' : 'eyeoff'),
        onclick: ev => { ev.stopPropagation(); mutate(() => members.forEach(m => m.visible = !anyVisible)); }}),
      name, h('span', {class: 'ty'}, members.length + ' items'),
      h('button', {class: 'mini', title: 'Ungroup', onclick: ev => { ev.stopPropagation(); S.sel = members.map(m => m.id); ungroupSel(); }}, '⊟'));
    folder.addEventListener('click', ev => { S.sel = ev.shiftKey ? [...new Set([...S.sel, ...members.map(m => m.id)])] : members.map(m => m.id); render(); });
    folder.addEventListener('dblclick', () => {
      const inp = h('input', {class: 'inp', value: groupName(g)});
      name.replaceWith(inp); inp.focus(); inp.select();
      inp.addEventListener('blur', () => renameGroup(g, inp.value)); inp.addEventListener('keydown', k => { if (k.key === 'Enter') inp.blur(); });
    });
    list.append(folder);
    if (!closed) for (const m of members) list.append(makeRow(m, true));
  }
  const sets = tabsets();
  const tabsBox = h('div', {class: 'sec'}, h('h4', {}, 'Tab sets', h('button', {class: 'btn sm', onclick: openNewTabset}, '+ Tab set')),
    ...Object.entries(sets).map(([set, pages]) => h('div', {style: {marginBottom: '8px'}},
      h('div', {class: 'lab', style: {marginBottom: '4px'}}, set + ' · opens on ', h('b', {style: {color: 'var(--text)'}}, (doc().tabs || {})[set] || pages[0])),
      h('div', {style: {display: 'flex', gap: '4px', flexWrap: 'wrap'}}, ...pages.map(pg => h('button', {class: 'btn sm' + (viewPage(set) === pg ? ' primary' : ''), title: 'Edit this page · double-click to make it the default',
        onclick: () => setViewPage(set, pg), ondblclick: () => mutate(d => { d.tabs = d.tabs || {}; d.tabs[set] = pg; })}, pg))),
      S.sel.length ? h('div', {style: {display: 'flex', gap: '4px', marginTop: '6px', flexWrap: 'wrap'}},
        h('button', {class: 'btn sm ghost', onclick: () => putOnPage(set, viewPage(set))}, 'Put selection on “' + viewPage(set) + '”'),
        h('button', {class: 'btn sm ghost', onclick: () => putOnPage(set, '')}, 'Make always visible')) : null)),
    Object.keys(sets).length ? null : h('p', {style: {margin: 0, color: 'var(--faint)', fontSize: '12px'}}, 'Pages that share one spot, switched by tab buttons.'));
  return h('div', {}, h('div', {class: 'layer-count'}, items.length + (query ? ' matches' : ' layers'), h('button', {class: 'icon-btn', title: 'Add element', onclick: () => { S.nav = 'insert'; renderNavigator(); }}, '+')), list, tabsBox);
}

/* ============================================================== edit operations */
function removeSel() {
  if (!S.sel.length) return;
  const ids = new Set(S.sel);
  mutate(d => {
    d.elements = d.elements.filter(e => !ids.has(e.id));
    for (const a of d.animations) a.tracks = a.tracks.filter(t => !ids.has(t.element));
  });
  S.sel = [];
  render();
}
function duplicate() {
  const src = selected();
  if (!src.length) return;
  const copies = src.map(e => Object.assign(clone(e), {id: rid(), name: e.name + ' copy', x: e.x + 8, y: e.y + 8}));
  const actions = cloneQuickActions(src, copies);
  mutate(d => { d.elements.push(...copies); d.script.nodes.push(...actions); });
  S.sel = copies.map(c => c.id);
  render();
}
function arrange(where) {
  const ids = S.sel;
  if (!ids.length) return;
  mutate(d => {
    const list = d.elements;
    if (where === 'top' || where === 'bottom') {
      const moving = list.filter(e => ids.includes(e.id)), rest = list.filter(e => !ids.includes(e.id));
      d.elements = where === 'top' ? [...rest, ...moving] : [...moving, ...rest];
    } else {
      const dir = where === 'up' ? 1 : -1;
      const order = dir > 0 ? [...list.keys()].reverse() : [...list.keys()];
      for (const i of order) {
        const j = i + dir;
        if (ids.includes(list[i].id) && j >= 0 && j < list.length && !ids.includes(list[j].id)) [list[i], list[j]] = [list[j], list[i]];
      }
    }
  });
}
function alignSel(mode) {
  const list = selected();
  if (!list.length) return;
  const d = doc();
  const ref = list.length === 1 ? {x: 0, y: 0, w: d.canvas.width, h: d.canvas.height} : bbox(list);
  mutate(() => {
    for (const e of list) {
      if (mode === 'left') e.x = ref.x;
      if (mode === 'right') e.x = ref.x + ref.w - e.w;
      if (mode === 'cx') e.x = Math.round(ref.x + ref.w / 2 - e.w / 2);
      if (mode === 'top') e.y = ref.y;
      if (mode === 'bottom') e.y = ref.y + ref.h - e.h;
      if (mode === 'cy') e.y = Math.round(ref.y + ref.h / 2 - e.h / 2);
    }
  });
}
function distribute(axis) {
  const list = selected().sort((a, b) => a[axis] - b[axis]);
  if (list.length < 3) return toast('Select at least 3 elements');
  const size = axis === 'x' ? 'w' : 'h';
  const first = list[0], last = list[list.length - 1];
  const total = list.reduce((s, e) => s + e[size], 0);
  const gap = (last[axis] + last[size] - first[axis] - total) / (list.length - 1);
  mutate(() => { let p = first[axis]; for (const e of list) { e[axis] = Math.round(p); p += e[size] + gap; } });
}
function nudge(dx, dy) {
  const list = selected().filter(e => !e.locked);
  if (!list.length) return;
  mutate(() => list.forEach(e => { setProp(e, 'x', vals(e).x + dx); setProp(e, 'y', vals(e).y + dy); }));
}
function copySel() { S.clip = clone(selected()); S.clipActions = clone(doc().script.nodes.filter(n => S.sel.some(id => n.id === 'quick_event_' + id || n.id === 'quick_action_' + id))); if (S.clip.length) toast('Copied ' + S.clip.length); }
function paste() {
  if (!S.clip || !S.clip.length) return;
  const copies = S.clip.map(e => Object.assign(clone(e), {id: byId(e.id) ? rid() : e.id, x: e.x + 10, y: e.y + 10}));
  const actions = cloneQuickActions(S.clip, copies, S.clipActions || []);
  mutate(d => { d.elements.push(...copies); d.script.nodes.push(...actions); });
  S.clipActions = clone(actions);
  S.clip = clone(copies);
  S.sel = copies.map(c => c.id);
  render();
}

/* ============================================================== rail, menus, tabs, status */
const TOOLS = [['select', 'Select (V)', 'select'], ['rect', 'Rectangle (R)', 'rect'], ['text', 'Text (T)', 'text'], ['button', 'Button (B)', 'button'],
  ['image', 'Image / sprite (I)', 'image'], ['input', 'Text input (N)', 'input'], ['progress', 'Progress bar (G)', 'progress'], ['item', 'Item (E)', 'item'], ['head', 'Player head (H)', 'head'], ['model', 'Player model (M)', 'model']];
function setTool(t) { S.tool = t; renderRail(); stage.style.cursor = t === 'select' ? '' : 'crosshair'; }
function renderRail() {
  const r = $('#rail');
  r.replaceChildren(
    ...TOOLS.map(([id, title, icon]) => h('button', {class: 'icon-btn' + (S.tool === id ? ' on' : ''), title, html: svg(icon), onclick: () => setTool(id)})),
    h('div', {class: 'sep'}),
    h('button', {class: 'icon-btn' + (S.library ? ' on' : ''), 'data-rail': 'library', title: 'Library: drag items and sprites onto the canvas (L)', html: svg('library'), onclick: () => toggleLibrary()}),
    h('button', {class: 'icon-btn', 'data-rail': 'assets', title: 'Assets: sprites, items, textures, skins', html: svg('assets'), onclick: () => openAssets()}),
    h('button', {class: 'icon-btn', 'data-rail': 'presets', title: 'Presets', html: svg('presets'), onclick: openPresets}),
    h('button', {class: 'icon-btn', 'data-rail': 'placeholders', title: 'Placeholders', html: svg('braces'), onclick: openPlaceholders}),
    h('button', {class: 'icon-btn', 'data-rail': 'commands', title: 'Commands & functions', html: svg('commands'), onclick: openCommands}),
    h('button', {class: 'icon-btn' + (S.dock ? ' on' : ''), 'data-rail': 'timeline', title: 'Animation timeline (A)', html: svg('timeline'), onclick: toggleDock}),
    h('button', {class: 'icon-btn' + (S.scriptOpen ? ' on' : ''), 'data-rail': 'scripts', title: 'Scripts (K)', html: svg('script'), onclick: toggleScript}),
    h('div', {class: 'grow'}),
    h('button', {class: 'icon-btn', 'data-rail': 'tutorial', title: 'Tutorial', onclick: () => startTutorial(0)}, '?'),
    h('button', {class: 'icon-btn', title: 'Code (JSON)', html: svg('code'), onclick: openCode}));
}
const MENUS = {
  File: [['New UI from template…', openNewDocument], ['New screen UI', () => newTab('screen')], ['New chest inventory', () => newTab('chest')], ['New HUD', () => newTab('hud')], ['New world UI', () => newTab('world')], ['Reopen closed UI', reopenClosed], '-',
    ['New logic file (commands only)', () => newTab('logic')], '-',
    ['Example: profile menu', () => openTab(tplMenu())], ['Example: tabbed shop', () => openTab(tplShop())], ['Example: HUD', () => openTab(tplHud())], ['Example: world board', () => openTab(tplBoard())], '-',
    ['Open JSON…', () => $('#fileOpen').click(), 'Ctrl+O'], ['Load from server…', openPush], ['Export…', openExport, 'Ctrl+E'], ['Push to server…', openPush]],
  Edit: [['Undo', undo, 'Ctrl+Z'], ['Redo', redo, 'Ctrl+Shift+Z'], '-', ['Copy', copySel, 'Ctrl+C'], ['Paste', paste, 'Ctrl+V'], ['Duplicate', duplicate, 'Ctrl+D'], ['Delete', removeSel, 'Del'], '-',
    ['Select all', () => { S.sel = els().filter(e => !e.locked).map(e => e.id); render(); }, 'Ctrl+A']],
  Layer: [['Bring to front', () => arrange('top'), ']'], ['Bring forward', () => arrange('up')], ['Send backward', () => arrange('down')], ['Send to back', () => arrange('bottom'), '['], '-',
    ['Group', groupSel, 'Ctrl+G'], ['Ungroup', ungroupSel, 'Ctrl+Shift+G'], '-',
    ['Show / hide', () => mutate(() => selected().forEach(e => e.visible = !e.visible)), 'Ctrl+H'], ['Lock / unlock', () => mutate(() => selected().forEach(e => e.locked = !e.locked)), 'Ctrl+L']],
  Insert: [['Item…', () => pickItem(id => addItemEl(id.replace('minecraft:', '')))], ['Player head', () => addElement('head')], ['Player model', () => addElement('model')], '-',
    ['Preset…', openPresets], ['Tab set…', openNewTabset], ['Command…', openCommands]],
  Window: [['Library (drag & drop)', () => toggleLibrary(), 'L'], ['Placeholders', openPlaceholders], ['Tutorial', () => startTutorial(0)], '-', ['Assets', () => openAssets()], ['Presets', openPresets], ['Commands & functions', openCommands], ['Timeline', toggleDock, 'A'], ['Scripts', toggleScript, 'K'], ['Code (JSON)', openCode], '-', ['Preview', startPreview, 'P'], ['Fit canvas', fit, 'Shift+1'], ['Help', openHelp, '?']]
};
function renderMenus() {
  const m = $('#menus');
  m.replaceChildren(...Object.entries(MENUS).map(([name, items]) => {
    const wrap = h('div', {class: 'menu'});
    const btn = h('button', {onclick: ev => { ev.stopPropagation(); const open = wrap.classList.contains('open'); closeMenus(); if (!open) { wrap.classList.add('open'); wrap.append(drop); } }}, name);
    const drop = h('div', {class: 'dropdown'}, ...items.map(it => it === '-' ? h('hr') :
      h('button', {onclick: () => { closeMenus(); it[1](); }}, h('span', {}, it[0]), it[2] ? h('kbd', {}, it[2]) : '')));
    wrap.append(btn);
    return wrap;
  }));
}
function closeMenus() { $$('.menu.open').forEach(m => { m.classList.remove('open'); m.querySelector('.dropdown')?.remove(); }); }
document.addEventListener('click', closeMenus);
function renderTabs() {
  const t = $('#tabs');
  t.replaceChildren(...S.docs.map((d, i) => h('div', {class: 'tab' + (i === S.cur ? ' active' : ''), role: 'tab', tabindex: '0', 'aria-selected': i === S.cur, onkeydown: ev => { if (ev.key === 'Enter') switchTab(i); }, onclick: () => switchTab(i)},
    h('span', {class: 'typ'}, d.type), d.name || d.id,
    h('button', {class: 'x', title: 'Close', onclick: ev => { ev.stopPropagation(); closeTab(i); }}, '×'))),
    h('button', {class: 'icon-btn', title: 'New UI', 'aria-label': 'New UI', onclick: openNewDocument}, '+'));
  $('#docname').value = doc().name;
}
function rememberSession() {
  S.sessions[S.cur] = {undo: S.undo, redo: S.redo, sel: [...S.sel], zoom: S.zoom, panX: S.panX, panY: S.panY, autoFit: S.autoFit, anim: {...S.anim, playing: false}};
}
function restoreSession() {
  const saved = S.sessions[S.cur];
  S.undo = saved?.undo || []; S.redo = saved?.redo || []; S.sel = (saved?.sel || []).filter(id => byId(id));
  S.anim = saved?.anim || {id: doc().animations[0]?.id || null, t: 0, playing: false, auto: false, selKey: null};
  S.hover = null; S.selectedSlot = null;
  if (saved) { S.zoom = saved.zoom; S.panX = saved.panX; S.panY = saved.panY; S.autoFit = saved.autoFit; }
  render();
  if (!saved || S.autoFit) fit();
}
function switchTab(i) {
  if (i === S.cur || !S.docs[i]) return;
  stopPreview(); stopPlay(); rememberSession(); S.cur = i;
  restoreSession(); saveLocal();
}
function openTab(d) {
  const normalized = normalize(d);
  const existing = S.docs.findIndex(x => x.id === normalized.id);
  if (existing >= 0) { switchTab(existing); toast('This UI is already open.'); return; }
  S.docs.push(normalized); switchTab(S.docs.length - 1); saveLocal();
}
function newTab(type) { const name = {hud: 'New HUD', world: 'New board', logic: 'New commands', chest: 'New inventory'}[type] || 'New menu'; openTab(uniqueDocument(newDoc(type, name))); }
function closeTab(i) {
  stopPreview(); stopPlay(); rememberSession();
  S.closedTabs.push({d: S.docs[i], session: S.sessions[i]});
  if (S.closedTabs.length > 10) S.closedTabs.shift();
  S.docs.splice(i, 1); S.sessions.splice(i, 1);
  if (!S.docs.length) { S.docs = [newDoc('screen', 'Untitled')]; S.sessions = []; }
  S.cur = clamp(S.cur > i ? S.cur - 1 : S.cur, 0, S.docs.length - 1);
  restoreSession(); saveLocal();
  toast('Document closed. Reopen it from File → Reopen closed UI.');
}
function renderStatus() {
  const d = doc(), e = selected()[0];
  const v = e ? vals(e) : null;
  $('#status').replaceChildren(
    h('span', {}, d.id + '.json'),
    h('span', {}, v ? `${e.name}  x ${round(v.x)}  y ${round(v.y)}  ${round(v.w)}×${round(v.h)}` : `${d.elements.length} layers`),
    h('span', {class: 'sp'}),
    h('span', {}, S.tool === 'select' ? 'Select' : 'Draw ' + S.tool),
    h('span', {}, Math.round(S.zoom * 100) + '%'));
}
$('#docname').addEventListener('change', ev => mutate(d => { d.name = ev.target.value || d.name; if (!d.id || d.id === 'ui') d.id = slug(d.name); }));
$('#sidetabs').addEventListener('click', ev => { const b = ev.target.closest('button'); if (b) { S.side = b.dataset.t; renderPanel(); } });
/* ============================================================== timeline */
const dock = $('#dock');
const PX_PER_TICK = 14;
const PROP_LABEL = {x: 'Position X', y: 'Position Y', w: 'Width', h: 'Height', scale: 'Scale', rotation: 'Rotation', opacity: 'Opacity', turn: '3D turn', fill: 'Fill colour', textColor: 'Text colour'};
function toggleDock() {
  S.dock = !S.dock;
  if (S.dock) S.scriptOpen = false;
  if (S.dock && !curAnim() && doc().animations.length) S.anim.id = doc().animations[0].id;
  if (!S.dock) stopPlay();
  render();
}
function newAnimation() {
  const a = {id: rid('an'), name: 'animation' + (doc().animations.length + 1), length: 20, loop: false, autoplay: false, tracks: []};
  mutate(d => d.animations.push(a));
  S.anim.id = a.id; S.anim.t = 0; S.anim.auto = true;
  render();
  toast('Auto-key is on: moving or resizing writes keyframes at the playhead.');
}
let playRaf = 0, playLast = 0;
function togglePlay() { if (S.anim.playing) stopPlay(); else startPlay(); }
function startPlay() {
  const a = curAnim(); if (!a) return;
  S.anim.playing = true; playLast = performance.now();
  if (S.anim.t >= a.length) S.anim.t = 0;
  const step = now => {
    if (!S.anim.playing) return;
    S.anim.t += (now - playLast) / 50; playLast = now;
    if (S.anim.t >= a.length) { if (a.loop) S.anim.t %= a.length; else { S.anim.t = a.length; stopPlay(); } }
    renderCanvas(); renderTimelineHead();
    playRaf = requestAnimationFrame(step);
  };
  playRaf = requestAnimationFrame(step);
  renderTimeline();
}
function stopPlay() { S.anim.playing = false; cancelAnimationFrame(playRaf); S.anim.t = Math.round(S.anim.t); if (S.dock) { renderTimeline(); renderCanvas(); } }
function renderTimelineHead() {
  const ph = $('#tlPlayhead'); if (ph) ph.style.left = (S.anim.t * PX_PER_TICK) + 'px';
  const tm = $('#tlTime'); if (tm) tm.textContent = `${Math.round(S.anim.t)}t · ${(S.anim.t / 20).toFixed(2)}s`;
}
function renderTimeline() {
  dock.hidden = !S.dock;
  if (!S.dock) return;
  const d = doc(), a = curAnim(), e = selected()[0];
  const bar = h('div', {class: 'dockbar'});
  const sel = h('select', {id: 'tlAnim', onchange: ev => { S.anim.id = ev.target.value || null; S.anim.t = 0; render(); }},
    h('option', {value: ''}, d.animations.length ? '— pick animation —' : 'no animations'),
    ...d.animations.map(x => h('option', {value: x.id, selected: x.id === S.anim.id}, x.name)));
  bar.append(
    h('button', {class: 'icon-btn', title: 'Play / pause (Space in timeline)', onclick: togglePlay, disabled: !a}, S.anim.playing ? '❚❚' : '▶'),
    h('span', {class: 'time', id: 'tlTime'}, `${Math.round(S.anim.t)}t · ${(S.anim.t / 20).toFixed(2)}s`),
    sel,
    h('button', {class: 'btn sm', onclick: newAnimation}, '+ New'));
  if (a) {
    const nm = h('input', {value: a.name, style: {width: '110px'}, title: 'Name (used by Play animation nodes)', onchange: ev => mutate(() => a.name = ev.target.value.trim() || a.name)});
    const len = h('input', {type: 'number', min: 1, value: a.length, style: {width: '64px'}, title: 'Length in ticks', onchange: ev => mutate(() => a.length = Math.max(1, Math.round(+ev.target.value || 1)))});
    const loop = h('label', {class: 'check', style: {margin: 0}}, h('input', {type: 'checkbox', checked: a.loop, onchange: ev => mutate(() => a.loop = ev.target.checked)}), 'Loop');
    const auto = h('label', {class: 'check', style: {margin: 0}}, h('input', {type: 'checkbox', checked: a.autoplay, onchange: ev => mutate(() => a.autoplay = ev.target.checked)}), 'Autoplay');
    const rec = h('button', {class: 'btn sm' + (S.anim.auto ? ' primary' : ''), title: 'When on, changes to position/size/rotation/scale/opacity become keyframes', onclick: () => { S.anim.auto = !S.anim.auto; renderTimeline(); }}, S.anim.auto ? '● Auto-key' : '○ Auto-key');
    bar.append(nm, len, h('span', {class: 'lab'}, 'ticks'), loop, auto, rec,
      h('button', {class: 'btn sm danger', onclick: () => { mutate(dd => dd.animations = dd.animations.filter(x => x.id !== a.id)); S.anim.id = null; render(); }}, 'Delete'));
    const k = S.anim.selKey && findKey(a, S.anim.selKey);
    if (k) {
      bar.append(h('span', {class: 'lab', style: {marginLeft: '8px'}}, 'Key'),
        h('input', {type: 'number', value: k.k.t, style: {width: '58px'}, title: 'Tick', onchange: ev => mutate(() => { k.k.t = clamp(Math.round(+ev.target.value), 0, a.length); k.t.keys.sort((p, q) => p.t - q.t); })}),
        h('input', {type: COLOR_PROPS.includes(k.t.property) ? 'color' : 'number', step: 'any', value: COLOR_PROPS.includes(k.t.property) ? '#' + Math.round(k.k.v).toString(16).padStart(6, '0') : round(k.k.v, 3), style: {width: '70px'}, title: 'Value', onchange: ev => mutate(() => k.k.v = COLOR_PROPS.includes(k.t.property) ? parseInt(ev.target.value.slice(1), 16) : +ev.target.value)}),
        h('select', {title: 'Easing to the next key', onchange: ev => mutate(() => k.k.ease = ev.target.value)},
          ...['linear', 'ease-in', 'ease-out', 'ease-in-out', 'back', 'bounce', 'step'].map(o => h('option', {value: o, selected: k.k.ease === o}, o))),
        h('button', {class: 'btn sm danger', onclick: () => { mutate(() => k.t.keys.splice(k.t.keys.indexOf(k.k), 1)); S.anim.selKey = null; render(); }}, 'Delete key'));
    }
  }
  const tl = h('div', {class: 'tl'});
  const left = h('div', {class: 'tl-left'}), right = h('div', {class: 'tl-right', id: 'tlRight'});
  left.append(h('div', {class: 'tl-row head'}, e ? e.name : (a ? 'Select an element' : 'Create an animation')));
  const width = ((a ? a.length : 20) + 6) * PX_PER_TICK;
  const ruler = h('div', {class: 'ruler', style: {width: width + 'px'}});
  for (let t = 0; t <= (a ? a.length : 20) + 5; t++) {
    ruler.append(h('i', {style: {left: t * PX_PER_TICK + 'px', height: t % 5 ? '4px' : '8px'}}));
    if (t % 5 === 0) ruler.append(h('span', {style: {left: t * PX_PER_TICK + 'px'}}, String(t)));
  }
  const scrub = ev => { const r = ruler.getBoundingClientRect(); S.anim.t = clamp(Math.round((ev.clientX - r.left) / PX_PER_TICK), 0, a ? a.length : 0); renderCanvas(); renderTimelineHead(); renderPanelLive(); };
  ruler.addEventListener('pointerdown', ev => { if (!a) return; stopPlay(); scrub(ev); ruler.setPointerCapture(ev.pointerId); ruler.onpointermove = scrub; ruler.onpointerup = () => { ruler.onpointermove = null; renderPanel(); }; });
  right.append(ruler);
  if (a && e) {
    for (const p of ANIM_PROPS) {
      const t = track(a, e.id, p);
      left.append(h('div', {class: 'tl-row'}, PROP_LABEL[p], h('button', {title: 'Add keyframe at playhead', onclick: () => addKeyAtPlayhead(e, p)}, '+')));
      const row = h('div', {class: 'track', style: {width: width + 'px'}});
      if (t && t.keys.length > 1) row.append(h('div', {class: 'bar', style: {left: t.keys[0].t * PX_PER_TICK + 'px', width: (t.keys[t.keys.length - 1].t - t.keys[0].t) * PX_PER_TICK + 'px'}}));
      if (t) for (const k of t.keys) {
        const id = e.id + '|' + p + '|' + t.keys.indexOf(k);
        const kf = h('div', {class: 'kf' + (S.anim.selKey === id ? ' on' : ''), style: {left: k.t * PX_PER_TICK + 'px'}, title: `${k.t}t = ${round(k.v, 3)} (${k.ease})`});
        kf.addEventListener('pointerdown', ev => {
          ev.stopPropagation();
          S.anim.selKey = id;
          const snap = snapshot(), sx = ev.clientX, t0 = k.t;
          kf.setPointerCapture(ev.pointerId);
          kf.onpointermove = m => { k.t = clamp(Math.round(t0 + (m.clientX - sx) / PX_PER_TICK), 0, a.length); kf.style.left = k.t * PX_PER_TICK + 'px'; };
          kf.onpointerup = () => { kf.onpointermove = null; t.keys.sort((x, y) => x.t - y.t); S.anim.selKey = e.id + '|' + p + '|' + t.keys.indexOf(k); if (snapshot() !== snap) pushUndo(snap); S.anim.t = k.t; afterChange(); };
        });
        row.append(kf);
      }
      right.append(row);
    }
  } else if (a) {
    // overview: one row per element that has tracks
    const ids = [...new Set(a.tracks.map(t => t.element))];
    for (const id of ids) {
      const el = byId(id); if (!el) continue;
      left.append(h('div', {class: 'tl-row', style: {cursor: 'pointer'}, onclick: () => { S.sel = [id]; render(); }}, el.name, h('span', {class: 'lab'}, a.tracks.filter(t => t.element === id).length + ' tracks')));
      const row = h('div', {class: 'track', style: {width: width + 'px'}});
      for (const t of a.tracks.filter(t => t.element === id)) for (const k of t.keys) row.append(h('div', {class: 'kf', style: {left: k.t * PX_PER_TICK + 'px', background: 'var(--faint)'}}));
      right.append(row);
    }
    if (!ids.length) left.append(h('div', {class: 'tl-row'}, 'Select an element, then press + on a property.'));
  }
  if (a) {
    right.append(h('div', {class: 'endline', style: {left: a.length * PX_PER_TICK + 'px'}}));
    right.append(h('div', {class: 'playhead', id: 'tlPlayhead', style: {left: S.anim.t * PX_PER_TICK + 'px'}}));
  }
  tl.append(left, right);
  const prevScroll = $('#tlRight')?.scrollLeft || 0;
  dock.replaceChildren(bar, tl);
  right.scrollLeft = prevScroll;
  right.addEventListener('scroll', () => { left.scrollTop = right.scrollTop; });
}
function findKey(a, key) {
  const [id, p, i] = key.split('|');
  const t = track(a, id, p);
  return t && t.keys[+i] ? {t, k: t.keys[+i]} : null;
}
function addKeyAtPlayhead(e, p) {
  const a = curAnim(); if (!a) return;
  const raw = vals(e)[p];
  const v = COLOR_PROPS.includes(p) ? parseInt(String(raw || '#ffffff').replace('#', ''), 16) : Number(raw) || 0;
  mutate(() => {
    const t = track(a, e.id, p, true);
    const tick = Math.round(S.anim.t);
    const k = t.keys.find(k => k.t === tick);
    if (k) k.v = v; else t.keys.push({t: tick, v, ease: 'linear'});
    t.keys.sort((x, y) => x.t - y.t);
  });
}

/* ============================================================== script editor */
const script = $('#script');
const SC = {zoom: 1, panX: 40, panY: 30, sel: null, wire: null};
function toggleScript() { S.scriptOpen = !S.scriptOpen; if (S.scriptOpen) S.dock = false; render(); }
function renderScript() {
  script.hidden = !S.scriptOpen;
  if (!S.scriptOpen) return;
  const top = h('div', {class: 'sc-top'},
    h('span', {class: 'ttl'}, doc().name + '.script'),
    h('span', {class: 'lab'}, doc().script.nodes.length + ' nodes'),
    h('button', {class: 'btn sm ghost', onclick: () => { SC.zoom = 1; SC.panX = 40; SC.panY = 30; renderNodes(); }}, 'Reset view'),
    h('button', {class: 'btn sm', onclick: openCode}, '‹› Code'),
    h('button', {class: 'btn sm', onclick: startPreview}, '▶ Preview'),
    h('button', {class: 'btn sm primary', onclick: toggleScript}, 'Done'));
  const search = h('input', {class: 'inp', placeholder: 'Search nodes…', id: 'nodeSearch'});
  const pal = h('div', {class: 'palette'}, search);
  const list = h('div');
  const fill = () => {
    const q = search.value.toLowerCase();
    list.replaceChildren();
    for (const [cat, [label, color]] of Object.entries(CAT)) {
      const items = Object.entries(NODE_TYPES).filter(([k, v]) => v.cat === cat && (!q || v.label.toLowerCase().includes(q) || k.includes(q)));
      if (!items.length) continue;
      list.append(h('div', {class: 'grp'}, h('i', {style: {background: color}}), label));
      for (const [k, v] of items) list.append(h('button', {onclick: () => addNode(k)}, v.label));
    }
  };
  search.addEventListener('input', fill);
  fill();
  pal.append(list);
  const area = h('div', {id: 'nodes'}, h('div', {id: 'nodeLayer'}));
  script.replaceChildren(top, pal, area);
  wireNodeArea(area);
  renderNodes();
}
function addNode(type) {
  const area = $('#nodes').getBoundingClientRect();
  const x = (area.width / 2 - SC.panX) / SC.zoom - 118 + (Math.random() * 40 - 20), y = (area.height / 2 - SC.panY) / SC.zoom - 60 + (Math.random() * 40 - 20);
  const props = {};
  for (const f of NODE_TYPES[type].fields) {
    if (f.kind === 'select') props[f.k] = f.opts[0];
    else if (f.def) props[f.k] = f.def;
    else if (f.kind === 'color') props[f.k] = '#ff8fd1';
    else if (f.kind === 'element' && !f.any && selected()[0]) props[f.k] = selected()[0].id;
  }
  const n = {id: rid('n'), type, x: Math.round(x), y: Math.round(y), props, next: {}};
  mutate(d => d.script.nodes.push(n));
  SC.sel = n.id;
  renderNodes();
}
function nodeField(n, f) {
  const set = v => mutate(() => { n.props[f.k] = v; }, {keepScript: true});
  const val = n.props[f.k] ?? '';
  let input;
  if (f.kind === 'element') {
    input = h('select', {class: 'inp'}, h('option', {value: ''}, f.any ? 'Any element' : '— pick —'),
      ...els().map(e => h('option', {value: e.id, selected: e.id === val}, e.name)));
  } else if (f.kind === 'select') {
    input = h('select', {class: 'inp'}, ...f.opts.map(o => h('option', {value: o, selected: o === val}, o)));
  } else if (f.kind === 'sprite') {
    input = h('select', {class: 'inp'}, h('option', {value: ''}, '— pick —'), ...doc().sprites.map(s => h('option', {value: s.id, selected: s.id === val}, s.id)));
  } else if (f.kind === 'anim') {
    input = h('select', {class: 'inp'}, h('option', {value: ''}, '— pick —'), ...doc().animations.map(a => h('option', {value: a.name, selected: a.name === val || a.id === val}, a.name)));
  } else if (f.kind === 'tabset') {
    input = h('select', {class: 'inp'}, h('option', {value: ''}, '— pick —'), ...Object.keys(tabsets()).map(t => h('option', {value: t, selected: t === val}, t)));
  } else if (f.kind === 'group') {
    const groups = [...new Set(els().map(e => e.group).filter(Boolean).flatMap(g => [g, ...(g.includes('/') ? [g.split('/')[0]] : [])]))];
    input = h('select', {class: 'inp'}, h('option', {value: ''}, '— pick —'), ...groups.map(g => h('option', {value: g, selected: g === val}, g)));
  } else if (f.kind === 'fn') {
    const names = functionNames();
    if (val && !names.includes(val)) names.push(val);
    input = h('select', {class: 'inp'}, h('option', {value: ''}, '— pick —'), ...names.map(g => h('option', {value: g, selected: g === val}, 'ƒ ' + g)));
  } else if (f.kind === 'item') {
    input = h('input', {class: 'inp', value: val, placeholder: 'minecraft:diamond or {item}'});
    const wrap = h('div', {style: {display: 'flex', gap: '4px'}}, input, h('button', {class: 'btn sm', title: 'Pick item', onclick: () => pickItem(id => set(id))}, '…'));
    input.addEventListener('change', () => set(input.value));
    input.addEventListener('pointerdown', ev => ev.stopPropagation());
    return h('div', {class: 'nf'}, h('span', {}, f.label), wrap);
  } else if (f.kind === 'color') {
    input = h('input', {class: 'inp', type: 'color', value: /^#[0-9a-f]{6}$/i.test(val) ? val : '#ffffff', style: {padding: '1px', height: '26px'}});
  } else {
    input = h('input', {class: 'inp', value: val, placeholder: f.ph || f.def || ''});
  }
  input.addEventListener('change', () => set(input.value));
  input.addEventListener('pointerdown', ev => ev.stopPropagation());
  return h('div', {class: 'nf'}, h('span', {}, f.label), input);
}
function renderNodes() {
  const layer = $('#nodeLayer'); if (!layer) return;
  layer.style.transform = `translate(${SC.panX}px,${SC.panY}px) scale(${SC.zoom})`;
  const wires = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
  wires.id = 'wires'; wires.setAttribute('width', '1'); wires.setAttribute('height', '1');
  const kids = [wires];
  for (const n of doc().script.nodes) {
    const def = NODE_TYPES[n.type] || {cat: 'action', label: n.type, fields: []};
    const color = CAT[def.cat][1];
    const el = h('div', {class: 'node' + (SC.sel === n.id ? ' on' : ''), 'data-id': n.id, style: {left: n.x + 'px', top: n.y + 'px'}});
    const head = h('div', {class: 'nh'}, h('i', {style: {background: color}}), def.label,
      h('button', {class: 'del', title: 'Delete node', onclick: ev => { ev.stopPropagation(); deleteNode(n.id); }}, '×'));
    el.append(head);
    if (def.cat !== 'event') el.append(h('div', {class: 'port in', title: 'Input'}));
    const body = h('div', {class: 'nb'}, ...def.fields.map(f => nodeField(n, f)));
    const outs = h('div', {class: 'outs'});
    for (const port of def.branch ? ['true', 'false'] : ['then']) {
      outs.append(h('div', {class: 'out ' + port}, port === 'then' ? 'Then' : port === 'true' ? 'True' : 'False', h('div', {class: 'port', 'data-port': port, 'data-node': n.id})));
    }
    body.append(outs);
    el.append(body);
    head.addEventListener('pointerdown', ev => {
      if (ev.target.closest('.del')) return;
      ev.stopPropagation();
      SC.sel = n.id;
      $$('.node.on').forEach(x => x.classList.remove('on')); el.classList.add('on');
      const sx = ev.clientX, sy = ev.clientY, x0 = n.x, y0 = n.y, snap = snapshot();
      head.setPointerCapture(ev.pointerId);
      head.onpointermove = m => { n.x = Math.round(x0 + (m.clientX - sx) / SC.zoom); n.y = Math.round(y0 + (m.clientY - sy) / SC.zoom); el.style.left = n.x + 'px'; el.style.top = n.y + 'px'; drawWires(); };
      head.onpointerup = () => { head.onpointermove = null; if (snapshot() !== snap) { pushUndo(snap); saveLocal(); } };
    });
    kids.push(el);
  }
  layer.replaceChildren(...kids);
  drawWires();
}
function portPos(nodeId, port) {
  const layer = $('#nodeLayer');
  const n = layer.querySelector(`.node[data-id="${nodeId}"]`);
  if (!n) return null;
  const p = port === 'in' ? n.querySelector('.port.in') : n.querySelector(`.port[data-port="${port}"]`);
  if (!p) return {x: n.offsetLeft, y: n.offsetTop + 18};
  let x = p.offsetLeft + p.offsetWidth / 2, y = p.offsetTop + p.offsetHeight / 2, o = p.offsetParent;
  while (o && o !== layer) { x += o.offsetLeft; y += o.offsetTop; o = o.offsetParent; }
  return {x, y};
}
function bez(a, b) { const dx = Math.max(40, Math.abs(b.x - a.x) * .5); return `M${a.x},${a.y} C${a.x + dx},${a.y} ${b.x - dx},${b.y} ${b.x},${b.y}`; }
function drawWires() {
  const w = $('#wires'); if (!w) return;
  const paths = [];
  for (const n of doc().script.nodes) for (const [port, list] of Object.entries(n.next || {})) for (const to of list || []) {
    const a = portPos(n.id, port), b = portPos(to, 'in');
    if (!a || !b) continue;
    const p = document.createElementNS('http://www.w3.org/2000/svg', 'path');
    p.setAttribute('d', bez(a, b));
    if (port === 'true') p.classList.add('t'); if (port === 'false') p.classList.add('f');
    p.innerHTML = '<title>Click to disconnect</title>';
    p.addEventListener('click', () => mutate(() => { n.next[port] = n.next[port].filter(x => x !== to); }, {keepScript: true}));
    paths.push(p);
  }
  if (SC.wire) {
    const p = document.createElementNS('http://www.w3.org/2000/svg', 'path');
    p.setAttribute('d', bez(SC.wire.a, SC.wire.b)); p.classList.add('temp'); paths.push(p);
  }
  w.replaceChildren(...paths);
}
function deleteNode(id) {
  mutate(d => {
    d.script.nodes = d.script.nodes.filter(n => n.id !== id);
    for (const n of d.script.nodes) for (const k of Object.keys(n.next || {})) n.next[k] = n.next[k].filter(x => x !== id);
  }, {keepScript: true});
}
function wireNodeArea(area) {
  const toLayer = ev => { const r = area.getBoundingClientRect(); return {x: (ev.clientX - r.left - SC.panX) / SC.zoom, y: (ev.clientY - r.top - SC.panY) / SC.zoom}; };
  area.addEventListener('wheel', ev => {
    ev.preventDefault();
    const r = area.getBoundingClientRect();
    if (ev.ctrlKey || ev.metaKey) {
      const nz = clamp(SC.zoom * Math.exp(-ev.deltaY * .002), .3, 2), cx = ev.clientX - r.left, cy = ev.clientY - r.top;
      SC.panX = cx - (cx - SC.panX) * nz / SC.zoom; SC.panY = cy - (cy - SC.panY) * nz / SC.zoom; SC.zoom = nz;
    } else { SC.panX -= ev.deltaX; SC.panY -= ev.deltaY; }
    $('#nodeLayer').style.transform = `translate(${SC.panX}px,${SC.panY}px) scale(${SC.zoom})`;
  }, {passive: false});
  area.addEventListener('pointerdown', ev => {
    const port = ev.target.closest('.port[data-port]');
    if (port) {
      ev.stopPropagation();
      const from = port.dataset.node, which = port.dataset.port;
      SC.wire = {a: portPos(from, which), b: toLayer(ev)};
      area.setPointerCapture(ev.pointerId);
      let over = null;
      area.onpointermove = m => {
        SC.wire.b = toLayer(m);
        const target = document.elementFromPoint(m.clientX, m.clientY)?.closest('.node');
        if (over && over !== target) over.classList.remove('drop');
        over = target && target.dataset.id !== from ? target : null;
        if (over) over.classList.add('drop');
        drawWires();
      };
      area.onpointerup = () => {
        area.onpointermove = null; area.onpointerup = null; SC.wire = null;
        if (over) {
          over.classList.remove('drop');
          const to = over.dataset.id, toNode = doc().script.nodes.find(n => n.id === to);
          if (toNode && NODE_TYPES[toNode.type]?.cat === 'event') toast('Events start chains, they cannot be connected into.', 'err');
          else mutate(() => { const n = doc().script.nodes.find(x => x.id === from); n.next = n.next || {}; const l = n.next[which] = n.next[which] || []; if (!l.includes(to)) l.push(to); }, {keepScript: true});
        }
        drawWires();
      };
      return;
    }
    if (ev.target.closest('.node')) return;
    SC.sel = null; $$('.node.on').forEach(x => x.classList.remove('on'));
    const sx = ev.clientX, sy = ev.clientY, px = SC.panX, py = SC.panY;
    area.setPointerCapture(ev.pointerId);
    area.onpointermove = m => { SC.panX = px + m.clientX - sx; SC.panY = py + m.clientY - sy; $('#nodeLayer').style.transform = `translate(${SC.panX}px,${SC.panY}px) scale(${SC.zoom})`; };
    area.onpointerup = () => { area.onpointermove = null; area.onpointerup = null; };
  });
}
/* ============================================================== modals */
function modal(title, body, actions = []) {
  const previous = document.activeElement;
  const bg = h('div', {class: 'modal-bg', onpointerdown: ev => { if (ev.target === bg) close(); }});
  const close = () => { bg.remove(); $('#app').inert = !!$('.modal-bg'); document.removeEventListener('keydown', onKey, true); if (previous?.isConnected) previous.focus(); };
  const onKey = ev => {
    if (bg !== $$('.modal-bg').at(-1)) return;
    if (ev.key === 'Escape') { ev.preventDefault(); ev.stopImmediatePropagation(); close(); }
    if (ev.key === 'Tab') {
      const controls = [...bg.querySelectorAll('button,input,select,textarea,a[href],[tabindex="0"]')].filter(e => !e.disabled && e.getClientRects().length);
      const first = controls[0], last = controls.at(-1);
      if (ev.shiftKey && document.activeElement === first) { ev.preventDefault(); last?.focus(); }
      else if (!ev.shiftKey && document.activeElement === last) { ev.preventDefault(); first?.focus(); }
    }
  };
  document.addEventListener('keydown', onKey, true);
  const m = h('div', {class: 'modal', role: 'dialog', 'aria-modal': 'true', 'aria-label': title},
    h('header', {}, title, h('button', {class: 'icon-btn', title: 'Close', 'aria-label': 'Close', onclick: close}, '×')),
    h('div', {class: 'body'}, ...[].concat(body)),
    actions.length ? h('footer', {}, ...actions.map(a => typeof a === 'function' ? a(close) : a)) : null);
  bg.append(m);
  document.body.append(bg);
  $('#app').inert = true;
  m.querySelector('button,input,select,textarea')?.focus();
  return {close, el: m};
}
async function copyText(text, label = 'Copied') {
  try { await navigator.clipboard.writeText(text); toast(label, 'ok'); }
  catch { toast('Copy blocked here, select the text and press Ctrl+C', 'err'); }
}
function exportJson() { return JSON.stringify(cleanForExport(doc()), null, 2); }
function cleanForExport(d) {
  const out = clone(d);
  out.elements = out.elements.map(e => { const c = {...e}; delete c.locked; return c; });
  return out;
}
function validate(d) {
  const w = [];
  if (!d.id) w.push('The UI has no ID.');
  const ids = new Set();
  for (const e of d.elements) { if (ids.has(e.id)) w.push(`Duplicate element ID "${e.id}".`); ids.add(e.id); }
  for (const e of d.elements) for (const k of ['x', 'y', 'w', 'h', 'scale', 'rotation', 'opacity']) if (!Number.isFinite(e[k])) w.push(`${e.name}: ${k} must be a finite number.`);
  if (d.type === 'chest') for (const s of d.slots || []) if (s.slot >= d.settings.rows * 9) w.push(`Slot ${s.slot} is outside the inventory. Increase rows or remove this slot.`);
  const nodeIds = new Set(d.script.nodes.map(n => n.id));
  if (d.type === 'hud') {
    for (const e of d.elements) {
      if (e.type === 'item' || e.type === 'head' || e.type === 'model') w.push(`"${e.name}": items, heads and player models don't render on HUDs. Add them as sprites instead (Assets).`);
      if (/[^\x20-\x7e]/.test((e.text || '').replace(/<[^>]*>/g, ''))) w.push(`"${e.name}": HUD text only supports plain ASCII characters.`);
      if (e.rotation || (e.scale !== 1)) w.push(`"${e.name}": rotation and scale are ignored on HUDs.`);
    }
  }
  for (const n of d.script.nodes) {
    for (const target of Object.values(n.next || {}).flat()) if (!nodeIds.has(target)) w.push(`Script node ${n.id} points to missing node ${target}.`);
    const def = NODE_TYPES[n.type];
    if (!def) { w.push(`Unknown script node "${n.type}".`); continue; }
    for (const f of def.fields) if (f.kind === 'element' && !f.any && n.props[f.k] && !ids.has(n.props[f.k])) w.push(`${def.label}: element "${n.props[f.k]}" no longer exists.`);
    if (def.cat === 'event' && !(n.next?.then || []).length) w.push(`${def.label} is not connected to anything.`);
  }
  for (const c of d.commands || []) {
    if (c.function && !d.script.nodes.some(n => n.type === 'on_function' && n.props.name === c.function)) w.push(`/${c.name} runs function "${c.function}", which doesn't exist.`);
    for (const a of c.args) if (a.type === 'choice' && !a.choices.length) w.push(`/${c.name}: argument ${a.name} is a choice with no choices.`);
  }
  for (const n of d.script.nodes) if (n.type === 'call_function' && !n.props.ui && n.props.name && !d.script.nodes.some(f => f.type === 'on_function' && f.props.name === n.props.name)) w.push(`Call function: "${n.props.name}" has no "When function is called" node.`);
  if (d.type === 'screen' && !d.settings.command && !(d.commands || []).length && !d.script.nodes.some(n => n.type === 'on_command')) w.push('No command opens this UI yet. Set one in the UI panel (click empty canvas) or use /sui open ' + d.id + '.');
  return w;
}
function openExport() {
  const json = exportJson(), d = doc(), warns = validate(d);
  const kb = Math.round(json.length / 1024);
  const pre = h('pre', {}, json.length > 60000 ? json.slice(0, 60000) + '\n… (' + kb + ' KB total, use Copy to get everything)' : json);
  modal('Export ' + d.id + '.json', [
    h('p', {}, 'Save this as ', h('b', {}, d.id + '.json'), ' in ', h('code', {}, 'plugins/SilentUI/uis/'), ', then run ', h('code', {}, '/sui reload'), '. Or use Push to server to skip the file.'),
    warns.length ? h('div', {class: 'sec', style: {border: '1px solid var(--warn)', borderRadius: '8px', color: 'var(--warn)'}}, h('h4', {style: {color: 'var(--warn)'}}, 'Check before export'), ...warns.map(x => h('div', {}, '• ' + x))) : null,
    pre
  ], [
    h('span', {class: 'lab', style: {marginRight: 'auto'}}, kb + ' KB'),
    h('button', {class: 'btn', onclick: () => copyText(json, 'JSON copied')}, 'Copy JSON'),
    h('button', {class: 'btn primary', onclick: () => download(d.id + '.json', json)}, 'Download')
  ]);
}
function download(name, text) {
  try {
    const url = URL.createObjectURL(new Blob([text], {type: 'application/json'}));
    const a = h('a', {href: url, download: name}); document.body.append(a); a.click(); a.remove();
    setTimeout(() => URL.revokeObjectURL(url), 2000);
    toast('Download started (if nothing happens here, use Copy JSON)');
  } catch { toast('Downloads are blocked here, use Copy JSON', 'err'); }
}
function servedByPlugin() { return !new URLSearchParams(location.search).has('offline') && /^https?:$/.test(location.protocol) && /\/(editor\/?)?$/.test(location.pathname) && !/claude|usercontent/.test(location.hostname); }
function serverDefault() {
  if (servedByPlugin()) return location.origin;
  return store.get('server', '');
}
async function api(path, opts = {}) {
  const raw = ($('#srvUrl')?.value ?? SRV.base).trim(), token = ($('#srvToken')?.value ?? SRV.token).trim();
  let base;
  try { const url = new URL(raw); if (!['http:', 'https:'].includes(url.protocol)) throw new Error(); base = url.origin; }
  catch { throw new Error('Enter the server address, e.g. http://play.example.com:8163'); }
  store.set('server', base); store.set('token', token);
  if (base !== SRV.base || token !== SRV.token) connectServer(base, token, true);
  if (!base) throw new Error('Enter the server address, e.g. http://play.example.com:8163');
  const res = await fetch(base + path, {signal: AbortSignal.timeout(25000), ...opts, headers: {'Content-Type': 'application/json', 'X-SilentUI-Token': token, ...(opts.headers || {})}});
  const txt = await res.text();
  let data; try { data = JSON.parse(txt); } catch { data = {ok: false, error: txt.slice(0, 200)}; }
  if (!res.ok || data.ok === false) throw new Error(data.error || res.statusText);
  return data;
}
function openPush() {
  const list = h('div', {style: {display: 'flex', flexDirection: 'column', gap: '6px'}});
  const status = h('p', {}, '');
  const insecure = location.protocol === 'https:';
  modal('Server connection', [
    h('p', {}, 'Push sends this UI straight to your server, saves it in plugins/SilentUI/uis and reloads. Open the editor from your server (', h('code', {}, '/sui editor'), ') so this works without browser blocks.'),
    insecure ? h('p', {style: {color: 'var(--warn)'}}, 'This copy of the editor runs on https, so browsers block requests to an http:// server. Use the editor your plugin hosts, or Export instead.') : null,
    field('Server', h('input', {class: 'inp', id: 'srvUrl', value: serverDefault(), placeholder: 'http://your-server-ip:8163'})),
    field('Token', h('input', {class: 'inp', id: 'srvToken', value: store.get('token', ''), placeholder: 'web.upload-token from config.yml', type: 'password'})),
    status, list
  ], [
    h('button', {class: 'btn', onclick: async () => {
      status.textContent = 'Connecting…';
      const ok = await connectServer($('#srvUrl').value, $('#srvToken').value);
      status.textContent = ok ? (SRV.authenticated ? '✓ Connected and upload token verified.' : '✓ Assets connected. Add an upload token to load or push UIs.') : '✕ ' + (SRV.error || 'Could not reach the server.');
    }}, 'Connect'),
    h('button', {class: 'btn', onclick: async () => {
      status.textContent = 'Loading…';
      try {
        const r = await api('/api/uis');
        status.textContent = r.uis.length + ' UI(s) on the server:';
        list.replaceChildren(...r.uis.map(u => h('div', {style: {display: 'flex', alignItems: 'center', gap: '8px'}},
          h('span', {style: {flex: 1}}, u.name, ' ', h('span', {class: 'lab'}, u.id + ' · ' + u.type)),
          h('button', {class: 'btn sm', onclick: async () => { try { openTab(await api('/api/ui?id=' + encodeURIComponent(u.id))); toast('Opened ' + u.id, 'ok'); } catch (e) { toast(e.message, 'err'); } }}, 'Open'))));
      } catch (e) { status.textContent = '✕ ' + e.message; }
    }}, 'Load server UIs'),
    h('button', {class: 'btn primary', onclick: async () => {
      status.textContent = 'Pushing…';
      try { await api('/api/upload', {method: 'POST', body: exportJson()}); status.textContent = '✓ Pushed and reloaded on the server.'; toast('Pushed ' + doc().id, 'ok'); }
      catch (e) { status.textContent = '✕ ' + e.message; }
    }}, 'Push ' + doc().id)
  ]);
}
function openCode() {
  const ta = h('textarea', {class: 'code', rows: 22, spellcheck: 'false'});
  ta.value = exportJson();
  modal('Code · ' + doc().id + '.json', [h('p', {}, 'The exact file the plugin reads. You can edit it and apply.'), ta], [
    h('button', {class: 'btn', onclick: () => copyText(ta.value, 'JSON copied')}, 'Copy'),
    close => h('button', {class: 'btn primary', onclick: () => {
      try { const d = normalize(JSON.parse(ta.value)); pushUndo(); S.docs[S.cur] = d; S.sel = []; afterChange(); close(); toast('Applied', 'ok'); }
      catch (e) { toast('Invalid JSON: ' + e.message, 'err'); }
    }}, 'Apply')
  ]);
}
function openHelp() {
  const rows = [['V R T B I N G E H', 'Tools: select, rectangle, text, button, image, input, progress, item, head'], ['Drag on canvas', 'Draw with the active tool (hold Shift to keep the tool)'],
    ['Shift + drag', 'Lock axis / keep ratio / snap rotation to 15°'], ['Alt + drag', 'Duplicate while moving'], ['Arrows', 'Nudge 1px (Shift: 10px)'], ['Space + drag, scroll', 'Pan'],
    ['Ctrl + scroll', 'Zoom'], ['Ctrl+Z / Ctrl+Shift+Z', 'Undo / redo'], ['Ctrl+G / Ctrl+Shift+G', 'Group / ungroup'], ['L', 'Library (drag & drop)'], ['Ctrl+C / V / D', 'Copy / paste / duplicate'], ['[ and ]', 'Send to back / bring to front'],
    ['A / K / P', 'Timeline / scripts / preview'], ['Ctrl+E', 'Export']];
  modal('Silent UI editor', [
    h('p', {}, 'Build the UI here, export it (or push it), and the plugin draws it in game with display entities (screens and world UIs) or bossbar glyphs (HUDs). No client mods.'),
    h('div', {}, ...rows.map(([k, v]) => h('div', {class: 'row', style: {gridTemplateColumns: '170px 1fr'}}, h('code', {style: {color: 'var(--accent)'}}, k), h('span', {}, v)))),
    h('p', {}, 'Placeholders: {player} {level} {health} {online} {time} {date} {var:name} and any %placeholderapi% value. Text accepts MiniMessage tags like <white>, <bold>, <gray>.'),
    h('div', {class: 'toolbar'}, h('button', {class: 'btn primary', onclick: () => { $$('.modal-bg').forEach(x => x.remove()); startTutorial(0); }}, 'Start the tutorial'), h('button', {class: 'btn', onclick: () => { $$('.modal-bg').forEach(x => x.remove()); openPlaceholders(); }}, 'All placeholders'))
  ]);
}

/* ============================================================== sprites */
function openSprites() { openAssets('sprites'); }
function spritesView(closeAll) {
  const m = {close: closeAll};
  const grid = h('div', {class: 'sprites'});
  const fillGrid = () => {
    grid.replaceChildren(...doc().sprites.map(s => {
      const name = h('input', {class: 'inp', value: s.id, title: 'Sprite ID'});
      name.addEventListener('change', () => renameSprite(s, name.value));
      return h('div', {class: 'sprite'}, h('div', {class: 'th'}, h('img', {src: s.data, alt: s.id})), name,
        h('div', {class: 'meta'}, s.width + '×' + s.height + ' · ' + Math.round(s.data.length * .75 / 1024 * 10) / 10 + ' KB'),
        h('div', {class: 'acts'},
          h('button', {class: 'btn sm', onclick: () => { m.close(); addElement('image', {sprite: s.id, w: s.width, h: s.height, x: Math.round(doc().canvas.width / 2 - s.width / 2), y: Math.round(doc().canvas.height / 2 - s.height / 2), name: s.id}); }}, 'Place'),
          h('button', {class: 'btn sm danger', onclick: () => { mutate(d => d.sprites = d.sprites.filter(x => x !== s)); fillGrid(); }}, 'Delete')));
    }));
    if (!doc().sprites.length) grid.append(h('p', {style: {gridColumn: '1/-1'}}, 'No sprites yet. Drop PNGs above or generate one below.'));
  };
  const dz = h('div', {class: 'drop'}, 'Drop PNG files here, paste an image, or ', h('button', {class: 'btn sm', onclick: () => $('#fileSprite').click()}, 'Choose files'));
  dz.addEventListener('dragover', ev => { ev.preventDefault(); dz.classList.add('over'); });
  dz.addEventListener('dragleave', () => dz.classList.remove('over'));
  dz.addEventListener('drop', async ev => { ev.preventDefault(); dz.classList.remove('over'); await importImages(ev.dataTransfer.files); fillGrid(); });
  spriteRefresh = fillGrid;
  // generator
  const g = {kind: 'rounded', w: 64, h: 24, r: 5, fill: '#2a2633', fill2: '#16131b', alpha: 1, border: '#ff8fd1', bw: 0, icon: '', iconColor: '#ffffff', vgrad: false};
  const prev = h('img', {style: {imageRendering: 'pixelated', maxWidth: '100%', maxHeight: '110px', minWidth: '40px'}});
  const upd = () => { prev.src = genShape(shapeOpts(g)); };
  const gi = (label, k, type = 'number', extra = {}) => {
    const i = h('input', {class: 'inp', type, value: g[k], ...extra});
    i.addEventListener('input', () => { g[k] = type === 'number' ? +i.value : i.value; upd(); });
    return field(label, i);
  };
  const kindSel = h('select', {class: 'inp'}, ...[['rounded', 'Rounded panel'], ['circle', 'Circle'], ['pill', 'Pill'], ['gradient', 'Gradient bar'], ['vgrad', 'Vertical gradient panel'], ['icon', 'Pixel icon']].map(([v, l]) => h('option', {value: v}, l)));
  kindSel.addEventListener('change', () => { g.kind = kindSel.value; upd(); });
  const iconSel = h('select', {class: 'inp'}, h('option', {value: ''}, 'none'), ...['close', 'gem', 'clock', 'user', 'star', 'home', 'cart', 'gear', 'portal', 'chart'].map(v => h('option', {value: v}, v)));
  iconSel.addEventListener('change', () => { g.icon = iconSel.value; upd(); });
  const nameIn = h('input', {class: 'inp', placeholder: 'panel_dark', value: 'shape'});
  const gen = h('div', {class: 'sec', style: {border: '1px solid var(--line)', borderRadius: '10px'}},
    h('h4', {}, 'Generate a sprite'),
    h('div', {style: {display: 'grid', gridTemplateColumns: 'minmax(0,1fr) 150px', gap: '14px'}},
      h('div', {}, field('Shape', kindSel), h('div', {class: 'row2'}, gi('W', 'w', 'number', {min: 1, max: 256}), gi('H', 'h', 'number', {min: 1, max: 256})),
        gi('Radius', 'r'), gi('Colour', 'fill', 'color'), gi('Colour 2', 'fill2', 'color'), gi('Opacity', 'alpha', 'number', {step: .05, min: 0, max: 1}),
        gi('Border', 'border', 'color'), gi('Border px', 'bw'), field('Icon', iconSel), gi('Icon colour', 'iconColor', 'color'), field('ID', nameIn)),
      h('div', {style: {display: 'grid', placeItems: 'center', background: 'var(--stage)', borderRadius: '8px', minHeight: '120px'}}, prev)),
    h('div', {style: {display: 'flex', justifyContent: 'flex-end', marginTop: '8px'}},
      h('button', {class: 'btn primary', onclick: async () => { await addSpriteFromDataUrl(genShape(shapeOpts(g)), nameIn.value || 'shape'); fillGrid(); toast('Sprite created', 'ok'); }}, 'Create sprite')));
  upd();
  fillGrid();
  return h('div', {style: {display: 'flex', flexDirection: 'column', gap: '10px'}}, dz, grid, gen);
}
let spriteRefresh = null;
function shapeOpts(g) {
  const o = {w: clamp(Math.round(g.w) || 1, 1, 256), h: clamp(Math.round(g.h) || 1, 1, 256), r: g.r, fill: g.fill, fill2: g.fill2, alpha: g.alpha, border: g.border, bw: g.bw, icon: g.icon, iconColor: g.iconColor};
  if (g.kind === 'circle') o.kind = 'circle';
  if (g.kind === 'pill') o.r = 999;
  if (g.kind === 'gradient') o.kind = 'gradient';
  if (g.kind === 'vgrad') o.vgrad = true;
  if (g.kind === 'icon') { o.alpha = 0; o.bw = 0; if (!o.icon) o.icon = 'star'; }
  return o;
}
function renameSprite(s, raw) {
  const id = slug(raw);
  if (!id || id === s.id) return;
  if (doc().sprites.some(x => x.id === id)) return toast('Sprite ID already used', 'err');
  const old = s.id;
  mutate(d => {
    s.id = id;
    for (const e of d.elements) for (const k of ['sprite', 'hoverSprite', 'barSprite']) if (e[k] === old) e[k] = id;
    if (d.settings.cursorSprite === old) d.settings.cursorSprite = id;
    for (const n of d.script.nodes) if (n.props.sprite === old) n.props.sprite = id;
  });
}
async function importImages(files) {
  for (const f of files) {
    if (!f.type.startsWith('image/')) continue;
    const url = await new Promise(r => { const fr = new FileReader(); fr.onload = () => r(fr.result); fr.readAsDataURL(f); });
    try { await addSpriteFromDataUrl(url, f.name.replace(/\.[^.]+$/, '')); } catch { toast('Could not read ' + f.name, 'err'); }
  }
  toast('Sprites added', 'ok');
}
$('#fileSprite').addEventListener('change', async ev => { await importImages(ev.target.files); ev.target.value = ''; if (spriteRefresh) spriteRefresh(); });
$('#fileOpen').addEventListener('change', async ev => {
  const f = ev.target.files[0]; ev.target.value = '';
  if (!f) return;
  try { openTab(JSON.parse(await f.text())); toast('Opened ' + f.name, 'ok'); } catch (e) { toast('Not a Silent UI file: ' + e.message, 'err'); }
});
document.addEventListener('paste', async ev => {
  if (ev.target.closest('input,textarea')) return;
  const files = [...(ev.clipboardData?.files || [])].filter(f => f.type.startsWith('image/'));
  if (files.length) { ev.preventDefault(); await importImages(files); if (spriteRefresh) spriteRefresh(); }
});

/* ============================================================== preview (runs the UI like the plugin does) */
const pvEl = $('#preview');
let PV = null;
function startPreview() {
  stopPlay();
  closeMenus();
  const d = clone(doc());
  PV = {d, states: {}, pages: {}, hover: null, anims: [], vars: {}, ticks: 0, acc: 0, last: performance.now(), raf: 0, chat: [], mouse: null, closed: false, input: null, slot: 0, fadeUntil: 0, title: null};
  for (const e of d.elements) PV.states[e.id] = {...e};
  for (const [set, page] of Object.entries(d.tabs || {})) pvSwitch(set, page);
  for (const a of d.animations) if (a.autoplay) pvPlay(a.name);
  pvEl.hidden = false;
  pvEl.replaceChildren(h('div', {class: 'scene'}), h('div', {class: 'pv-dim', style: {opacity: d.type === 'screen' ? d.settings.dim : 0}}),
    h('div', {class: 'pv-board', id: 'pvBoard'}), h('div', {class: 'pv-title', id: 'pvTitle'}), h('div', {class: 'pv-fade', id: 'pvFade'}),
    h('div', {class: 'chat', id: 'pvChat'}), h('div', {class: 'hotbar', id: 'pvHotbar'}), h('div', {class: 'xhair', id: 'pvX'}),
    h('div', {class: 'pv-bar'}, h('b', {}, 'Preview'), h('span', {class: 'lab'}, d.type === 'hud' ? 'HUD at the top of the screen' : 'Aim and click. WASD, Space, Shift, Q, F, 1-9 and scroll fire key events.'),
      h('button', {class: 'btn sm', onclick: stopPreview}, 'Exit (Esc)')));
  pvChat('<gray>Preview started. Messages and commands show here.</gray>');
  pvRun('on_open');
  PV.raf = requestAnimationFrame(pvFrame);
}
function stopPreview() {
  if (!PV) return;
  if (!PV.closed) pvRun('on_close');
  cancelAnimationFrame(PV.raf);
  PV = null;
  pvEl.hidden = true;
  pvEl.replaceChildren();
}
function pvLayout() {
  const r = pvEl.getBoundingClientRect(), d = PV.d;
  if (d.type === 'hud') { const z = Math.max(1, Math.floor(r.width / 480 * 2) / 2); return {z, x: r.width / 2 - d.canvas.width / 2 * z, y: 0}; }
  const z = Math.min((r.width - 60) / d.canvas.width, (r.height - 110) / d.canvas.height) * (d.type === 'screen' ? d.settings.screenSize || 1 : 1);
  return {z, x: (r.width - d.canvas.width * z) / 2, y: (r.height - d.canvas.height * z) / 2};
}
function pvFrame(now) {
  if (!PV) return;
  const dt = Math.min(250, now - PV.last); PV.last = now;
  PV.acc += dt;
  while (PV.acc >= 50) { PV.acc -= 50; pvTick(); if (!PV) return; }
  // animations sample continuously for smooth frames
  for (const a of PV.anims) pvApply(a, Math.min(a.def.length, a.t + PV.acc / 50));
  const L = pvLayout(), b = $('#pvBoard');
  b.style.transform = `translate(${L.x}px,${L.y}px) scale(${L.z})`;
  b.style.width = PV.d.canvas.width + 'px'; b.style.height = PV.d.canvas.height + 'px';
  const signature = JSON.stringify([PV.states, PV.hover, PV.vars, PV.d.slots?.map(s => [s, s.frames?.length ? Math.floor(PV.ticks / Math.max(1, s.frameTicks)) : 0])]);
  if (signature !== PV.drawSignature) {
    b.replaceChildren(...PV.d.elements.map(e => elNode(e, PV.states[e.id], {preview: true, hover: PV.hover === e.id, vars: PV.vars})));
    if (PV.d.type === 'chest') renderChestSlots(PV.d, b, true);
    PV.drawSignature = signature;
  }
  const chat = PV.chat.slice(-8).join('\n');
  if (chat !== PV.chatSignature) { $('#pvChat').replaceChildren(...PV.chat.slice(-8).map(c => h('div', {html: c}))); PV.chatSignature = chat; }
  if (PV.slot !== PV.hotbarSlot) { $('#pvHotbar').replaceChildren(...Array.from({length: 9}, (_, i) => h('i', {class: i === PV.slot ? 'on' : ''}))); PV.hotbarSlot = PV.slot; }
  const t = $('#pvTitle'); t.innerHTML = PV.title && now < PV.title.until ? PV.title.html : '';
  const f = $('#pvFade'); if (PV.fade) { const p = now - PV.fade.start, {i, s, o} = PV.fade; f.style.background = PV.fade.color; f.style.opacity = p < i ? p / i : p < i + s ? 1 : p < i + s + o ? 1 - (p - i - s) / o : 0; if (p > i + s + o) PV.fade = null; }
  PV.raf = requestAnimationFrame(pvFrame);
}
function pvTick() {
  PV.ticks++;
  for (const a of [...PV.anims]) {
    a.t++;
    if (a.t > a.def.length) {
      pvApply(a, a.def.length);
      if (a.def.loop) a.t = 0; else PV.anims.splice(PV.anims.indexOf(a), 1);
    }
  }
  for (const n of PV.d.script.nodes) if (n.type === 'on_timer') {
    const iv = Math.max(1, Math.round((+n.props.seconds || 1) * 20));
    if (PV.ticks % iv === 0) pvChain(n.next?.then || [], {});
  }
}
function pvSwitch(set, page) {
  if (!set) return;
  PV.pages[set] = page;
  for (const e of PV.d.elements) {
    const st = PV.states[e.id];
    if ((e.group || '').startsWith(set + '/')) st.visible = e.group === set + '/' + page || e.group.startsWith(set + '/' + page + '/');
    if ((e.tabFor || '').startsWith(set + '/')) st.active = e.tabFor === set + '/' + page;
  }
}
function pvGroup(group, vis) {
  for (const e of PV.d.elements) if (e.group && (e.group === group || e.group.startsWith(group + '/'))) PV.states[e.id].visible = vis == null ? !PV.states[e.id].visible : vis;
}
function pvCall(name, args, depth = 0) {
  if (depth > 16) return;
  for (const n of PV.d.script.nodes) if (n.type === 'on_function' && (n.props.name || '').toLowerCase() === String(name).toLowerCase()) pvChain(n.next?.then || [], args);
}
function pvPlay(name) {
  const a = PV.d.animations.find(x => x.name === name || x.id === name);
  if (!a) return;
  PV.anims = PV.anims.filter(x => x.def !== a);
  const ent = {def: a, t: 0};
  PV.anims.push(ent);
  pvApply(ent, 0);
}
function pvApply(a, t) {
  for (const tr of a.def.tracks) {
    const s = PV.states[tr.element];
    if (!s || !tr.keys.length) continue;
    const p = tr.property === 'color' ? (['text', 'button', 'input'].includes(s.type) ? 'textColor' : 'fill') : tr.property;
    s[p] = COLOR_PROPS.includes(tr.property) ? sampleColor(tr.keys, t) : sampleKeys(tr.keys, t);
  }
}
function pvChat(html) { if (PV) PV.chat.push(mmToHtml(html)); }
function pvRun(type, filter = () => true, extra = {}) {
  if (!PV) return;
  for (const n of PV.d.script.nodes) if (n.type === type && filter(n)) pvChain(n.next?.then || [], extra);
}
function pvChain(ids, extra, depth = 0) {
  const queue = [...ids];
  let steps = 0;
  while (queue.length && PV && steps++ < 512) {
    const nextId = queue.shift();
    const n = PV.d.script.nodes.find(x => x.id === nextId);
    if (!n) continue;
    if (n.type === 'wait') { const rest = [...(n.next?.then || []), ...queue]; setTimeout(() => PV && pvChain(rest, extra), (+n.props.ticks || 20) * 50); return; }
    const port = pvExec(n, extra);
    if (port) queue.push(...(n.next?.[port] || []));
  }
}
function pvRes(s, extra) { let o = resolve(s, PV.vars); for (const [k, v] of Object.entries(extra)) o = o.split('{' + k + '}').join(v); return o; }
function pvCompare(a, op, b) {
  const na = parseFloat(a), nb = parseFloat(b), num = !isNaN(na) && !isNaN(nb);
  switch (op) { case '!=': return num ? na !== nb : a !== b; case '>': return num && na > nb; case '>=': return num && na >= nb; case '<': return num && na < nb; case '<=': return num && na <= nb;
    case 'contains': return String(a).toLowerCase().includes(String(b).toLowerCase()); default: return num ? na === nb : String(a).toLowerCase() === String(b).toLowerCase(); }
}
function pvExec(n, x) {
  const p = n.props, st = PV.states[p.element];
  switch (n.type) {
    case 'show': if (st) st.visible = true; break;
    case 'hide': if (st) st.visible = false; break;
    case 'toggle': if (st) st.visible = !st.visible; break;
    case 'set_text': if (st) st.text = PV.d.type === 'world' ? pvRes(p.text || '', x) : (p.text || ''); break;
    case 'set_color': if (st) { if (st.type === 'text') st.textColor = p.color; else st.fill = p.color; } break;
    case 'set_text_color': if (st) st.textColor = p.color; break;
    case 'set_sprite': if (st) st.sprite = p.sprite || ''; break;
    case 'set_progress': if (st) { st.value = p.value || '0'; if (p.max) st.max = p.max; } break;
    case 'move': if (st) { const xx = Number(pvRes(p.x ?? st.x, x)), yy = Number(pvRes(p.y ?? st.y, x)); if (Number.isFinite(xx)) st.x = xx; if (Number.isFinite(yy)) st.y = yy; } break;
    case 'play_animation': pvPlay(p.animation); break;
    case 'switch_tab': pvSwitch(p.tabset, pvRes(p.page || '', x)); break;
    case 'next_tab': { const pages = tabsets(PV.d)[p.tabset] || []; if (pages.length) { const i = pages.indexOf(PV.pages[p.tabset]), delta = p.direction === 'previous' ? -1 : 1; pvSwitch(p.tabset, pages[(i + delta + pages.length) % pages.length]); } break; }
    case 'set_slot': {
      if (PV.d.type !== 'chest') break;
      const index = Number(pvRes(p.slot || '0', x));
      if (!Number.isInteger(index) || index < 0 || index >= PV.d.settings.rows * 9) break;
      let item = PV.d.slots.find(s => s.slot === index);
      if (!item) PV.d.slots.push(item = slotDefaults(index));
      for (const k of ['item', 'name', 'lore']) if (p[k]) item[k] = pvRes(p[k], x);
      if (p.amount) item.amount = clamp(Number(pvRes(p.amount, x)) || 1, 1, 99);
      break;
    }
    case 'show_group': pvGroup(p.group, true); break;
    case 'hide_group': pvGroup(p.group, false); break;
    case 'toggle_group': pvGroup(p.group, null); break;
    case 'set_item': if (st) st.item = pvRes(p.item || '', x); break;
    case 'set_head': if (st) st.headOwner = pvRes(p.owner || '', x); break;
    case 'rotate': if (st) st.turn = (st.turn || 0) + Number(p.degrees || 90); break;
    case 'give_item': pvChat(`<gray>Silent receives</gray> <white>${pvRes(p.amount || '1', x)}× ${pvRes(p.item || '', x).replace('minecraft:', '')}</white>`); break;
    case 'call_function': {
      const args = {...x};
      for (const part of String(p.args || '').split(',')) { const i = part.indexOf('='); if (i > 0) args[part.slice(0, i).trim()] = pvRes(part.slice(i + 1).trim(), x); }
      if (p.ui && p.ui !== PV.d.id) { pvChat(`<dark_gray>calls ${p.ui}:${p.name}</dark_gray>`); break; }
      pvCall(p.name, args);
      break;
    }
    case 'stop_animation': PV.anims = PV.anims.filter(a => a.def.name !== p.animation && a.def.id !== p.animation); break;
    case 'open_ui': {
      const i = S.docs.findIndex(dd => dd.id === p.ui);
      pvChat(`<gray>Open UI</gray> <#ff8fd1>${p.ui}</#ff8fd1>` + (i < 0 ? ' <dark_gray>(not open in a tab)</dark_gray>' : ''));
      if (i >= 0) { setTimeout(() => { stopPreview(); switchTab(i); startPreview(); }, 0); }
      break;
    }
    case 'close_ui': if (PV.d.type !== 'hud') { setTimeout(stopPreview, 0); } break;
    case 'focus_input': pvAsk(p.element); break;
    case 'run_command': pvChat(`<dark_gray>${p.as === 'console' ? 'console' : 'Silent'} runs</dark_gray> <white>/${pvRes(p.command || '', x).replace(/^\//, '')}</white>`); break;
    case 'message': case 'broadcast': pvChat(pvRes(p.text || '', x)); break;
    case 'actionbar': pvChat('<gray>[action bar]</gray> ' + pvRes(p.text || '', x)); break;
    case 'title': PV.title = {html: mmToHtml(pvRes(p.title || '', x)) + '<div style="font-size:20px;font-weight:400">' + mmToHtml(pvRes(p.subtitle || '', x)) + '</div>', until: performance.now() + ((+p.in || 0) + (+p.stay || 40) + (+p.out || 0)) * 50}; break;
    case 'sound': pvChat(`<dark_gray>♪ ${p.sound}</dark_gray>`); break;
    case 'screen_fade': PV.fade = {color: p.color || '#000', i: (+p.in || 0) * 50, s: (+p.stay || 0) * 50, o: (+p.out || 0) * 50, start: performance.now()}; break;
    case 'set_var': PV.vars[p.name || 'var'] = pvRes(p.value ?? '', x); break;
    case 'add_var': { const k = p.name || 'var'; const v = (parseFloat(PV.vars[k]) || 0) + (parseFloat(pvRes(p.amount ?? '1', x)) || 0); PV.vars[k] = String(Math.round(v * 1000) / 1000); break; }
    case 'if_var': return pvCompare(PV.vars[p.name || 'var'] ?? '', p.op || '==', pvRes(p.value ?? '', x)) ? 'true' : 'false';
    case 'if_compare': return pvCompare(pvRes(p.left ?? '', x), p.op || '==', pvRes(p.right ?? '', x)) ? 'true' : 'false';
    case 'if_permission': return 'true';
    case 'random': return Math.random() * 100 < (+p.chance || 50) ? 'true' : 'false';
  }
  return 'then';
}
function pvAsk(id) {
  const st = PV.states[id]; if (!st) return;
  pvChat('<#ff8fd1>✎</#ff8fd1> <gray>Type your text and press Enter.</gray>');
  const inp = h('input', {class: 'inp', style: {position: 'absolute', left: '12px', right: '12px', bottom: '8px', zIndex: 6, maxWidth: '520px'}, placeholder: 'Type in chat…'});
  pvEl.append(inp); inp.focus();
  inp.addEventListener('keydown', ev => {
    ev.stopPropagation();
    if (ev.key === 'Enter') {
      const text = inp.value.replace(/[<>\\{}%&]/g, '').trim().slice(0, 96); inp.remove();
      if (!text || text.toLowerCase() === 'cancel') return;
      st.text = text; PV.vars['input_' + id] = text;
      pvRun('on_text', n => !n.props.element || n.props.element === id, {text});
    }
    if (ev.key === 'Escape') inp.remove();
  });
}
function pvHit(ev) {
  const r = pvEl.getBoundingClientRect(), L = pvLayout();
  const p = {x: (ev.clientX - r.left - L.x) / L.z, y: (ev.clientY - r.top - L.y) / L.z};
  for (let i = PV.d.elements.length - 1; i >= 0; i--) {
    const e = PV.d.elements[i], s = PV.states[e.id];
    if (!s.visible || s.blocked || s.opacity <= 0.01) continue;
    const pv = pivotOf(s, e.anchor), sc = (s.scale || 1) * (PV.hover === e.id ? e.hoverScale || 1 : 1);
    let dx = p.x - pv[0], dy = p.y - pv[1];
    if (s.rotation) { const rr = -s.rotation * Math.PI / 180, c = Math.cos(rr), sn = Math.sin(rr); [dx, dy] = [dx * c - dy * sn, dx * sn + dy * c]; }
    const lx = pv[0] + dx / sc, ly = pv[1] + dy / sc;
    if (lx >= s.x && lx <= s.x + s.w && ly >= s.y && ly <= s.y + s.h) return e.id;
  }
  return null;
}
pvEl.addEventListener('pointermove', ev => {
  if (!PV) return;
  const r = pvEl.getBoundingClientRect(), x = $('#pvX');
  if (x) { x.style.left = (ev.clientX - r.left) + 'px'; x.style.top = (ev.clientY - r.top) + 'px'; }
  if (PV.d.type === 'hud') return;
  const id = pvHit(ev);
  if (id !== PV.hover) {
    const old = PV.hover; PV.hover = id;
    if (old) pvRun('on_hover_leave', n => !n.props.element || n.props.element === old);
    if (id) pvRun('on_hover_enter', n => !n.props.element || n.props.element === id);
  }
});
const pvClick = (ev, button) => {
  if (!PV || ev.target.closest('.pv-bar,input')) return;
  ev.preventDefault();
  if (PV.d.type === 'chest') {
    const r = pvEl.getBoundingClientRect(), L = pvLayout();
    const x = (ev.clientX - r.left - L.x) / L.z - 8, y = (ev.clientY - r.top - L.y) / L.z - PV.d.settings.guiTop - 18;
    const col = Math.floor(x / 18), row = Math.floor(y / 18), index = row * 9 + col;
    if (col >= 0 && col < 9 && row >= 0 && row < PV.d.settings.rows) pvRun('on_slot_click', n => slotMatches(n.props.slot, index) && (!n.props.button || n.props.button === 'any' || n.props.button === (ev.shiftKey ? 'shift' : button)), {slot: String(index), button, shift: String(ev.shiftKey)});
  }
  if (!PV) return;
  const id = PV.d.type === 'hud' ? null : pvHit(ev);
  if (!id) return;
  pvRun('on_click', n => (!n.props.element || n.props.element === id) && (!n.props.button || n.props.button === 'any' || n.props.button === button), {element: id, button});
  const tf = PV && PV.d.elements.find(e => e.id === id)?.tabFor;
  if (tf && tf.lastIndexOf('/') > 0) pvSwitch(tf.slice(0, tf.lastIndexOf('/')), tf.slice(tf.lastIndexOf('/') + 1));
  if (PV && PV.states[id]?.type === 'input') pvAsk(id);
};
pvEl.addEventListener('click', ev => pvClick(ev, 'left'));
pvEl.addEventListener('contextmenu', ev => pvClick(ev, 'right'));
pvEl.addEventListener('wheel', ev => {
  if (!PV) return; ev.preventDefault();
  PV.slot = (PV.slot + (ev.deltaY > 0 ? 1 : 8)) % 9;
  pvKey(ev.deltaY > 0 ? 'SCROLL_DOWN' : 'SCROLL_UP');
}, {passive: false});
function pvKey(key) {
  if (!PV) return;
  pvRun('on_key', n => !n.props.key || n.props.key === 'any' || n.props.key === key, {key});
  if (key === 'SNEAK' && PV && PV.d.type === 'screen' && PV.d.settings.closeOnSneak) stopPreview();
}

/* ============================================================== keyboard */
document.addEventListener('keydown', ev => {
  if ($('.modal-bg') || ev.defaultPrevented) return;
  if ((ev.ctrlKey || ev.metaKey) && ev.key.toLowerCase() === 'k') { ev.preventDefault(); openCommandPalette(); return; }
  const typing = ev.target.closest('input,textarea,select,[contenteditable]');
  if (PV) {
    if (typing) return;
    if (ev.key === 'Escape') return stopPreview();
    const map = {w: 'W', a: 'A', s: 'S', d: 'D', ' ': 'JUMP', shift: 'SNEAK', control: 'SPRINT', q: 'DROP', f: 'SWAP'};
    const k = map[ev.key.toLowerCase()];
    if (/^[1-9]$/.test(ev.key)) { PV.slot = +ev.key - 1; pvKey('SLOT_' + ev.key); }
    else if (k && !ev.repeat) { ev.preventDefault(); pvKey(k); }
    return;
  }
  if (typing) return;
  const mod = ev.ctrlKey || ev.metaKey, k = ev.key.toLowerCase();
  if (k === ' ' && !S.scriptOpen) { if (S.dock && document.activeElement?.closest?.('#dock')) { ev.preventDefault(); togglePlay(); return; } S.spaceDown = true; stage.style.cursor = 'grab'; ev.preventDefault(); return; }
  if (mod && k === 'z') { ev.preventDefault(); ev.shiftKey ? redo() : undo(); return; }
  if (mod && k === 'y') { ev.preventDefault(); redo(); return; }
  if (mod && k === 'g') { ev.preventDefault(); ev.shiftKey ? ungroupSel() : groupSel(); return; }
  if (mod && k === 'c') { copySel(); return; }
  if (mod && k === 'v') { paste(); return; }
  if (mod && k === 'd') { ev.preventDefault(); duplicate(); return; }
  if (mod && k === 'a') { ev.preventDefault(); S.sel = els().filter(e => !e.locked).map(e => e.id); render(); return; }
  if (mod && k === 'e') { ev.preventDefault(); openExport(); return; }
  if (mod && k === 'o') { ev.preventDefault(); $('#fileOpen').click(); return; }
  if (mod && k === 's') { ev.preventDefault(); const ok = saveLocal(); toast(ok ? 'Saved in this browser' : 'Storage is full. Export JSON to keep your changes.', ok ? 'ok' : 'err'); return; }
  if (mod && k === 'h') { ev.preventDefault(); mutate(() => selected().forEach(e => e.visible = !e.visible)); return; }
  if (mod && k === 'l') { ev.preventDefault(); mutate(() => selected().forEach(e => e.locked = !e.locked)); return; }
  if (mod) return;
  if (S.scriptOpen) {
    if ((k === 'delete' || k === 'backspace') && SC.sel) { deleteNode(SC.sel); SC.sel = null; }
    if (k === 'escape' || k === 'k') toggleScript();
    return;
  }
  if (k === 'delete' || k === 'backspace') { removeSel(); return; }
  if (k === 'escape') { S.sel = []; setTool('select'); render(); return; }
  if (k.startsWith('arrow')) { ev.preventDefault(); const s = ev.shiftKey ? 10 : 1; nudge(k === 'arrowleft' ? -s : k === 'arrowright' ? s : 0, k === 'arrowup' ? -s : k === 'arrowdown' ? s : 0); return; }
  if (ev.key === '!' || (ev.shiftKey && k === '1')) { fit(); return; }
  const tools = {v: 'select', r: 'rect', t: 'text', b: 'button', i: 'image', n: 'input', g: 'progress', e: 'item', h: 'head', m: 'model'};
  if (tools[k]) { setTool(tools[k]); return; }
  if (k === 'a') toggleDock();
  if (k === 'l') toggleLibrary();
  if (k === 'k') toggleScript();
  if (k === 'p') startPreview();
  if (k === '?') openHelp();
  if (k === ']') arrange('top');
  if (k === '[') arrange('bottom');
});
document.addEventListener('keyup', ev => { if (ev.key === ' ') { S.spaceDown = false; stage.style.cursor = S.tool === 'select' ? '' : 'crosshair'; } });

/* ============================================================== render + boot */
function render(opts = {}) {
  renderTabs();
  renderRail();
  renderCanvas();
  renderPanel();
  renderTimeline();
  if (opts.keepScript && S.scriptOpen && $('#nodeLayer')) renderNodes(); else renderScript();
  renderStatus();
  renderPageStrip();
  renderNavigator();
  renderSaveState();
  renderWorkspace();
  if (S.library && libTab === 'sprites') renderLibrary();
}
$('#undoBtn').addEventListener('click', undo);
$('#redoBtn').addEventListener('click', redo);
$('#exportBtn').addEventListener('click', openExport);
$('#pushBtn').addEventListener('click', openPush);
$('#previewBtn').addEventListener('click', startPreview);
$('#zoomIn').addEventListener('click', () => zoomAt(1.25));
$('#zoomOut').addEventListener('click', () => zoomAt(0.8));
$('#zoomFit').addEventListener('click', fit);
window.addEventListener('resize', () => S.autoFit ? fit() : renderCanvas());
renderMenus();
$('#srvChip').addEventListener('click', openPush);
/* ============================================================== server connection + Minecraft assets */
const SRV = {base: '', token: '', state: 'offline', version: '', items: null, authenticated: false, error: ''};
let connectAttempt = 0, connectTimer = 0;
function srvBase() { return SRV.base.replace(/\/+$/, ''); }
function connected() { return !!SRV.base && SRV.state !== 'offline'; }
const assetsReady = () => SRV.state === 'ready';
const iconUrl = (id, size = 64) => assetsReady() ? `${srvBase()}/assets/icon/${String(id).replace(/^minecraft:/, '').toLowerCase()}.png?size=${size}` : '';
const headUrl = (name, size = 64) => connected() ? `${srvBase()}/assets/head/${encodeURIComponent(name || 'Steve')}.png?size=${size}` : '';
const bodyUrl = (name, size = 128) => connected() ? `${srvBase()}/assets/body/${encodeURIComponent(name || 'Steve')}.png?size=${size}` : '';
const textureUrl = rel => assetsReady() ? `${srvBase()}/assets/texture/${rel}.png` : '';
async function connectServer(base, token, quiet) {
  const attempt = ++connectAttempt;
  clearTimeout(connectTimer);
  let address = (base || '').trim().replace(/\/+$/, '');
  if (address) {
    try { const parsed = new URL(address); if (!['http:', 'https:'].includes(parsed.protocol)) throw new Error(); address = parsed.origin; }
    catch { SRV.error = 'Enter an http:// or https:// server address.'; if (!quiet) toast(SRV.error, 'err'); return false; }
  }
  if (address !== SRV.base) SRV.items = null;
  SRV.base = address;
  SRV.token = (token || '').trim();
  SRV.authenticated = false; SRV.error = '';
  store.set('server', SRV.base); store.set('token', SRV.token);
  if (!SRV.base) { SRV.state = 'offline'; renderSrv(); return false; }
  try {
    const r = await fetch(address + '/assets/status', {signal: AbortSignal.timeout(10000)});
    if (!r.ok) throw new Error('Could not reach the Silent UI asset API.');
    const j = await r.json();
    if (attempt !== connectAttempt) return false;
    SRV.state = j.state === 'ready' ? 'ready' : j.state === 'loading' ? 'loading' : 'online';
    SRV.version = j.version || '';
    if (SRV.token) {
      const ping = await fetch(address + '/api/ping', {headers: {'X-SilentUI-Token': SRV.token}, signal: AbortSignal.timeout(10000)});
      if (attempt !== connectAttempt) return false;
      SRV.authenticated = ping.ok;
      if (!ping.ok) { SRV.error = 'Server reached, but the upload token is invalid. Copy web.upload-token from config.yml.'; renderSrv(); if (!quiet) toast(SRV.error, 'err'); return false; }
    }
    if (SRV.state === 'loading') connectTimer = setTimeout(() => connectServer(SRV.base, SRV.token, true), 4000);
    if (!quiet) toast(SRV.state === 'ready' ? 'Connected · Minecraft ' + SRV.version + ' assets ready' : 'Connected · assets ' + j.state, 'ok');
  } catch (e) {
    if (attempt !== connectAttempt) return false;
    SRV.state = 'offline';
    SRV.error = 'Could not reach the server. Check the address and web.port.';
    if (!quiet) toast('Could not reach ' + SRV.base + (location.protocol === 'https:' ? ' (https pages cannot call http servers)' : ''), 'err');
  }
  renderSrv();
  renderCanvas();
  return SRV.state !== 'offline';
}
function renderSrv() {
  const c = $('#srvChip'); if (!c) return;
  c.classList.toggle('on', connected());
  c.querySelector('span').textContent = SRV.state === 'ready' ? 'Server · MC ' + SRV.version : SRV.state === 'loading' ? 'Server · loading assets…' : connected() ? 'Server · no assets' : 'Local workspace';
  c.setAttribute('aria-label', connected() ? 'Server connection' : 'Connect to a server');
}
async function loadItems() {
  if (SRV.items) return SRV.items;
  if (!assetsReady()) return [];
  try { SRV.items = await (await fetch(srvBase() + '/assets/items')).json(); } catch { SRV.items = []; }
  return SRV.items;
}
async function urlToDataUrl(url) {
  const blob = await (await fetch(url)).blob();
  return await new Promise(r => { const fr = new FileReader(); fr.onload = () => r(fr.result); fr.readAsDataURL(blob); });
}
function needServer(where) {
  return h('div', {class: 'drop'}, h('p', {style: {margin: '0 0 8px'}}, `${where} come from your server's Minecraft files. Connect to the server running Silent UI first.`),
    h('button', {class: 'btn primary', onclick: openPush}, 'Connect to server'));
}

/* ============================================================== assets browser (sprites, items, textures, players) */
let assetsTab = 'sprites';
function openAssets(tab) {
  if (tab) assetsTab = tab;
  let m;
  const body = h('div', {style: {display: 'flex', flexDirection: 'column', gap: '10px'}});
  const tabs = h('div', {class: 'mtabs'});
  const draw = () => {
    tabs.replaceChildren(...[['sprites', 'Sprites'], ['items', 'Items'], ['textures', 'Textures'], ['players', 'Players']].map(([k, l]) =>
      h('button', {class: assetsTab === k ? 'on' : '', onclick: () => { assetsTab = k; draw(); }}, l)));
    const view = assetsTab === 'items' ? itemsView({onPick: null, close: () => m.close()}) : assetsTab === 'textures' ? texturesView(() => m.close())
      : assetsTab === 'players' ? playersView(() => m.close()) : spritesView(() => m.close());
    body.replaceChildren(tabs, view);
  };
  draw();
  m = modal('Assets', [body]);
  m.el.style.width = 'min(900px,100%)';
}
function itemsView({onPick, close}) {
  if (!assetsReady()) return needServer('Item icons');
  const wrap = h('div', {style: {display: 'flex', flexDirection: 'column', gap: '8px'}});
  const search = h('input', {class: 'inp', placeholder: 'Search 1,400+ items… (diamond, oak, sword)', id: 'itemSearch'});
  const grid = h('div', {class: 'grid-icons'});
  let sel = null;
  const actions = h('div', {class: 'toolbar'});
  const fill = async () => {
    const q = search.value.trim().toLowerCase();
    const list = (await loadItems()).filter(i => !q || i.id.includes(q.replace(/ /g, '_')) || i.name.toLowerCase().includes(q)).slice(0, 240);
    grid.replaceChildren(...list.map(i => {
      const c = h('div', {class: 'icon-cell' + (sel === i.id ? ' on' : ''), title: i.name + '\nminecraft:' + i.id},
        h('img', {src: iconUrl(i.id, 64), alt: '', loading: 'lazy'}), h('span', {}, i.name));
      c.addEventListener('click', () => {
        if (onPick) { onPick('minecraft:' + i.id); close(); return; }
        sel = i.id; $$('.icon-cell.on', grid).forEach(x => x.classList.remove('on')); c.classList.add('on'); renderActions(i);
      });
      c.addEventListener('dblclick', () => { if (!onPick) { addItemEl(i.id); close(); } });
      return c;
    }));
    if (!list.length) grid.append(h('p', {}, 'No items match.'));
  };
  const renderActions = i => actions.replaceChildren(
    h('span', {style: {flex: 1}}, h('b', {}, i.name), ' ', h('span', {class: 'chip'}, 'minecraft:' + i.id)),
    h('button', {class: 'btn', onclick: () => { addItemEl(i.id); close(); }}, 'Add 3D item'),
    h('button', {class: 'btn', title: 'Works on HUDs too', onclick: async () => { await addSpriteFromDataUrl(await urlToDataUrl(iconUrl(i.id, 64)), i.id); toast('Sprite ' + i.id + ' added', 'ok'); }}, 'Add as sprite'));
  search.addEventListener('input', () => { clearTimeout(search._t); search._t = setTimeout(fill, 120); });
  fill();
  setTimeout(() => search.focus(), 30);
  wrap.append(search, grid, actions);
  return wrap;
}
function addItemEl(id) {
  const d = doc();
  addElement('item', {item: 'minecraft:' + id, name: id.replace(/_/g, ' '), w: 32, h: 32, x: Math.round(d.canvas.width / 2 - 16), y: Math.round(d.canvas.height / 2 - 16)});
}
function pickItem(cb) {
  let m;
  m = modal('Pick an item', [itemsView({onPick: cb, close: () => m.close()})]);
  m.el.style.width = 'min(820px,100%)';
}
function texturesView(close) {
  if (!assetsReady()) return needServer('Textures');
  const folder = h('select', {class: 'inp', style: {width: '160px'}}, ...['item', 'block', 'gui/sprites', 'mob_effect', 'particle', 'painting', 'entity', 'environment', 'misc'].map(f => h('option', {value: f}, f)));
  const search = h('input', {class: 'inp', placeholder: 'Filter…'});
  const grid = h('div', {class: 'grid-icons'});
  const fill = async () => {
    grid.replaceChildren(h('p', {}, 'Loading…'));
    let list = [];
    try { list = await (await fetch(`${srvBase()}/assets/textures?folder=${encodeURIComponent(folder.value)}&q=${encodeURIComponent(search.value.trim())}`)).json(); } catch { /* offline */ }
    grid.replaceChildren(...list.slice(0, 300).map(rel => {
      const c = h('div', {class: 'icon-cell', title: rel + '\nClick to add as a sprite'}, h('img', {src: textureUrl(rel), alt: '', loading: 'lazy', style: {objectFit: 'contain'}}), h('span', {}, rel.split('/').pop()));
      c.addEventListener('click', async () => { const id = await addSpriteFromDataUrl(await urlToDataUrl(textureUrl(rel)), rel.split('/').pop()); toast('Sprite ' + id + ' added', 'ok'); });
      return c;
    }));
    if (!list.length) grid.replaceChildren(h('p', {}, 'Nothing here.'));
  };
  folder.addEventListener('change', fill);
  search.addEventListener('input', () => { clearTimeout(search._t); search._t = setTimeout(fill, 200); });
  fill();
  return h('div', {style: {display: 'flex', flexDirection: 'column', gap: '8px'}},
    h('div', {class: 'toolbar'}, folder, h('div', {style: {flex: 1}}, search)),
    h('p', {style: {margin: 0}}, 'Every texture from Minecraft ' + SRV.version + '. Click one to add it as a sprite (animated textures use their first frame).'), grid);
}
function playersView(close) {
  if (!connected()) return needServer('Player skins');
  const name = h('input', {class: 'inp', placeholder: 'Player name', value: store.get('lastHead', 'Notch'), style: {maxWidth: '220px'}});
  const pv = h('div', {style: {display: 'flex', gap: '24px', alignItems: 'flex-end', padding: '12px', background: 'var(--stage)', borderRadius: '10px', minHeight: '150px'}});
  const show = () => {
    const n = name.value.trim() || 'Steve'; store.set('lastHead', n);
    pv.replaceChildren(
      h('figure', {style: {margin: 0, textAlign: 'center'}}, h('img', {src: headUrl(n, 96), width: 96, height: 96, style: {imageRendering: 'pixelated'}}), h('figcaption', {class: 'lab'}, '3D head')),
      h('figure', {style: {margin: 0, textAlign: 'center'}}, h('img', {src: bodyUrl(n, 128), width: 64, height: 128, style: {imageRendering: 'pixelated'}}), h('figcaption', {class: 'lab'}, 'Player model')));
  };
  name.addEventListener('change', show);
  show();
  const n = () => name.value.trim();
  return h('div', {style: {display: 'flex', flexDirection: 'column', gap: '10px'}},
    h('div', {class: 'toolbar'}, name, h('button', {class: 'btn', onclick: show}, 'Look up'), h('span', {class: 'lab'}, 'Leave the owner blank on an element to show whoever is viewing.')),
    pv,
    h('div', {class: 'toolbar'},
      h('button', {class: 'btn', onclick: () => { addElement('head', {headOwner: n(), name: (n() || 'Viewer') + ' head', w: 40, h: 40}); close(); }}, 'Add 3D head'),
      h('button', {class: 'btn', onclick: () => { addElement('model', {headOwner: n(), name: (n() || 'Viewer') + ' model', w: 40, h: 80}); close(); }}, 'Add player model'),
      h('button', {class: 'btn', title: 'For HUDs, which cannot show 3D heads', onclick: async () => { await addSpriteFromDataUrl(await urlToDataUrl(headUrl(n() || 'Steve', 32)), (n() || 'steve') + '_head'); toast('Head sprite added', 'ok'); }}, 'Add head as sprite'),
      h('button', {class: 'btn', onclick: async () => { await addSpriteFromDataUrl(await urlToDataUrl(bodyUrl(n() || 'Steve', 64)), (n() || 'steve') + '_body'); toast('Body sprite added', 'ok'); }}, 'Add body as sprite')));
}

/* ============================================================== tabs (pages inside a UI) */
/** {tabset: [pages]} from element groups, tab buttons and doc.tabs */
function tabsets(d = doc()) {
  const out = {};
  const add = (set, page) => { if (!set || !page) return; (out[set] = out[set] || []); if (!out[set].includes(page)) out[set].push(page); };
  for (const [set, page] of Object.entries(d.tabs || {})) add(set, page);
  for (const e of d.elements) for (const g of [e.group, e.tabFor]) { const i = (g || '').indexOf('/'); if (i > 0) add(g.slice(0, i), g.slice(i + 1).split('/')[0]); }
  return out;
}
function viewPage(set) { const d = doc(); return (S.pages && S.pages[d.id + ':' + set]) || (d.tabs || {})[set] || (tabsets(d)[set] || [])[0]; }
function setViewPage(set, page) { S.pages = S.pages || {}; S.pages[doc().id + ':' + set] = page; render(); }
/** Is the element on a page that is not being viewed? */
function offPage(e) {
  const g = e.group || '', i = g.indexOf('/');
  if (i <= 0) return false;
  const set = g.slice(0, i), page = g.slice(i + 1).split('/')[0];
  return viewPage(set) !== page;
}
function isActiveTab(e) {
  const t = e.tabFor || '', i = t.lastIndexOf('/');
  return i > 0 && viewPage(t.slice(0, i)) === t.slice(i + 1);
}
function renderPageStrip() {
  const sets = tabsets();
  let strip = $('#pageStrip');
  if (!Object.keys(sets).length || S.scriptOpen) { strip?.remove(); return; }
  if (!strip) { strip = h('div', {class: 'pages', id: 'pageStrip'}); stage.append(strip); }
  strip.replaceChildren(...Object.entries(sets).map(([set, pages]) => h('div', {class: 'set', title: 'Tab set "' + set + '": pick the page you are editing'}, h('b', {}, set),
    ...pages.map(p => h('button', {class: viewPage(set) === p ? 'on' : '', onclick: () => setViewPage(set, p)}, p)))));
}
function openNewTabset() {
  const name = h('input', {class: 'inp', value: 'main'});
  const pages = h('input', {class: 'inp', value: 'Home, Shop, Settings'});
  const style = h('select', {class: 'inp'}, h('option', {value: 'top'}, 'Buttons in a row along the top'), h('option', {value: 'left'}, 'Buttons in a column on the left'), h('option', {value: 'none'}, 'No buttons (I will make my own)'));
  modal('New tab set', [
    h('p', {}, 'A tab set is a group of pages that share one spot. Clicking a tab button shows its page and hides the others, in game too, with no scripts needed.'),
    field('Name', name), field('Pages', pages), field('Buttons', style),
    h('p', {style: {fontSize: '12px'}}, 'Afterwards, pick a page in the strip at the top of the canvas and draw on it, or select elements and use "Put on page" in the Layers tab.')
  ], [close => h('button', {class: 'btn primary', onclick: () => {
    const set = slug(name.value) || 'tabs';
    const list = pages.value.split(',').map(s => s.trim()).filter(Boolean);
    if (!list.length) return toast('Add at least one page', 'err');
    const d = doc();
    if (tabsets(d)[set]) return toast('A tab set called ' + set + ' already exists', 'err');
    mutate(dd => {
      dd.tabs = dd.tabs || {};
      dd.tabs[set] = slug(list[0]);
      if (style.value !== 'none') {
        const W = dd.canvas.width, n = list.length;
        list.forEach((p, i) => {
          const pg = slug(p);
          const top = style.value === 'top';
          const bw = top ? Math.min(84, Math.floor((W - 40) / n) - 4) : 70, bh = top ? 18 : 18;
          dd.elements.push(Object.assign(defaultsFor('button'), {id: rid(), name: p + ' tab', text: p, tabFor: set + '/' + pg,
            x: top ? Math.round(W / 2 - (n * (bw + 4) - 4) / 2 + i * (bw + 4)) : 12, y: top ? 12 : 40 + i * (bh + 4), w: bw, h: bh,
            fill: '#1e1e1e', textColor: '#d0d0d0', activeFill: '#ffffff', activeTextColor: '#000000', hoverFill: '#2c2c2c', hoverScale: 1, shadow: false}));
          dd.elements.push(Object.assign(defaultsFor('text'), {id: rid(), name: p + ' page title', text: '<bold>' + p + '</bold>', group: set + '/' + pg,
            x: top ? 20 : 96, y: top ? 44 : 40, w: 200, h: 12, align: 'left'}));
        });
      }
    });
    S.pages = S.pages || {}; S.pages[doc().id + ':' + set] = slug(list[0]);
    close(); render(); toast('Tab set ' + set + ' created', 'ok');
  }}, 'Create')]);
}
function putOnPage(set, page) {
  const list = selected();
  if (!list.length) return toast('Select elements first');
  mutate(() => list.forEach(e => e.group = page ? set + '/' + page : ''));
}

/* ============================================================== presets */
function presetFromSelection(name) {
  const d = doc(), list = selected();
  const ids = new Set(list.map(e => e.id));
  const sprites = new Set();
  for (const e of list) for (const k of ['sprite', 'hoverSprite', 'activeSprite', 'barSprite']) if (e[k]) sprites.add(e[k]);
  // scripts: event nodes about these elements, everything they lead to, and functions they call
  const byNode = Object.fromEntries(d.script.nodes.map(n => [n.id, n]));
  const keep = new Set();
  const walk = id => { if (keep.has(id) || !byNode[id]) return; keep.add(id); const n = byNode[id]; for (const l of Object.values(n.next || {})) for (const x of l) walk(x); if (n.type === 'call_function' && !n.props.ui) d.script.nodes.filter(f => f.type === 'on_function' && f.props.name === n.props.name).forEach(f => walk(f.id)); };
  d.script.nodes.filter(n => NODE_TYPES[n.type]?.cat === 'event' && n.props.element && ids.has(n.props.element)).forEach(n => walk(n.id));
  for (const s of [...keep].map(k => byNode[k])) if (s.props?.sprite) sprites.add(s.props.sprite);
  const sets = new Set(list.map(e => (e.group || e.tabFor || '').split('/')[0]).filter(Boolean));
  return {id: slug(name) || 'preset', name, created: new Date().toISOString().slice(0, 10), canvasType: d.type,
    elements: clone(list), sprites: d.sprites.filter(s => sprites.has(s.id)).map(clone),
    nodes: [...keep].map(k => clone(byNode[k])), tabs: Object.fromEntries(Object.entries(d.tabs || {}).filter(([k]) => sets.has(k))),
    animations: d.animations.filter(a => a.tracks.some(t => ids.has(t.element))).map(a => Object.assign(clone(a), {tracks: a.tracks.filter(t => ids.has(t.element))}))};
}
function insertPreset(p) {
  const d = doc();
  const idMap = {}, nodeMap = {}, spriteMap = {}, setMap = {};
  for (const s of p.sprites || []) {
    const same = d.sprites.find(x => x.data === s.data);
    if (same) { spriteMap[s.id] = same.id; continue; }
    let id = s.id, n = 2; while (d.sprites.some(x => x.id === id)) id = s.id + '_' + n++;
    spriteMap[s.id] = id;
  }
  const existingSets = tabsets(d);
  for (const set of Object.keys(p.tabs || {})) { let s = set, n = 2; while (existingSets[s]) s = set + n++; setMap[set] = s; }
  const remapGroup = g => { if (!g) return g; const i = g.indexOf('/'); const set = i > 0 ? g.slice(0, i) : g; return (setMap[set] || set) + (i > 0 ? g.slice(i) : ''); };
  for (const e of p.elements) idMap[e.id] = byId(e.id) ? rid() : e.id;
  for (const n of p.nodes || []) nodeMap[n.id] = rid('n');
  const bb = bbox(p.elements);
  const dx = Math.round(d.canvas.width / 2 - (bb.x + bb.w / 2)), dy = Math.round(d.canvas.height / 2 - (bb.y + bb.h / 2));
  mutate(dd => {
    for (const s of p.sprites || []) if (!dd.sprites.some(x => x.id === spriteMap[s.id])) dd.sprites.push(Object.assign(clone(s), {id: spriteMap[s.id]}));
    for (const e of p.elements) {
      const c = Object.assign(defaultsFor(e.type), clone(e), {id: idMap[e.id]});
      c.x += dx; c.y += dy;
      for (const k of ['sprite', 'hoverSprite', 'activeSprite', 'barSprite']) if (c[k] && spriteMap[c[k]]) c[k] = spriteMap[c[k]];
      c.group = remapGroup(c.group); c.tabFor = remapGroup(c.tabFor);
      dd.elements.push(c);
    }
    const oy = Math.max(0, ...dd.script.nodes.map(n => n.y + 200));
    for (const n of p.nodes || []) {
      const c = clone(n); c.id = nodeMap[n.id]; c.y += oy;
      if (c.props.element && idMap[c.props.element]) c.props.element = idMap[c.props.element];
      if (c.props.sprite && spriteMap[c.props.sprite]) c.props.sprite = spriteMap[c.props.sprite];
      if (c.props.tabset && setMap[c.props.tabset]) c.props.tabset = setMap[c.props.tabset];
      if (c.props.group) c.props.group = remapGroup(c.props.group);
      for (const k of Object.keys(c.next || {})) c.next[k] = c.next[k].map(x => nodeMap[x]).filter(Boolean);
      dd.script.nodes.push(c);
    }
    dd.tabs = dd.tabs || {};
    for (const [set, page] of Object.entries(p.tabs || {})) dd.tabs[setMap[set] || set] = page;
    for (const a of p.animations || []) {
      let name = a.name, n = 2; while (dd.animations.some(x => x.name === name)) name = a.name + n++;
      dd.animations.push(Object.assign(clone(a), {id: rid('an'), name, tracks: a.tracks.map(t => Object.assign(clone(t), {element: idMap[t.element]}))}));
    }
  });
  S.sel = p.elements.map(e => idMap[e.id]);
  render();
  toast('Inserted ' + p.name, 'ok');
}
function localPresets() { return store.get('presets', []); }
async function serverPresets() {
  if (!connected() || !SRV.token) return [];
  try { const r = await fetch(srvBase() + '/api/presets', {headers: {'X-SilentUI-Token': SRV.token}}); const j = await r.json(); return (j.presets || []).map(p => Object.assign(p, {server: true})); } catch { return []; }
}
async function savePreset(p) {
  const list = localPresets().filter(x => x.id !== p.id); list.push(p); store.set('presets', list);
  if (connected() && SRV.token) {
    try { await fetch(srvBase() + '/api/presets', {method: 'POST', headers: {'Content-Type': 'application/json', 'X-SilentUI-Token': SRV.token}, body: JSON.stringify(p)}); toast('Preset saved on the server too', 'ok'); }
    catch { toast('Saved in this browser (server unreachable)'); }
  } else toast('Preset saved in this browser', 'ok');
}
async function deletePreset(p) {
  store.set('presets', localPresets().filter(x => x.id !== p.id));
  if (p.server && SRV.token) { try { await fetch(srvBase() + '/api/presets/delete?id=' + encodeURIComponent(p.id), {method: 'POST', headers: {'X-SilentUI-Token': SRV.token}}); } catch { /* offline */ } }
}
function presetThumb(p) {
  const bb = bbox(p.elements);
  const k = Math.min(180 / Math.max(1, bb.w), 104 / Math.max(1, bb.h), 2);
  const mini = h('div', {class: 'mini', style: {width: bb.w + 'px', height: bb.h + 'px', transform: `scale(${k}) translate(${-bb.w / 2}px,${-bb.h / 2}px)`}});
  const spr = Object.fromEntries((p.sprites || []).map(s => [s.id, s.data]));
  for (const e of p.elements) {
    const v = Object.assign(defaultsFor(e.type), e, {x: e.x - bb.x, y: e.y - bb.y});
    const node = elNode(v, v, {preview: true, sprites: spr});
    if (offPagePreset(p, e)) node.style.display = 'none';
    mini.append(node);
  }
  return h('div', {class: 'pv'}, mini);
}
function offPagePreset(p, e) {
  const g = e.group || '', i = g.indexOf('/');
  if (i <= 0) return false;
  const set = g.slice(0, i), page = g.slice(i + 1);
  const def = (p.tabs || {})[set];
  return def ? def !== page : false;
}
async function openPresets() {
  let m;
  const grid = h('div', {class: 'preset-grid'});
  const tabsEl = h('div', {class: 'mtabs'});
  let tab = 'builtin';
  const card = (p, actions) => h('div', {class: 'preset'}, presetThumb(p),
    h('div', {class: 'meta'}, h('b', {}, p.name), h('span', {}, p.desc || `${p.elements.length} elements · ${(p.nodes || []).length} script nodes` + (p.server ? ' · on server' : ''))),
    h('div', {class: 'acts'}, ...actions));
  const draw = async () => {
    tabsEl.replaceChildren(...[['builtin', 'Built in'], ['mine', 'My presets']].map(([k, l]) => h('button', {class: tab === k ? 'on' : '', onclick: () => { tab = k; draw(); }}, l)));
    if (tab === 'builtin') {
      grid.replaceChildren(...BUILTIN_PRESETS().map(p => card(p, [h('button', {class: 'btn sm primary', onclick: () => { m.close(); insertPreset(p); }}, 'Insert')])));
    } else {
      grid.replaceChildren(h('p', {}, 'Loading…'));
      const mine = [...localPresets()];
      for (const sp of await serverPresets()) if (!mine.some(x => x.id === sp.id)) mine.push(sp);
      grid.replaceChildren(...mine.map(p => card(p, [
        h('button', {class: 'btn sm primary', onclick: () => { m.close(); insertPreset(p); }}, 'Insert'),
        h('button', {class: 'btn sm', onclick: () => copyText(JSON.stringify(p), 'Preset JSON copied')}, 'Copy'),
        h('button', {class: 'btn sm danger', onclick: async () => { await deletePreset(p); draw(); }}, 'Delete')])));
      if (!mine.length) grid.replaceChildren(h('p', {style: {gridColumn: '1/-1'}}, 'No saved presets yet. Select elements on the canvas and press "Save selection as preset".'));
    }
  };
  const nameIn = h('input', {class: 'inp', placeholder: 'Preset name', style: {maxWidth: '220px'}});
  const saveBar = h('div', {class: 'toolbar'},
    nameIn,
    h('button', {class: 'btn', disabled: !S.sel.length, title: S.sel.length ? '' : 'Select elements on the canvas first', onclick: async () => {
      if (!nameIn.value.trim()) return toast('Give the preset a name', 'err');
      await savePreset(presetFromSelection(nameIn.value.trim())); tab = 'mine'; draw();
    }}, `Save selection as preset (${S.sel.length})`),
    h('button', {class: 'btn ghost', onclick: () => {
      const ta = h('textarea', {class: 'code', rows: 10, placeholder: 'Paste preset JSON'});
      modal('Import preset', [ta], [c => h('button', {class: 'btn primary', onclick: async () => { try { const p = JSON.parse(ta.value); if (!p.elements) throw new Error('not a preset'); await savePreset(p); c(); tab = 'mine'; draw(); } catch (e) { toast('Invalid preset: ' + e.message, 'err'); } }}, 'Import')]);
    }}, 'Import JSON'));
  m = modal('Presets', [h('p', {}, 'Ready-made pieces with their scripts already wired. Your own presets are stored in this browser and, when connected, on the server so every admin can use them.'), saveBar, tabsEl, grid]);
  m.el.style.width = 'min(960px,100%)';
  draw();
}

/* ============================================================== commands & functions */
function functionNames(d = doc()) { return [...new Set(d.script.nodes.filter(n => n.type === 'on_function' && n.props.name).map(n => n.props.name))]; }
function openCommands() {
  let m;
  const body = h('div', {style: {display: 'flex', flexDirection: 'column', gap: '12px'}});
  const draw = () => {
    const d = doc();
    const list = d.commands || [];
    body.replaceChildren(
      h('p', {}, 'Commands are registered on the server when this file loads. Arguments reach scripts as ', h('code', {}, '{arg:name}'), ', with checking, tab completion and usage messages built in.'),
      ...list.map((c, i) => commandCard(c, i, draw)),
      h('div', {class: 'toolbar'},
        h('button', {class: 'btn primary', onclick: () => { mutate(dd => { dd.commands = dd.commands || []; dd.commands.push({name: 'cmd' + (list.length + 1), aliases: [], permission: '', description: '', args: [], function: '', open: d.type !== 'logic'}); }); draw(); }}, '+ New command'),
        h('span', {class: 'lab'}, d.type === 'logic' ? 'Logic file: commands run functions without drawing anything.' : 'Tip: a UI with type Logic holds commands and functions only.')),
      h('div', {class: 'sec', style: {border: '1px solid var(--line)', borderRadius: '10px'}},
        h('h4', {}, 'Functions'),
        ...functionNames(d).map(fn => h('div', {class: 'toolbar', style: {marginBottom: '6px'}}, h('span', {class: 'chip'}, 'ƒ ' + fn),
          h('span', {class: 'lab', style: {flex: 1}}, d.script.nodes.filter(n => n.type === 'call_function' && n.props.name === fn).length + ' callers'),
          h('button', {class: 'btn sm', onclick: () => { m.close(); S.scriptOpen = true; render(); focusNode(d.script.nodes.find(n => n.type === 'on_function' && n.props.name === fn)); }}, 'Open'))),
        newFunctionRow(() => { m.close(); }))
    );
  };
  draw();
  m = modal('Commands & functions', [body]);
  m.el.style.width = 'min(860px,100%)';
}
function newFunctionRow(after) {
  const name = h('input', {class: 'inp', placeholder: 'new function name, e.g. buy_item', style: {maxWidth: '260px'}});
  return h('div', {class: 'toolbar'}, name, h('button', {class: 'btn', onclick: () => {
    const fn = slug(name.value);
    if (!fn) return toast('Name the function', 'err');
    const n = {id: rid('n'), type: 'on_function', x: 40, y: Math.max(40, ...doc().script.nodes.map(k => k.y + 200)), props: {name: fn}, next: {}};
    mutate(d => d.script.nodes.push(n));
    after(); S.scriptOpen = true; render(); focusNode(n);
    toast('Function ' + fn + ' created. Connect actions to it, then call it with "Call function".', 'ok');
  }}, '+ New function'));
}
function focusNode(n) {
  if (!n) return;
  const area = $('#nodes')?.getBoundingClientRect(); if (!area) return;
  SC.zoom = 1; SC.panX = area.width / 2 - n.x - 118; SC.panY = 80 - n.y; SC.sel = n.id;
  renderNodes();
}
function commandCard(c, i, redraw) {
  const d = doc();
  const set = (fn) => mutate(() => { fn(); }, {});
  const inp = (val, onch, attrs = {}) => { const x = h('input', {class: 'inp', value: val ?? '', ...attrs}); x.addEventListener('change', () => { set(() => onch(x.value)); redraw(); }); return x; };
  const usage = '/' + c.name + c.args.map(a => ' ' + (a.optional ? '[' : '<') + a.name + (a.type === 'choice' && a.choices.length ? ':' + a.choices.join('|') : '') + (a.optional ? ']' : '>')).join('');
  const fnSel = h('select', {class: 'inp'}, h('option', {value: ''}, '— none (use “When command is used” nodes) —'), ...functionNames(d).map(f => h('option', {value: f, selected: f === c.function}, 'ƒ ' + f)));
  fnSel.addEventListener('change', () => { set(() => c.function = fnSel.value); redraw(); });
  const openCb = h('input', {type: 'checkbox', checked: c.open}); openCb.addEventListener('change', () => { set(() => c.open = openCb.checked); });
  const args = h('div');
  args.append(h('div', {class: 'cmd-row lab'}, h('span', {}, 'Argument'), h('span', {}, 'Type'), h('span', {}, 'Choices (a, b, c)'), h('span', {}, 'Optional'), h('span', {}, 'Default'), h('span')));
  c.args.forEach((a, ai) => {
    const type = h('select', {class: 'inp'}, ...['word', 'text', 'number', 'integer', 'player', 'choice', 'boolean'].map(t => h('option', {value: t, selected: t === a.type}, t)));
    type.addEventListener('change', () => { set(() => a.type = type.value); redraw(); });
    const opt = h('input', {type: 'checkbox', checked: a.optional}); opt.addEventListener('change', () => set(() => a.optional = opt.checked));
    args.append(h('div', {class: 'cmd-row'}, inp(a.name, v => a.name = slug(v) || a.name), type,
      inp((a.choices || []).join(', '), v => a.choices = v.split(',').map(s => s.trim()).filter(Boolean), {disabled: a.type !== 'choice', placeholder: a.type === 'choice' ? 'blocks, tools, food' : '—'}),
      h('label', {class: 'check', style: {margin: 0}}, opt), inp(a.def, v => a.def = v, {placeholder: 'if left out'}),
      h('button', {class: 'icon-btn', title: 'Remove', onclick: () => { set(() => c.args.splice(ai, 1)); redraw(); }}, '×')));
  });
  return h('div', {class: 'sec', style: {border: '1px solid var(--line)', borderRadius: '10px'}},
    h('h4', {}, h('code', {style: {textTransform: 'none', color: 'var(--text)', letterSpacing: 0, fontSize: '13px'}}, usage),
      h('button', {class: 'btn sm danger', onclick: () => { set(() => d.commands.splice(i, 1)); redraw(); }}, 'Delete')),
    h('div', {class: 'row2'}, field('Name', inp(c.name, v => c.name = slug(v).replace(/_/g, '') || c.name)), field('Aliases', inp(c.aliases.join(', '), v => c.aliases = v.split(',').map(s => slug(s)).filter(Boolean), {placeholder: 'store, buy'}))),
    h('div', {class: 'row2'}, field('Permission', inp(c.permission, v => c.permission = v.trim(), {placeholder: 'optional'})), field('Description', inp(c.description, v => c.description = v))),
    field('Runs', fnSel),
    d.type !== 'logic' ? h('label', {class: 'check'}, openCb, 'Open this UI first') : null,
    args,
    h('div', {class: 'toolbar'}, h('button', {class: 'btn sm', onclick: () => { set(() => c.args.push({name: 'arg' + (c.args.length + 1), type: 'word', choices: [], optional: false, def: ''})); redraw(); }}, '+ Argument'),
      h('span', {class: 'lab'}, 'In scripts: ' + (c.args.length ? c.args.map(a => '{arg:' + a.name + '}').join(' ') : 'no arguments'))));
}
/* ============================================================== built-in presets */
const MONO = {panel: '#141414', panel2: '#1e1e1e', line: '#3a3a3a', text: '#f2f2f2', muted: '#9a9a9a', white: '#ffffff', black: '#000000'};
function P(name, desc, build) { return {name, desc, ...build()}; }
function spr(id, o) { return {id, data: genShape(o), width: o.w, height: o.h}; }
function BUILTIN_PRESETS() {
  return [
    P('Tab bar with pages', '3 tabs that switch pages, no scripts needed', () => {
      const set = 'tabs';
      const els = [el('image', {id: 'tb_bg', name: 'Tab panel', x: 0, y: 0, w: 300, h: 180, sprite: 'tb_panel', blocked: true})];
      ['Home', 'Shop', 'Settings'].forEach((t, i) => {
        const pg = t.toLowerCase();
        els.push(el('button', {id: 'tb_' + pg, name: t + ' tab', text: t, tabFor: set + '/' + pg, x: 10 + i * 72, y: 10, w: 68, h: 18, fill: MONO.panel2, textColor: '#cfcfcf',
          activeFill: MONO.white, activeTextColor: MONO.black, hoverFill: '#2a2a2a', hoverScale: 1, shadow: false}));
        els.push(el('text', {id: 'tp_' + pg, name: t + ' page', group: set + '/' + pg, x: 12, y: 40, w: 276, h: 12, text: '<bold>' + t + '</bold> <gray>page content goes here</gray>', align: 'left', blocked: true}));
      });
      return {elements: els, sprites: [spr('tb_panel', {w: 300, h: 180, r: 8, fill: MONO.panel, alpha: .95, border: MONO.line, bw: 1})], nodes: [], tabs: {[set]: 'home'}};
    }),
    P('Close button', 'Top-right X that closes the UI', () => ({
      elements: [el('image', {id: 'close_btn', name: 'Close', x: 0, y: 0, w: 14, h: 14, sprite: 'x_btn', hoverSprite: 'x_btn_hover', hoverScale: 1.1})],
      sprites: [spr('x_btn', {w: 14, h: 14, r: 4, fill: MONO.panel2, icon: 'close', iconColor: '#bdbdbd'}), spr('x_btn_hover', {w: 14, h: 14, r: 4, fill: MONO.white, icon: 'close', iconColor: '#000000'})],
      nodes: [node('on_click', 40, 40, {__id: 'cb1', element: 'close_btn'}, {then: ['cb2']}), node('close_ui', 320, 40, {__id: 'cb2'})]
    })),
    P('Confirm dialog', 'Yes / No popup that calls a function', () => ({
      elements: [
        el('image', {id: 'cd_bg', name: 'Dialog', group: 'confirm', x: 0, y: 0, w: 200, h: 90, sprite: 'cd_panel', blocked: true}),
        el('text', {id: 'cd_msg', name: 'Question', group: 'confirm', x: 10, y: 18, w: 180, h: 24, text: 'Are you sure?', blocked: true}),
        el('button', {id: 'cd_yes', name: 'Yes', group: 'confirm', x: 20, y: 58, w: 74, h: 20, text: 'Yes', fill: MONO.white, textColor: MONO.black, shadow: false, hoverFill: '#d9d9d9', hoverScale: 1.04}),
        el('button', {id: 'cd_no', name: 'No', group: 'confirm', x: 106, y: 58, w: 74, h: 20, text: 'No', fill: MONO.panel2, textColor: MONO.text, shadow: false, hoverFill: '#2c2c2c', hoverScale: 1.04})],
      sprites: [spr('cd_panel', {w: 200, h: 90, r: 8, fill: MONO.panel, alpha: .97, border: MONO.line, bw: 1})],
      nodes: [
        node('on_click', 40, 40, {__id: 'cd1', element: 'cd_yes'}, {then: ['cd2', 'cd3']}),
        node('call_function', 320, 40, {__id: 'cd2', name: 'confirmed', args: ''}),
        node('hide_group', 320, 200, {__id: 'cd3', group: 'confirm'}),
        node('on_click', 40, 330, {__id: 'cd4', element: 'cd_no'}, {then: ['cd5']}),
        node('hide_group', 320, 330, {__id: 'cd5', group: 'confirm'}),
        node('on_function', 40, 470, {__id: 'cd6', name: 'confirmed'}, {then: ['cd7']}),
        node('message', 320, 470, {__id: 'cd7', text: '<white>Confirmed!</white>'})]
    })),
    P('Item shop grid', '6 real items with prices and a buy function', () => {
      const items = [['diamond', 100], ['emerald', 60], ['golden_apple', 40], ['ender_pearl', 25], ['iron_ingot', 10], ['bread', 2]];
      const els = [el('text', {id: 'sg_coins', name: 'Coins', x: 0, y: 0, w: 200, h: 10, text: '<gray>Coins:</gray> {var:coins}', align: 'left', blocked: true})];
      const nodes = [];
      items.forEach(([id, price], i) => {
        const x = (i % 3) * 66, y = 16 + Math.floor(i / 3) * 70;
        els.push(el('image', {id: 'sg_slot_' + id, name: id + ' slot', x, y, w: 60, h: 64, sprite: 'sg_slot', hoverSprite: 'sg_slot_hover', hoverScale: 1.04}));
        els.push(el('item', {id: 'sg_item_' + id, name: id, x: x + 16, y: y + 8, w: 28, h: 28, item: 'minecraft:' + id, blocked: true, fillEnabled: false}));
        els.push(el('text', {id: 'sg_price_' + id, name: id + ' price', x, y: y + 44, w: 60, h: 10, text: price + ' coins', textSize: .75, textColor: '#cfcfcf', blocked: true}));
        nodes.push(node('on_click', 40, 40 + i * 170, {__id: 'sgc' + i, element: 'sg_slot_' + id}, {then: ['sgf' + i]}));
        nodes.push(node('call_function', 320, 40 + i * 170, {__id: 'sgf' + i, name: 'buy', args: `item=minecraft:${id}, price=${price}`}));
      });
      nodes.push(node('on_function', 40, 1100, {__id: 'sgb', name: 'buy'}, {then: ['sgif']}));
      nodes.push(node('if_var', 320, 1100, {__id: 'sgif', name: 'coins', op: '>=', value: '{price}'}, {true: ['sgsub'], false: ['sgno']}));
      nodes.push(node('add_var', 600, 1060, {__id: 'sgsub', name: 'coins', amount: '-{price}'}, {then: ['sggive']}));
      nodes.push(node('give_item', 880, 1060, {__id: 'sggive', item: '{item}', amount: '1'}, {then: ['sgok']}));
      nodes.push(node('message', 1160, 1060, {__id: 'sgok', text: '<white>Bought for {price} coins.</white>'}));
      nodes.push(node('message', 600, 1240, {__id: 'sgno', text: "<gray>You can't afford that.</gray>"}));
      return {elements: els, nodes, sprites: [spr('sg_slot', {w: 60, h: 64, r: 6, fill: MONO.panel2, alpha: .95}), spr('sg_slot_hover', {w: 60, h: 64, r: 6, fill: '#2a2a2a', border: MONO.white, bw: 1})]};
    }),
    P('Player card', '3D head, name, level and XP bar', () => ({
      elements: [
        el('image', {id: 'pc_bg', name: 'Card', x: 0, y: 0, w: 180, h: 56, sprite: 'pc_panel', blocked: true}),
        el('head', {id: 'pc_head', name: 'Head', x: 10, y: 10, w: 36, h: 36, blocked: true, fillEnabled: false}),
        el('text', {id: 'pc_name', name: 'Name', x: 56, y: 12, w: 116, h: 10, text: '<bold>{player}</bold>', align: 'left', blocked: true}),
        el('text', {id: 'pc_lvl', name: 'Level', x: 56, y: 25, w: 116, h: 9, text: '<gray>Level</gray> {level}', align: 'left', textSize: .8, blocked: true}),
        el('progress', {id: 'pc_xp', name: 'XP', x: 56, y: 38, w: 112, h: 4, fill: '#2a2a2a', barColor: MONO.white, value: '%player_exp%', max: '1', blocked: true})],
      sprites: [spr('pc_panel', {w: 180, h: 56, r: 8, fill: MONO.panel, alpha: .95, border: MONO.line, bw: 1})], nodes: []
    })),
    P('Player model', 'Rotatable 3D player (scroll to turn)', () => ({
      elements: [
        el('image', {id: 'pm_bg', name: 'Stage', x: 0, y: 0, w: 90, h: 130, sprite: 'pm_panel', blocked: true}),
        el('model', {id: 'pm_model', name: 'Player model', x: 25, y: 12, w: 40, h: 90, rotatable: true, spin: 20, fillEnabled: false}),
        el('text', {id: 'pm_name', name: 'Name', x: 0, y: 110, w: 90, h: 10, text: '{player}', blocked: true})],
      sprites: [spr('pm_panel', {w: 90, h: 130, r: 8, fill: MONO.panel, fill2: '#050505', vgrad: true, alpha: .95, border: MONO.line, bw: 1})], nodes: []
    })),
    P('Search box', 'Input that reacts to typed text', () => ({
      elements: [
        el('input', {id: 'sb_in', name: 'Search', x: 0, y: 0, w: 160, h: 18, fill: MONO.panel2, placeholder: 'Search…', align: 'left'}),
        el('text', {id: 'sb_out', name: 'Result', x: 0, y: 24, w: 160, h: 10, text: '', textColor: MONO.muted, align: 'left', blocked: true})],
      sprites: [],
      nodes: [node('on_text', 40, 40, {__id: 'sb1', element: 'sb_in'}, {then: ['sb2']}), node('set_text', 320, 40, {__id: 'sb2', element: 'sb_out', text: 'Searching for "{text}"…'})]
    })),
    P('Toggle switch', 'On/off button that remembers its state', () => ({
      elements: [el('button', {id: 'tg', name: 'Toggle', x: 0, y: 0, w: 70, h: 18, text: 'OFF', fill: MONO.panel2, textColor: MONO.muted, shadow: false, hoverScale: 1.04})],
      sprites: [],
      nodes: [
        node('on_click', 40, 40, {__id: 'tg1', element: 'tg'}, {then: ['tg2']}),
        node('if_var', 320, 40, {__id: 'tg2', name: 'toggle', op: '==', value: 'on'}, {true: ['tg3'], false: ['tg6']}),
        node('set_var', 600, 0, {__id: 'tg3', name: 'toggle', value: 'off'}, {then: ['tg4']}),
        node('set_text', 880, 0, {__id: 'tg4', element: 'tg', text: 'OFF'}, {then: ['tg5']}),
        node('set_color', 1160, 0, {__id: 'tg5', element: 'tg', color: MONO.panel2}),
        node('set_var', 600, 200, {__id: 'tg6', name: 'toggle', value: 'on'}, {then: ['tg7']}),
        node('set_text', 880, 200, {__id: 'tg7', element: 'tg', text: 'ON'}, {then: ['tg8']}),
        node('set_color', 1160, 200, {__id: 'tg8', element: 'tg', color: MONO.white})]
    })),
    P('Notification toast', 'Slides in, waits, fades out; call notify', () => ({
      elements: [
        el('image', {id: 'nt_bg', name: 'Toast', x: 0, y: 0, w: 170, h: 26, sprite: 'nt_panel', blocked: true, opacity: 0}),
        el('text', {id: 'nt_text', name: 'Toast text', x: 10, y: 8, w: 150, h: 10, text: 'Hello!', align: 'left', blocked: true, opacity: 0})],
      sprites: [spr('nt_panel', {w: 170, h: 26, r: 6, fill: MONO.panel2, alpha: .96, border: MONO.line, bw: 1})],
      animations: [{id: 'nt', name: 'toast', length: 70, loop: false, autoplay: false, tracks: [
        {element: 'nt_bg', property: 'opacity', keys: [{t: 0, v: 0, ease: 'ease-out'}, {t: 6, v: 1, ease: 'linear'}, {t: 60, v: 1, ease: 'ease-in'}, {t: 70, v: 0, ease: 'linear'}]},
        {element: 'nt_text', property: 'opacity', keys: [{t: 0, v: 0, ease: 'ease-out'}, {t: 6, v: 1, ease: 'linear'}, {t: 60, v: 1, ease: 'ease-in'}, {t: 70, v: 0, ease: 'linear'}]},
        {element: 'nt_bg', property: 'x', keys: [{t: 0, v: 20, ease: 'ease-out'}, {t: 8, v: 0, ease: 'linear'}]}]}],
      nodes: [node('on_function', 40, 40, {__id: 'nt1', name: 'notify'}, {then: ['nt2']}), node('set_text', 320, 40, {__id: 'nt2', element: 'nt_text', text: '{msg}'}, {then: ['nt3']}),
        node('play_animation', 600, 40, {__id: 'nt3', animation: 'toast'})]
    })),
    P('HUD stat chip', 'Icon + value chip for HUDs', () => ({
      elements: [
        el('image', {id: 'hc_bg', name: 'Chip', x: 0, y: 0, w: 64, h: 15, sprite: 'hc_chip'}),
        el('image', {id: 'hc_ic', name: 'Icon', x: 5, y: 4, w: 8, h: 8, sprite: 'hc_icon'}),
        el('text', {id: 'hc_tx', name: 'Value', x: 15, y: 4, w: 46, h: 8, text: '{online} online', align: 'left'})],
      sprites: [spr('hc_chip', {w: 64, h: 15, r: 4, fill: '#000000', alpha: .6}), spr('hc_icon', {w: 8, h: 8, icon: 'user', iconColor: '#ffffff'})], nodes: []
    }))
  ];
}

/* ============================================================== example: tabbed shop with a typed command */
function tplShop() {
  const d = newDoc('screen', 'Shop');
  d.id = 'example_shop';
  d.settings.dim = 0.4;
  d.tabs = {shop: 'blocks'};
  d.sprites = [
    spr('panel', {w: 300, h: 210, r: 9, fill: '#121212', fill2: '#0b0b0b', vgrad: true, alpha: .96, border: '#3a3a3a', bw: 1}),
    spr('slot', {w: 54, h: 60, r: 6, fill: '#1e1e1e', alpha: .95}),
    spr('slot_hover', {w: 54, h: 60, r: 6, fill: '#2a2a2a', border: '#ffffff', bw: 1}),
    spr('x_btn', {w: 14, h: 14, r: 4, fill: '#1e1e1e', icon: 'close', iconColor: '#bdbdbd'}),
    spr('x_btn_hover', {w: 14, h: 14, r: 4, fill: '#ffffff', icon: 'close', iconColor: '#000000'})
  ];
  const X = 90, Y = 30;
  const E = [
    el('image', {id: 'panel', name: 'Panel', x: X, y: Y, w: 300, h: 210, sprite: 'panel', blocked: true}),
    el('text', {id: 'title', name: 'Title', x: X + 14, y: Y + 12, w: 120, h: 12, text: '<bold>Shop</bold>', align: 'left', blocked: true}),
    el('text', {id: 'coins', name: 'Coins', x: X + 140, y: Y + 13, w: 130, h: 10, text: '<gray>Coins</gray> {var:coins}', align: 'right', textSize: .85, blocked: true}),
    el('image', {id: 'close', name: 'Close', x: X + 278, y: Y + 10, w: 14, h: 14, sprite: 'x_btn', hoverSprite: 'x_btn_hover', hoverScale: 1.1})
  ];
  const cats = {blocks: [['grass_block', 4], ['oak_log', 6], ['stone_bricks', 5], ['glass', 3], ['bookshelf', 20], ['lantern', 12]],
    tools: [['diamond_sword', 120], ['diamond_pickaxe', 140], ['bow', 40], ['shield', 30], ['fishing_rod', 15], ['shears', 10]],
    food: [['golden_apple', 50], ['cooked_beef', 4], ['bread', 2], ['cake', 12], ['apple', 2], ['golden_carrot', 8]]};
  const nodes = [
    node('on_open', 40, 40, {__id: 'o1'}, {then: ['o2']}),
    node('if_var', 320, 40, {__id: 'o2', name: 'coins', op: '==', value: ''}, {true: ['o3']}),
    node('set_var', 600, 40, {__id: 'o3', name: 'coins', value: '250'}),
    node('on_click', 40, 280, {__id: 'c1', element: 'close'}, {then: ['c2']}),
    node('close_ui', 320, 280, {__id: 'c2'}),
    node('on_hover_enter', 40, 440, {__id: 'h1'}, {then: ['h2']}),
    node('sound', 320, 440, {__id: 'h2', sound: 'minecraft:block.note_block.hat', volume: '0.3', pitch: '1.8'})
  ];
  let ny = 700;
  Object.entries(cats).forEach(([cat, items], ci) => {
    E.push(el('button', {id: 'tab_' + cat, name: cat + ' tab', text: cat[0].toUpperCase() + cat.slice(1), tabFor: 'shop/' + cat,
      x: X + 14 + ci * 74, y: Y + 32, w: 70, h: 18, fill: '#1e1e1e', textColor: '#bdbdbd', activeFill: '#ffffff', activeTextColor: '#000000', hoverFill: '#2a2a2a', hoverScale: 1, shadow: false}));
    items.forEach(([id, price], i) => {
      const sx = X + 14 + (i % 5) * 0 + (i % 3) * 92, sy = Y + 60 + Math.floor(i / 3) * 70;
      const g = 'shop/' + cat;
      E.push(el('image', {id: `s_${cat}_${i}`, name: id + ' slot', group: g, x: sx, y: sy, w: 86, h: 64, sprite: 'slot', hoverSprite: 'slot_hover', hoverScale: 1.03}));
      E.push(el('item', {id: `i_${cat}_${i}`, name: id, group: g, x: sx + 8, y: sy + 16, w: 30, h: 30, item: 'minecraft:' + id, blocked: true, fillEnabled: false, rotatable: false}));
      E.push(el('text', {id: `n_${cat}_${i}`, name: id + ' name', group: g, x: sx + 42, y: sy + 18, w: 42, h: 20, text: id.replace(/_/g, ' '), textSize: .6, align: 'left', blocked: true}));
      E.push(el('text', {id: `p_${cat}_${i}`, name: id + ' price', group: g, x: sx + 42, y: sy + 42, w: 42, h: 9, text: '<white>' + price + '</white> <gray>c</gray>', textSize: .7, align: 'left', blocked: true}));
      nodes.push(node('on_click', 40, ny, {__id: `k_${cat}_${i}`, element: `s_${cat}_${i}`}, {then: [`f_${cat}_${i}`]}));
      nodes.push(node('call_function', 320, ny, {__id: `f_${cat}_${i}`, name: 'buy', args: `item=minecraft:${id}, price=${price}, label=${id.replace(/_/g, ' ')}`}));
      ny += 170;
    });
  });
  nodes.push(node('on_function', 40, ny, {__id: 'b1', name: 'buy'}, {then: ['b2']}));
  nodes.push(node('if_var', 320, ny, {__id: 'b2', name: 'coins', op: '>=', value: '{price}'}, {true: ['b3'], false: ['b6']}));
  nodes.push(node('add_var', 600, ny - 40, {__id: 'b3', name: 'coins', amount: '-{price}'}, {then: ['b4']}));
  nodes.push(node('give_item', 880, ny - 40, {__id: 'b4', item: '{item}', amount: '1'}, {then: ['b5']}));
  nodes.push(node('message', 1160, ny - 40, {__id: 'b5', text: '<white>Bought {label}</white> <gray>for {price} coins</gray>'}));
  nodes.push(node('sound', 600, ny + 160, {__id: 'b6', sound: 'minecraft:entity.villager.no', volume: '0.6', pitch: '1'}, {then: ['b7']}));
  nodes.push(node('message', 880, ny + 160, {__id: 'b7', text: "<gray>You can't afford {label}.</gray>"}));
  // /shop [category] opens the shop on a tab
  nodes.push(node('on_function', 40, ny + 360, {__id: 'q1', name: 'open_category'}, {then: ['q2']}));
  nodes.push(node('switch_tab', 320, ny + 360, {__id: 'q2', tabset: 'shop', page: '{arg:category}'}));
  d.elements = E;
  d.script.nodes = nodes;
  d.commands = [{name: 'shop', aliases: ['store'], permission: '', description: 'Open the shop', function: 'open_category', open: true,
    args: [{name: 'category', type: 'choice', choices: ['blocks', 'tools', 'food'], optional: true, def: 'blocks'}]}];
  return d;
}
/* ============================================================== placeholders */
const PH_STATIC = [
  ['Player', [['{player}', 'Player name'], ['{uuid}', 'Player UUID'], ['{world}', 'World the player is in'], ['{level}', 'XP level'],
    ['{health}', 'Health, rounded up'], ['{food}', 'Food level'], ['{ping}', 'Ping in ms'], ['{x}', 'Block X'], ['{y}', 'Block Y'], ['{z}', 'Block Z']]],
  ['Server', [['{online}', 'Players online'], ['{max_players}', 'Player limit'], ['{time}', 'Server time (HH:MM:SS)'], ['{date}', 'Date (YYYY-MM-DD)']]],
  ['Events', [['{text}', 'What the player typed (When text is typed)'], ['{key}', 'Key that was pressed (When key pressed)'],
    ['{button}', 'left or right (When clicked)'], ['{element}', 'ID of the clicked element (When clicked)'], ['{args}', 'Everything typed after a command'],
    ['{arg1}', 'First word after a command (up to {arg9})']]],
  ['Item field', [['{hand}', "Item in the viewer's main hand"], ['{offhand}', 'Item in the off hand'], ['{helmet}', 'Helmet slot'],
    ['{chestplate}', 'Chestplate slot'], ['{leggings}', 'Leggings slot'], ['{boots}', 'Boots slot']]],
  ['PlaceholderAPI', [['%player_name%', 'Player name · expansion: Player'], ['%player_exp%', 'XP progress 0-1 · Player'], ['%player_health%', 'Health · Player'],
    ['%player_gamemode%', 'Game mode · Player'], ['%vault_eco_balance%', 'Money · Vault + an economy plugin'], ['%vault_eco_balance_formatted%', 'Money, formatted · Vault'],
    ['%server_online%', 'Players online · Server'], ['%server_tps_1%', 'TPS over 1 minute · Server'], ['%statistic_player_kills%', 'Player kills · Statistic'],
    ['%statistic_deaths%', 'Deaths · Statistic'], ['%luckperms_prefix%', 'Rank prefix · LuckPerms'], ['%luckperms_primary_group_name%', 'Rank · LuckPerms']]],
  ['Formatting', [['<white>', 'White text (close with </white>)'], ['<gray>', 'Grey text'], ['<dark_gray>', 'Dark grey text'], ['<#ffffff>', 'Any hex colour'],
    ['<bold>', 'Bold'], ['<italic>', 'Italic'], ['<underlined>', 'Underlined'], ['<newline>', 'Line break'], ['<reset>', 'Clear formatting']]]
];
/** Placeholders this UI itself creates: variables, command arguments, function arguments, input boxes. */
function phDynamic(d = doc()) {
  const vars = new Set(), fargs = new Set();
  for (const n of d.script.nodes) {
    if (['set_var', 'add_var', 'if_var'].includes(n.type) && n.props.name) vars.add(n.props.name);
    if (n.type === 'call_function') for (const part of String(n.props.args || '').split(',')) { const i = part.indexOf('='); if (i > 0) fargs.add(part.slice(0, i).trim()); }
  }
  const out = [];
  if (vars.size) out.push(['Variables in this UI', [...vars].map(v => ['{var:' + v + '}', 'Script variable "' + v + '"'])]);
  const inputs = d.elements.filter(e => e.type === 'input');
  if (inputs.length) out.push(['Input boxes', inputs.map(e => ['{var:input_' + e.id + '}', 'Text typed into "' + e.name + '"'])]);
  const args = (d.commands || []).flatMap(c => c.args.map(a => ['{arg:' + a.name + '}', `/${c.name} argument (${a.type}${a.optional ? ', optional' : ''})`]));
  if (args.length) out.push(['Command arguments', args]);
  if (fargs.size) out.push(['Function arguments', [...fargs].map(a => ['{' + a + '}', 'Passed to a function with Call function'])]);
  return out;
}
document.addEventListener('focusin', ev => {
  const t = ev.target;
  if (t.matches && t.matches('textarea, input.inp:not([type]), input.inp[type=text], .node input, .modal input.inp') && !t.matches('#itemSearch,#nodeSearch,#phSearch,#libSearch')) S.lastField = t;
});
function insertPh(code) {
  const f = S.lastField;
  if (f && document.contains(f) && !f.disabled) {
    const a = f.selectionStart ?? f.value.length, b = f.selectionEnd ?? a;
    f.value = f.value.slice(0, a) + code + f.value.slice(b);
    f.focus();
    try { f.setSelectionRange(a + code.length, a + code.length); } catch { /* some inputs have no caret */ }
    f.dispatchEvent(new Event('input', {bubbles: true}));
    f.dispatchEvent(new Event('change', {bubbles: true}));
    toast('Inserted ' + code, 'ok');
  } else copyText(code, code + ' copied (click into a text box first to insert directly)');
}
function openPlaceholders() {
  const search = h('input', {class: 'inp', id: 'phSearch', placeholder: 'Search placeholders…'});
  const list = h('div', {class: 'ph-list'});
  const target = S.lastField && document.contains(S.lastField) ? (S.lastField.closest('.row,.nf')?.querySelector('label,span')?.textContent || 'the last text box') : null;
  const fill = () => {
    const q = search.value.toLowerCase();
    const groups = [...phDynamic(), ...PH_STATIC];
    list.replaceChildren(...groups.map(([cat, items]) => {
      const rows = items.filter(([c, d]) => !q || c.toLowerCase().includes(q) || d.toLowerCase().includes(q));
      if (!rows.length) return null;
      return h('div', {class: 'ph-group'}, h('h5', {}, cat), ...rows.map(([code, desc]) => {
        const r = code.startsWith('%') || code.startsWith('<') ? '' : resolve(code, PV_VARS);
        const sample = r && r !== code ? r : '';
        return h('div', {class: 'ph-row', title: 'Click to insert'},
          h('code', {}, code), h('span', {class: 'd'}, desc), sample ? h('span', {class: 'ex'}, 'e.g. ' + sample) : h('span', {class: 'ex'}),
          h('button', {class: 'btn sm', onclick: ev => { ev.stopPropagation(); copyText(code, code + ' copied'); }}, 'Copy'),
          h('button', {class: 'btn sm primary', onclick: ev => { ev.stopPropagation(); insertPh(code); }}, 'Insert'));
      }));
    }).filter(Boolean));
  };
  search.addEventListener('input', fill);
  fill();
  const m = modal('Placeholders', [
    h('p', {}, 'Placeholders are replaced with live values when players see the UI. They work in text, buttons, progress bars, head owners and script fields. ',
      target ? h('b', {style: {color: 'var(--text)'}}, 'Insert goes into: ' + target + '.') : 'Click into a text box first and Insert will type it there; otherwise it is copied.'),
    search, list]);
  m.el.style.width = 'min(820px,100%)';
  setTimeout(() => search.focus(), 30);
}

/* ============================================================== tutorial */
const TUT = [
  {t: 'Welcome to Silent UI', b: 'You design menus, HUDs and in-world boards here, push them to your server, and players use them with their crosshair. This tour takes about two minutes. You can reopen it any time from the ? button.'},
  {t: 'Pick what you are making', b: 'Click empty canvas to see the UI settings. Screen opens in front of the player, HUD stays on screen, World is a board placed in the world, Logic holds commands only.', hl: '#panel', go: () => { S.sel = []; S.side = 'props'; render(); }},
  {t: 'Draw elements', b: 'Pick a tool on the left and drag on the canvas: R rectangle, T text, B button, I sprite, E item, H head, M player model. Drag to move, use the handles to resize, the top dot rotates.', hl: '#rail', go: () => setTool('button')},
  {t: 'Drag in items and sprites', b: 'The Library has every Minecraft item sorted into Armour, Weapons, Tools, Food and more, plus your sprites. Drag any of them onto the canvas. You can also drop PNG files from your computer.', hl: '[data-rail=library]', go: () => toggleLibrary(true)},
  {t: 'Style it', b: 'Select an element. The inspector puts layout, text, fill, hover effects and click actions in one place. Open Insert → Manage assets to generate rounded panels and icons.', hl: '#side', go: () => { if (!S.sel.length && els().length) S.sel = [els()[els().length - 1].id]; S.side = 'props'; render(); }},
  {t: 'Use placeholders', b: 'Text can show live values like {player}, {var:coins} or %vault_eco_balance%. The Placeholders panel lists them all and inserts them where you were typing.', hl: '[data-rail=placeholders]', go: () => openPlaceholders()},
  {t: 'Group things', b: 'Select several elements and press Ctrl+G. Clicking any member then selects the whole group; double-click picks one inside. Groups appear as folders in Layers, and scripts can show or hide them.', hl: '#navigator', go: () => { S.nav = 'layers'; renderNavigator(); }},
  {t: 'Tabs', b: 'Layers → + Tab set makes pages that share one spot, with tab buttons that switch them on their own. Choose the page you are editing in the strip above the canvas.', hl: '#navigator'},
  {t: 'Make it do things', b: 'Scripts connect events to actions: When clicked → Give item, Run command, Switch tab. Functions are reusable chains you call with values like price=100.', hl: '[data-rail=scripts]'},
  {t: 'Add commands', b: 'Commands & functions adds /commands with typed arguments (player, number, choice…), tab completion and usage messages.', hl: '[data-rail=commands]'},
  {t: 'Animate', b: 'The timeline records keyframes: turn on Auto-key, move the playhead, then move or resize the element. Easing smooths it in game.', hl: '[data-rail=timeline]'},
  {t: 'Test, then ship', b: 'Preview runs the UI right here, scripts included. Push to server saves it on your server and reloads it instantly; Export gives you the JSON for plugins/SilentUI/uis.', hl: '#previewBtn'},
  {t: 'Edit in game', b: 'In Minecraft, /sui edit <id> opens the UI with a toolbar: click to select, click again to move, right click for properties, scroll to resize, Save when done. That is the tour!'}
];
let tutStep = -1;
function startTutorial(step = 0) { tutStep = step; renderTutorial(); }
function endTutorial() { tutStep = -1; store.set('tutorialDone', true); $$('.tut-hl').forEach(x => x.classList.remove('tut-hl')); $('#tutorial')?.remove(); }
function renderTutorial() {
  $$('.tut-hl').forEach(x => x.classList.remove('tut-hl'));
  if (tutStep < 0) return;
  const s = TUT[tutStep];
  let card = $('#tutorial');
  if (!card) { card = h('div', {id: 'tutorial', role: 'dialog', 'aria-label': 'Tutorial'}); document.body.append(card); }
  card.replaceChildren(
    h('div', {class: 'tut-top'}, h('span', {}, `Step ${tutStep + 1} of ${TUT.length}`), h('button', {class: 'icon-btn', title: 'Close tutorial', onclick: endTutorial}, '×')),
    h('div', {class: 'tut-bar'}, h('i', {style: {width: ((tutStep + 1) / TUT.length * 100) + '%'}})),
    h('h3', {}, s.t), h('p', {}, s.b),
    h('div', {class: 'tut-acts'},
      tutStep > 0 ? h('button', {class: 'btn sm ghost', onclick: () => { tutStep--; renderTutorial(); }}, 'Back') : h('span'),
      s.go ? h('button', {class: 'btn sm', onclick: () => { s.go(); setTimeout(renderTutorial, 50); }}, 'Show me') : null,
      h('button', {class: 'btn sm primary', onclick: () => { if (tutStep >= TUT.length - 1) endTutorial(); else { tutStep++; renderTutorial(); } }}, tutStep >= TUT.length - 1 ? 'Finish' : 'Next')));
  if (s.hl) { const t = $(s.hl); if (t) t.classList.add('tut-hl'); }
}

/* ============================================================== library drawer (drag onto the canvas) */
const ITEM_CATS = [
  ['All', null],
  ['Armour', /(helmet|chestplate|leggings|boots|elytra|horse_armor|wolf_armor|^shield$|turtle_helmet|carved_pumpkin)/],
  ['Weapons', /(sword|^bow$|crossbow|trident|^mace$|arrow|_axe$)/],
  ['Tools', /(pickaxe|_shovel|_hoe|_axe$|shears|fishing_rod|flint_and_steel|^brush$|spyglass|compass|^clock$|bucket|^lead$|name_tag|carrot_on_a_stick)/],
  ['Food', /(apple|bread|beef|porkchop|chicken|mutton|rabbit|cod$|salmon|potato|carrot$|melon_slice|cookie|cake|pie$|stew|soup|berries|honey_bottle|dried_kelp|beetroot$|chorus_fruit|golden_carrot|tropical_fish|pufferfish)/],
  ['Gems & ores', /(^diamond$|^emerald$|ingot$|nugget$|^raw_|^coal$|lapis_lazuli|^quartz$|amethyst_shard|netherite_scrap|^redstone$|_ore$|^echo_shard|heart_of_the_sea|nether_star)/],
  ['Blocks', /(_planks$|_log$|stone$|_bricks$|glass$|_wool$|_concrete$|terracotta|^dirt$|^sand$|_block$|bookshelf|^obsidian$|_leaves$|lantern$)/],
  ['Potions', /(potion|tipped_arrow|^experience_bottle$)/],
  ['Spawn eggs', /spawn_egg$/],
  ['Discs', /music_disc/]
];
let libTab = 'items', libCat = 'Armour';
function toggleLibrary(force) {
  S.library = force ?? !S.library;
  renderLibrary();
  renderRail();
}
function renderLibrary() {
  let lib = $('#library');
  if (!S.library) { lib?.remove(); return; }
  if (!lib) { lib = h('div', {id: 'library'}); $('#main').append(lib); }
  const tabs = h('div', {class: 'mtabs'}, ...[['items', 'Items'], ['sprites', 'Sprites']].map(([k, l]) =>
    h('button', {class: libTab === k ? 'on' : '', onclick: () => { libTab = k; renderLibrary(); }}, l)),
    h('button', {style: {marginLeft: 'auto'}, title: 'Close', onclick: () => toggleLibrary(false)}, '×'));
  const body = h('div', {class: 'lib-body'});
  lib.replaceChildren(tabs, body);
  if (libTab === 'sprites') return libSprites(body);
  if (!assetsReady()) { body.append(needServer('Item icons')); return; }
  const search = h('input', {class: 'inp', id: 'libSearch', placeholder: 'Search items…', value: S.libQuery || ''});
  const chips = h('div', {class: 'lib-cats'}, ...ITEM_CATS.map(([name]) => h('button', {class: libCat === name ? 'on' : '', onclick: () => { libCat = name; renderLibrary(); }}, name)));
  const grid = h('div', {class: 'lib-grid'});
  const fill = async () => {
    const q = (S.libQuery = search.value).trim().toLowerCase().replace(/ /g, '_');
    const re = ITEM_CATS.find(c => c[0] === libCat)?.[1];
    const list = (await loadItems()).filter(i => (!re || re.test(i.id)) && (!q || i.id.includes(q) || i.name.toLowerCase().includes(q.replace(/_/g, ' ')))).slice(0, 300);
    grid.replaceChildren(...list.map(i => {
      const c = h('div', {class: 'lib-cell', draggable: 'true', title: i.name + ' · drag onto the canvas'}, h('img', {src: iconUrl(i.id, 64), alt: '', loading: 'lazy', draggable: 'false'}));
      c.addEventListener('dragstart', ev => { ev.dataTransfer.setData('text/x-sui', JSON.stringify({kind: 'item', id: i.id})); ev.dataTransfer.effectAllowed = 'copy'; });
      c.addEventListener('dblclick', () => dropAsset({kind: 'item', id: i.id}, null));
      return c;
    }));
    if (!list.length) grid.append(h('p', {class: 'lab'}, 'Nothing in this category matches.'));
  };
  search.addEventListener('input', () => { clearTimeout(search._t); search._t = setTimeout(fill, 120); });
  body.append(search, chips, grid, h('p', {class: 'lab', style: {margin: '6px 2px 0'}},
    doc().type === 'hud' ? 'HUDs cannot show 3D items, so dropped items become sprites.' : 'Drag onto the canvas for a 3D item. Hold Alt while dropping to add it as a flat sprite.'));
  fill();
}
function libSprites(body) {
  const dz = h('div', {class: 'drop', style: {padding: '14px'}}, 'Drop PNGs here (armour, icons, panels) or ', h('button', {class: 'btn sm', onclick: () => $('#fileSprite').click()}, 'Choose files'));
  dz.addEventListener('dragover', ev => { if ([...ev.dataTransfer.types].includes('Files')) { ev.preventDefault(); dz.classList.add('over'); } });
  dz.addEventListener('dragleave', () => dz.classList.remove('over'));
  dz.addEventListener('drop', async ev => { ev.preventDefault(); dz.classList.remove('over'); await importImages(ev.dataTransfer.files); renderLibrary(); });
  const grid = h('div', {class: 'lib-grid'}, ...doc().sprites.map(s => {
    const c = h('div', {class: 'lib-cell', draggable: 'true', title: s.id + ' · ' + s.width + '×' + s.height}, h('img', {src: s.data, alt: '', draggable: 'false'}));
    c.addEventListener('dragstart', ev => { ev.dataTransfer.setData('text/x-sui', JSON.stringify({kind: 'sprite', id: s.id})); ev.dataTransfer.effectAllowed = 'copy'; });
    c.addEventListener('dblclick', () => dropAsset({kind: 'sprite', id: s.id}, null));
    return c;
  }));
  body.append(dz, doc().sprites.length ? grid : h('p', {class: 'lab'}, 'No sprites yet. Drop some above, or get them from Assets.'),
    h('button', {class: 'btn sm', style: {marginTop: '8px'}, onclick: () => openAssets('sprites')}, 'Generate shapes…'));
}
/** Adds an asset at a canvas point (or the centre). */
async function dropAsset(a, p, flat) {
  const d = doc();
  const at = p || {x: d.canvas.width / 2, y: d.canvas.height / 2};
  if (a.kind === 'item') {
    if (d.type === 'hud' || flat) {
      const id = await addSpriteFromDataUrl(await urlToDataUrl(iconUrl(a.id, 32)), a.id);
      addElement('image', {sprite: id, name: a.id.replace(/_/g, ' '), w: 16, h: 16, x: Math.round(at.x - 8), y: Math.round(at.y - 8)});
    } else {
      addElement('item', {item: 'minecraft:' + a.id, name: a.id.replace(/_/g, ' '), w: 32, h: 32, x: Math.round(at.x - 16), y: Math.round(at.y - 16)});
    }
  } else if (a.kind === 'sprite') {
    const s = d.sprites.find(x => x.id === a.id); if (!s) return;
    addElement('image', {sprite: s.id, name: s.id, w: s.width, h: s.height, x: Math.round(at.x - s.width / 2), y: Math.round(at.y - s.height / 2)});
  }
}
stage.addEventListener('dragover', ev => {
  const types = [...ev.dataTransfer.types];
  if (types.includes('text/x-sui') || types.includes('Files')) { ev.preventDefault(); ev.dataTransfer.dropEffect = 'copy'; stage.classList.add('dropping'); }
});
stage.addEventListener('dragleave', () => stage.classList.remove('dropping'));
stage.addEventListener('drop', async ev => {
  stage.classList.remove('dropping');
  const p = toCanvas(ev);
  const raw = ev.dataTransfer.getData('text/x-sui');
  if (raw) { ev.preventDefault(); try { await dropAsset(JSON.parse(raw), p, ev.altKey); } catch { toast('Could not add that', 'err'); } return; }
  const files = [...ev.dataTransfer.files].filter(f => f.type.startsWith('image/'));
  if (!files.length) return;
  ev.preventDefault();
  let off = 0;
  for (const f of files) {
    const url = await new Promise(r => { const fr = new FileReader(); fr.onload = () => r(fr.result); fr.readAsDataURL(f); });
    try {
      const id = await addSpriteFromDataUrl(url, f.name.replace(/\.[^.]+$/, ''));
      const s = doc().sprites.find(x => x.id === id);
      addElement('image', {sprite: id, name: id, w: s.width, h: s.height, x: Math.round(p.x - s.width / 2 + off), y: Math.round(p.y - s.height / 2 + off)});
      off += 8;
    } catch { toast('Could not read ' + f.name, 'err'); }
  }
  if (S.library) renderLibrary();
});

/* ============================================================== grouping */
/** The selectable group of an element: plain groups ("header") or groups inside a tab page ("shop/tools/header"). Tab pages themselves are not selection groups. */
function selGroup(e) {
  const g = e.group || '';
  if (!g) return null;
  const n = g.split('/').length;
  return n === 1 || n >= 3 ? g : null;
}
const groupMembers = g => els().filter(e => e.group === g);
function groupName(g) { return g.split('/').pop(); }
function groupSel() {
  const list = selected();
  if (list.length < 2) return toast('Select two or more elements to group');
  const pages = new Set(list.map(e => { const g = e.group || '', parts = g.split('/'); return parts.length >= 2 ? parts.slice(0, 2).join('/') : ''; }));
  if (pages.size > 1) return toast('Those elements are on different tab pages', 'err');
  const page = [...pages][0];
  const taken = new Set(els().map(e => e.group));
  let n = 1, name;
  do { name = (page ? page + '/' : '') + 'group' + n++; } while (taken.has(name));
  mutate(() => list.forEach(e => e.group = name));
  toast('Grouped as ' + groupName(name) + '. Click any member to select the group, double-click for one element.', 'ok');
}
function ungroupSel() {
  const list = selected().filter(e => selGroup(e));
  if (!list.length) return toast('Nothing grouped is selected');
  mutate(() => list.forEach(e => { const parts = e.group.split('/'); parts.pop(); e.group = parts.join('/'); }));
  toast('Ungrouped', 'ok');
}
function renameGroup(oldG, raw) {
  const name = slug(raw);
  if (!name) return;
  const parts = oldG.split('/'); parts[parts.length - 1] = name;
  const g = parts.join('/');
  if (g === oldG) return;
  if (els().some(e => e.group === g)) return toast('A group with that name already exists', 'err');
  mutate(d => {
    d.elements.forEach(e => { if (e.group === oldG) e.group = g; });
    d.script.nodes.forEach(n => { if (n.props.group === oldG) n.props.group = g; });
  });
}
/* ============================================================== workspace */
function renderSaveState() {
  const el = $('#saveState');
  if (!el) return;
  el.dataset.state = S.saveState;
  el.textContent = S.saveState === 'pending' ? 'Saving…' : S.saveState === 'error' ? 'Storage full · export to save' : 'Saved locally';
  $('#undoBtn').disabled = !S.undo.length;
  $('#redoBtn').disabled = !S.redo.length;
}

function inspectElement(e) {
  const props = propsPanel(e), appearance = designPanel(e);
  const sections = [...props.children];
  const text = [...appearance.children].find(s => s.querySelector('h4')?.textContent === 'Text');
  if (text) appearance.prepend(text);
  const extra = h('details', {class: 'adv'}, h('summary', {}, 'Advanced properties'), ...sections.slice(1));
  return h('div', {}, h('div', {class: 'inspector-heading'},
    h('span', {class: 'inspector-icon', html: svg(e.type)}),
    h('div', {}, textInput(() => e.name, x => e.name = x, {id: 'elementName'}), h('small', {}, e.type + ' · ' + e.id))),
    sections[0], appearance, ['button', 'input'].includes(e.type) ? quickActionPanel(e) : null, extra);
}

function renderNavigator() {
  const body = $('#navigatorBody');
  $('#layersTab').setAttribute('aria-selected', S.nav === 'layers');
  $('#insertTab').setAttribute('aria-selected', S.nav === 'insert');
  if (S.nav === 'insert') {
    if (body.dataset.view === 'insert' && body.dataset.type === doc().type) return;
    body.dataset.view = 'insert'; body.dataset.type = doc().type;
    const choices = [['rect', 'Rectangle', 'Panels & backgrounds'], ['text', 'Text', 'Labels & placeholders'], ['button', 'Button', 'Clickable actions'], ['image', 'Image', 'Sprites & textures'], ['input', 'Text input', 'Ask a player for text'], ['progress', 'Progress bar', 'Values & statistics'], ['item', '3D item', 'Minecraft items'], ['head', 'Player head', 'A player’s skin'], ['model', 'Player model', 'Full 3D character']];
    body.replaceChildren(h('div', {class: 'nav-section-label'}, 'ADD AN ELEMENT'), h('div', {class: 'insert-grid'},
      ...choices.map(([type, label, desc]) => h('button', {class: 'insert-card', disabled: doc().type === 'hud' && ['item', 'head', 'model'].includes(type), onclick: () => { addElement(type); S.side = 'props'; S.nav = 'layers'; render(); }, 'aria-label': 'Add ' + label},
        h('span', {html: svg(type)}), h('strong', {}, label), h('small', {}, desc)))),
      h('div', {class: 'nav-section-label'}, 'BUILD FASTER'),
      ...[['presets', 'Component presets', 'Ready-made, wired components', openPresets], ['library', 'Minecraft library', 'Drag in items and sprites', () => toggleLibrary()], ['assets', 'Manage assets', 'Import or create sprites', () => openAssets()], ['commands', 'Commands & functions', 'Connect your UI to the server', openCommands]].map(([icon, title, desc, fn]) => h('button', {class: 'nav-link', onclick: fn}, h('span', {html: svg(icon)}), h('span', {}, h('strong', {}, title), h('small', {}, desc)), h('span', {}, '↗'))));
    return;
  }
  if (body.dataset.view !== 'layers') {
    body.dataset.view = 'layers';
    const search = h('input', {class: 'inp', id: 'layerSearch', placeholder: 'Search layers…', 'aria-label': 'Search layers', value: S.layerQuery, oninput: ev => { S.layerQuery = ev.target.value; renderNavigator(); }});
    body.replaceChildren(h('div', {class: 'nav-search'}, search), h('div', {id: 'layerList', role: 'listbox', 'aria-label': 'Layers', 'aria-multiselectable': 'true'}));
  }
  const list = $('#layerList'), scroll = body.scrollTop;
  list.replaceChildren(layersPanel());
  if (S.layerQuery && !list.querySelector('.layer')) list.replaceChildren(h('div', {class: 'empty'}, 'No matching layers.'));
  body.scrollTop = scroll;
}

function renderWorkspace() {
  $$('button[title]:not([aria-label])').forEach(b => b.setAttribute('aria-label', b.title));
  stage.classList.toggle('show-grid', S.grid);
  $('#gridBtn').setAttribute('aria-pressed', S.grid);
  const empty = $('#emptyCanvas');
  empty.hidden = !!doc().elements.length || doc().type === 'logic' || doc().type === 'chest';
  if (!empty.hidden) empty.replaceChildren(h('span', {class: 'empty-symbol', html: svg('rect')}), h('h2', {}, 'Make something yours.'), h('p', {}, 'Add your first element, or start with a template.'), h('div', {}, h('button', {class: 'btn primary', onclick: () => { S.nav = 'insert'; renderNavigator(); }}, '+ Add element'), h('button', {class: 'btn ghost', onclick: openNewDocument}, 'Browse templates')));
}

function uniqueDocument(d) {
  const original = d.id || slug(d.name) || 'ui';
  let suffix = 2;
  while (S.docs.some(x => x.id === d.id)) d.id = original.slice(0, 42) + '_' + suffix++;
  return d;
}

function openNewDocument() {
  const templates = [['Blank canvas', 'Start from scratch', 'screen', null], ['Tabbed shop', 'Items, tabs & purchase actions', 'screen', tplShop], ['Player profile', 'A ready-to-edit player menu', 'screen', tplMenu], ['HUD overlay', 'Stats that stay on screen', 'hud', tplHud], ['World board', 'An interactive welcome display', 'world', tplBoard], ['Chest inventory', 'Native slots & custom backgrounds', 'chest', null]];
  const name = h('input', {class: 'inp', id: 'newDocName', value: 'Untitled UI', 'aria-label': 'New UI name'});
  let picked = 0;
  const cards = h('div', {class: 'template-grid'});
  const draw = () => cards.replaceChildren(...templates.map(([title, desc, type, build], i) => h('button', {class: 'template-card' + (i === picked ? ' on' : ''), 'aria-pressed': i === picked, onclick: () => { picked = i; draw(); }},
    h('div', {class: 'template-visual template-' + type}, h('span', {}, build ? '▤' : '+'), h('i'), h('i'), h('i')),
    h('strong', {}, title), h('small', {}, desc))));
  draw();
  modal('Create a UI', [h('p', {}, 'Choose a starting point. Everything is editable.'), cards, field('UI name', name)], [close => h('button', {class: 'btn primary', onclick: () => {
    const [title, desc, type, build] = templates[picked], d = build ? build() : newDoc(type);
    d.name = name.value.trim() || title; d.id = slug(d.name) || 'ui';
    openTab(uniqueDocument(d)); close();
  }}, 'Create UI')]);
}

function reopenClosed() {
  const last = S.closedTabs.pop();
  if (!last) return toast('No closed documents to reopen.');
  last.d = uniqueDocument(last.d);
  openTab(last.d);
  S.sessions[S.cur] = last.session;
  restoreSession();
}

function openCommandPalette() {
  const search = h('input', {class: 'inp command-search', placeholder: 'Search commands, tools, or layers…', 'aria-label': 'Search commands', autocomplete: 'off'});
  const results = h('div', {class: 'command-results', role: 'listbox', id: 'commandResults'});
  let index = 0, matches = [], closePalette;
  const commands = [
    ['Create a new UI', openNewDocument, 'File'], ['Reopen closed UI', reopenClosed, 'File'],
    ...Object.entries(MENUS).flatMap(([category, entries]) => entries.filter(Array.isArray).map(([title, run, key]) => [title, run, category, key])),
    ...TOOLS.map(([type, title]) => ['Add ' + title.replace(/ \(.+\)/, ''), () => { if (type === 'select') setTool(type); else addElement(type); }, 'Insert']),
    ...doc().elements.map(e => ['Select ' + e.name, () => { S.sel = [e.id]; S.side = 'props'; S.nav = 'layers'; render(); }, 'Layer'])
  ];
  const run = item => { closePalette(); item[1](); };
  const draw = () => {
    const q = search.value.trim().toLowerCase();
    matches = commands.filter(c => (c[0] + ' ' + c[2]).toLowerCase().includes(q)).slice(0, 30);
    index = clamp(index, 0, Math.max(0, matches.length - 1));
    results.replaceChildren(...matches.map((c, i) => h('button', {class: 'command-result' + (i === index ? ' on' : ''), role: 'option', 'aria-selected': i === index, id: 'command_' + i, onclick: () => run(c)}, h('span', {}, c[0]), h('small', {}, c[2]), c[3] ? h('kbd', {}, c[3]) : null)));
    if (!matches.length) results.append(h('div', {class: 'empty'}, 'No results. Try “text”, “preview”, or “shop”.'));
    search.setAttribute('aria-activedescendant', matches.length ? 'command_' + index : '');
    results.children[index]?.scrollIntoView({block: 'nearest'});
  };
  search.addEventListener('input', () => { index = 0; draw(); });
  search.addEventListener('keydown', ev => {
    if (ev.key === 'ArrowDown' || ev.key === 'ArrowUp') { ev.preventDefault(); index = clamp(index + (ev.key === 'ArrowDown' ? 1 : -1), 0, matches.length - 1); draw(); }
    if (ev.key === 'Enter' && matches[index]) { ev.preventDefault(); run(matches[index]); }
  });
  const m = modal('Find anything', [search, results, h('p', {class: 'palette-hint'}, '↑ ↓ to navigate · Enter to run · Esc to close')]);
  closePalette = m.close; m.el.classList.add('command-palette'); draw(); search.focus();
}

function cloneQuickActions(source, copies, nodes = doc().script.nodes) {
  return source.flatMap((e, i) => {
    const newId = copies[i].id;
    const remap = {[`quick_event_${e.id}`]: `quick_event_${newId}`, [`quick_action_${e.id}`]: `quick_action_${newId}`};
    return nodes.filter(n => remap[n.id]).map(n => {
      const out = clone(n); out.id = remap[n.id];
      if (out.props.element === e.id) out.props.element = newId;
      for (const port of Object.keys(out.next)) out.next[port] = out.next[port].map(id => remap[id] || id);
      out.y += 80;
      return out;
    });
  });
}

function quickActionPanel(e) {
  const eventId = 'quick_event_' + e.id, actionId = 'quick_action_' + e.id;
  const current = doc().script.nodes.find(n => n.id === actionId);
  const types = [['', 'No quick action'], ['message', 'Send a message'], ['open_ui', 'Open another UI'], ['close_ui', 'Close this UI'], ['run_command', 'Run a command']];
  const key = {message: 'text', open_ui: 'ui', run_command: 'command'}[current?.type];
  return sec('Click action', field('Action', selectInput(types, () => current?.type || '', type => {
    const nodes = doc().script.nodes;
    doc().script.nodes = nodes.filter(n => ![eventId, actionId].includes(n.id));
    if (type) doc().script.nodes.push({id: eventId, type: 'on_click', x: 40, y: 40, props: {element: e.id, button: 'left'}, next: {then: [actionId]}},
      {id: actionId, type, x: 360, y: 40, props: type === 'message' ? {text: 'Hello {player}!'} : type === 'run_command' ? {command: 'spawn', as: 'player'} : {ui: ''}, next: {}});
  })), key ? field(key === 'ui' ? 'UI ID' : key === 'text' ? 'Message' : 'Command', textInput(() => current.props[key] || '', x => current.props[key] = x, {area: key === 'text'})) : null,
  h('button', {class: 'btn sm ghost', onclick: () => { if (!S.scriptOpen) toggleScript(); }}, 'Open script graph ↗'));
}

function slotDefaults(slot) { return {slot, item: 'minecraft:stone', amount: 1, name: '', lore: '', glint: false, itemModel: '', headOwner: '', blank: false, hideTooltip: false, frames: [], frameTicks: 10}; }
function slotMatches(spec, slot) {
  if (!spec || spec === 'any') return true;
  return String(spec).split(',').some(part => { const [a, b = a] = part.trim().split('-').map(Number); return Number.isFinite(a) && Number.isFinite(b) && slot >= Math.min(a, b) && slot <= Math.max(a, b); });
}
function renderChestSlots(d, host, preview) {
  for (let i = 0; i < d.settings.rows * 9; i++) {
    const item = d.slots.find(s => s.slot === i);
    const cell = h('div', {class: 'chest-guide' + (!preview && S.selectedSlot === i ? ' selected' : ''), style: {left: (8 + i % 9 * 18) + 'px', top: (d.settings.guiTop + 18 + Math.floor(i / 9) * 18) + 'px', width: '16px', height: '16px', pointerEvents: !preview && S.side === 'slots' ? 'auto' : 'none'}, title: 'Slot ' + i});
    const frames = item?.frames || [];
    const id = frames.length && preview ? frames[Math.floor(PV.ticks / Math.max(1, item.frameTicks)) % frames.length] : item?.item;
    const url = id && iconUrl(id);
    if (item && !item.blank) {
      if (url) cell.append(h('img', {src: url, alt: '', style: {width: '100%', height: '100%', imageRendering: 'pixelated'}}));
      else cell.append(h('span', {}, String(id || '').replace('minecraft:', '').slice(0, 3).toUpperCase()));
    } else if (!preview) cell.append(String(i));
    if (preview) cell.style.borderColor = 'transparent';
    else cell.addEventListener('pointerdown', ev => { ev.stopPropagation(); S.selectedSlot = i; S.sel = []; renderPanel(); });
    host.append(cell);
  }
}
function resizeChest(d) {
  d.settings.rows = clamp(Math.round(Number(d.settings.rows) || 6), 1, 6);
  d.settings.guiTop = clamp(Math.round(Number(d.settings.guiTop) || 0), 0, 64);
  d.canvas = {width: 176, height: d.settings.guiTop + d.settings.rows * 18 + 114};
}
function chestPanel() {
  const d = doc(), index = clamp(S.selectedSlot ?? 0, 0, d.settings.rows * 9 - 1);
  const item = d.slots.find(s => s.slot === index);
  const set = (key, value) => { let s = d.slots.find(s => s.slot === index); if (!s) d.slots.push(s = slotDefaults(index)); s[key] = value; };
  const val = key => (item || slotDefaults(index))[key];
  return h('div', {}, sec('Inventory slots', h('p', {class: 'lab'}, 'Select a slot to edit its item. Slot numbers start at 0.'),
    h('div', {class: 'slot-grid'}, ...Array.from({length: d.settings.rows * 9}, (_, i) => h('button', {class: 'slot-cell' + (i === index ? ' on' : '') + (d.slots.some(s => s.slot === i) ? ' filled' : ''), 'aria-label': 'Slot ' + i, 'aria-pressed': i === index, onclick: () => { S.selectedSlot = i; renderPanel(); }}, String(i))))),
    sec('Slot ' + index, field('Item', textInput(() => val('item'), x => set('item', x), {ph: 'minecraft:stone'})),
      h('button', {class: 'btn sm', onclick: () => pickItem(id => mutate(() => set('item', id)))}, 'Pick item…'),
      field('Amount', numInput('#', () => val('amount'), x => set('amount', clamp(Math.round(x), 1, 99)), {min: 1, max: 99})),
      field('Name', textInput(() => val('name'), x => set('name', x))),
      field('Lore', textInput(() => val('lore'), x => set('lore', x), {area: true, ph: 'One line per row'})),
      field('Model', textInput(() => val('itemModel'), x => set('itemModel', x))),
      field('Head owner', textInput(() => val('headOwner'), x => set('headOwner', x))),
      checkInput('Enchantment glint', () => val('glint'), x => set('glint', x)),
      checkInput('Invisible clickable item', () => val('blank'), x => set('blank', x)),
      checkInput('Hide tooltip', () => val('hideTooltip'), x => set('hideTooltip', x)),
      field('Frames', textInput(() => val('frames').join(', '), x => set('frames', x.split(',').map(v => v.trim()).filter(Boolean)), {ph: 'minecraft:stone, minecraft:diamond'})),
      field('Ticks/frame', numInput('t', () => val('frameTicks'), x => set('frameTicks', Math.max(1, Math.round(x))), {min: 1})),
      h('button', {class: 'btn', onclick: () => { const n = node('on_slot_click', 40, 40, {slot: String(index), button: 'any'}); mutate(dd => dd.script.nodes.push(n)); focusNode(n); }}, 'Add slot click event ↗'),
      item ? h('button', {class: 'btn danger', onclick: () => mutate(dd => dd.slots = dd.slots.filter(s => s.slot !== index))}, 'Clear slot') : null));
}

function initializeWorkspace() {
  $('#layersTab').onclick = () => { S.nav = 'layers'; renderNavigator(); };
  $('#insertTab').onclick = () => { S.nav = 'insert'; renderNavigator(); };
  $('#templatesBtn').onclick = openNewDocument;
  $('#helpBtn').onclick = openHelp;
  $('#searchBtn').onclick = openCommandPalette;
  $('#fitBtn').onclick = fit;
  $('#gridBtn').onclick = () => { S.grid = !S.grid; renderWorkspace(); };
  window.addEventListener('pagehide', saveLocal);
  window.addEventListener('blur', () => { S.spaceDown = false; });
}

(function boot() {
  initializeWorkspace();
  const saved = store.get('docs', null);
  if (saved && Array.isArray(saved.docs) && saved.docs.length) {
    S.docs = saved.docs.flatMap(d => { try { return [normalize(d)]; } catch { return []; } });
    if (!S.docs.length) S.docs = [newDoc()];
    S.cur = clamp(saved.cur || 0, 0, S.docs.length - 1);
  } else {
    S.docs = [tplShop(), tplMenu(), tplHud(), tplBoard()].map(normalize);
    S.cur = 0;
  }
  S.anim.id = doc().animations[0]?.id || null;
  render();
  requestAnimationFrame(fit);
  const base = serverDefault();
  if (base) connectServer(base, store.get('token', ''), true);
  else renderSrv();
  // Help is always available; keep the first canvas unobstructed.
})();
window.SilentUIEditor = {S, doc, exportJson, tplMenu, tplHud, tplBoard, tplShop, normalize, validate, BUILTIN_PRESETS, insertPreset, presetFromSelection, SRV, connectServer, groupSel, ungroupSel, startTutorial, toggleLibrary};
