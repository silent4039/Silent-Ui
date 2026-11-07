const {test, expect} = require('@playwright/test');

test.beforeEach(async ({page}) => {
  await page.goto('/editor/?offline=1');
  await expect(page.locator('.artboard')).toBeVisible();
});

async function createUI(page, name = 'My menu', template = 'Blank canvas') {
  await page.getByRole('button', {name: 'New UI', exact: true}).click();
  await page.getByRole('button', {name: new RegExp(template)}).click();
  await page.getByRole('textbox', {name: 'New UI name'}).fill(name);
  await page.getByRole('button', {name: 'Create UI', exact: true}).click();
}
async function add(page, name) {
  await page.getByRole('tab', {name: 'Insert', exact: true}).click();
  await page.getByRole('button', {name: 'Add ' + name, exact: true}).click();
}
const data = page => page.evaluate(() => JSON.parse(window.SilentUIEditor.exportJson()));

test('loads all bundled examples with no JavaScript errors', async ({page}) => {
  const errors = [];
  page.on('pageerror', e => errors.push(e.message));
  for (const tab of ['Shop', 'Menu', 'HUD', 'Board']) {
    const match = page.locator('#tabs .tab').filter({hasText: new RegExp(tab, 'i')});
    if (await match.count()) await match.first().click();
  }
  expect(errors).toEqual([]);
  await expect(page.locator('#navigator')).toBeVisible();
  await expect(page.locator('#panel')).toBeVisible();
});

test('inserts and edits text in the unified inspector', async ({page}) => {
  await createUI(page);
  await add(page, 'Text');
  await expect(page.getByText('Layout', {exact: true})).toBeVisible();
  const text = page.locator('#f_text');
  await text.fill('Welcome, {player}!'); await text.press('Tab');
  expect((await data(page)).elements[0].text).toBe('Welcome, {player}!');
  await expect(page.locator('#saveState')).toHaveText('Saved locally');
  await expect(page.locator('#navigator .layer')).toHaveCount(1);
});

test('keeps undo, selection and redo when switching documents', async ({page}) => {
  await createUI(page, 'History test'); await add(page, 'Button');
  await page.keyboard.press('Control+d');
  expect((await data(page)).elements).toHaveLength(2);
  await page.locator('#tabs .tab').first().click();
  await page.locator('#tabs .tab').filter({hasText: 'History test'}).click();
  await page.locator('#undoBtn').click();
  expect((await data(page)).elements).toHaveLength(1);
  await page.locator('#redoBtn').click();
  expect((await data(page)).elements).toHaveLength(2);
});

test('filters layers and selects them without hiding the inspector', async ({page}) => {
  await createUI(page); await add(page, 'Text'); await add(page, 'Button');
  await page.getByRole('textbox', {name: 'Search layers'}).fill('Text');
  await expect(page.locator('#navigator .layer')).toHaveCount(1);
  await page.locator('#navigator .layer .nm').click();
  await expect(page.locator('#f_text')).toBeVisible();
  await page.getByRole('textbox', {name: 'Search layers'}).fill('no such layer');
  await expect(page.getByText('No matching layers.')).toBeVisible();
});

test('runs command search with keyboard and contains modal shortcuts', async ({page}) => {
  await page.keyboard.press('Control+k');
  await page.getByRole('textbox', {name: 'Search commands'}).fill('New chest inventory');
  await page.keyboard.press('Enter');
  expect((await data(page)).type).toBe('chest');
  await page.keyboard.press('Control+k');
  await page.getByRole('textbox', {name: 'Search commands'}).fill('');
  const before = (await data(page)).elements.length;
  await page.getByRole('dialog').getByRole('button', {name: 'Close', exact: true}).focus();
  await page.keyboard.press('Delete');
  expect((await data(page)).elements).toHaveLength(before);
  await page.keyboard.press('Escape');
  await expect(page.getByRole('dialog')).toHaveCount(0);
});

test('creates native chest slots and exports their configuration', async ({page}) => {
  await createUI(page, 'Inventory test', 'Chest inventory');
  await page.getByRole('button', {name: 'Slots', exact: true}).click();
  await page.getByRole('button', {name: 'Slot 13', exact: true}).click();
  await page.getByRole('textbox', {name: 'Item', exact: true}).fill('minecraft:diamond');
  await page.getByRole('textbox', {name: 'Item', exact: true}).press('Tab');
  const d = await data(page);
  expect(d.slots).toEqual([expect.objectContaining({slot: 13, item: 'minecraft:diamond'})]);
  expect(d.canvas).toEqual({width: 176, height: 222});
  await page.getByRole('button', {name: 'Add slot click event'}).click();
  expect((await data(page)).script.nodes).toEqual([expect.objectContaining({type: 'on_slot_click', props: {slot: '13', button: 'any'}})]);
});

test('quick button action creates a runnable graph', async ({page}) => {
  await createUI(page); await add(page, 'Button');
  await page.getByRole('combobox', {name: 'Action', exact: true}).selectOption('message');
  await page.getByRole('textbox', {name: 'Message', exact: true}).fill('Clicked!');
  await page.getByRole('textbox', {name: 'Message', exact: true}).press('Tab');
  const d = await data(page), event = d.script.nodes.find(n => n.type === 'on_click');
  expect(event.props.element).toBe(d.elements[0].id);
  expect(d.script.nodes.find(n => n.id === event.next.then[0]).props.text).toBe('Clicked!');
  await page.locator('#previewBtn').click();
  await expect(page.locator('#preview')).toBeVisible();
  await page.locator('#pvBoard .el').click();
  await expect(page.locator('#pvChat')).toContainText('Clicked!');
  await page.keyboard.press('Escape');
  await expect(page.locator('#preview')).toBeHidden();
});

test('JSON import and export preserve unknown fields and editor locks stay local', async ({page}) => {
  await createUI(page); await add(page, 'Text');
  await page.evaluate(() => { const d = SilentUIEditor.doc(); d.custom = {compatible: true}; d.elements[0].locked = true; });
  const d = await data(page);
  expect(d.custom.compatible).toBe(true);
  expect(d.elements[0].locked).toBeUndefined();
  await page.locator('#exportBtn').click();
  const download = page.waitForEvent('download');
  await page.getByRole('button', {name: 'Download', exact: true}).click();
  expect((await download).suggestedFilename()).toBe('my_menu.json');
});

test('autosave restores work after a reload', async ({page}) => {
  await createUI(page, 'Persistent menu'); await add(page, 'Progress bar');
  await expect(page.locator('#saveState')).toHaveText('Saved locally');
  await page.reload();
  expect((await data(page)).name).toBe('Persistent menu');
  expect((await data(page)).elements[0].type).toBe('progress');
});

test('closed documents can be recovered', async ({page}) => {
  await createUI(page, 'Recover me'); await add(page, 'Text');
  await page.locator('#tabs .tab.active .x').click();
  await page.keyboard.press('Control+k');
  await page.getByRole('textbox', {name: 'Search commands'}).fill('Reopen closed UI');
  await page.keyboard.press('Enter');
  expect((await data(page)).name).toBe('Recover me');
  expect((await data(page)).elements).toHaveLength(1);
});

test('world settings match the supported server settings', async ({page}) => {
  await createUI(page, 'Hologram', 'World board');
  await page.getByRole('button', {name: 'UI settings', exact: true}).click();
  await page.getByRole('combobox', {name: 'Faces', exact: true}).selectOption('center');
  await page.getByText('Separate display for each player', {exact: true}).click();
  const d = await data(page);
  expect(d.settings.billboard).toBe('center'); expect(d.settings.perPlayer).toBe(true);
});

test('workspace fits laptop and phone widths without horizontal overflow', async ({page}) => {
  for (const width of [1440, 1024, 768, 390]) {
    await page.setViewportSize({width, height: 900});
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
    await expect(page.locator('#pushBtn')).toBeVisible();
    await expect(page.locator('#insertTab')).toBeVisible();
  }
});

test('canvas drag and resize are undoable', async ({page}) => {
  await createUI(page); await add(page, 'Button');
  const original = (await data(page)).elements[0];
  const box = await page.locator('.artboard .el').boundingBox();
  await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2);
  await page.mouse.down(); await page.mouse.move(box.x + box.width / 2 + 40, box.y + box.height / 2 + 30, {steps: 5}); await page.mouse.up();
  const moved = (await data(page)).elements[0];
  expect(moved.x).not.toBe(original.x);
  const handle = await page.locator('.handle[data-h="1,1"]').boundingBox();
  await page.mouse.move(handle.x + handle.width / 2, handle.y + handle.height / 2);
  await page.mouse.down(); await page.mouse.move(handle.x + 40, handle.y + 30, {steps: 5}); await page.mouse.up();
  expect((await data(page)).elements[0].w).toBeGreaterThan(original.w);
  await page.locator('#undoBtn').click();
  expect((await data(page)).elements[0].w).toBe(original.w);
});

test('duplicating a quick action keeps both buttons functional', async ({page}) => {
  await createUI(page); await add(page, 'Button');
  await page.getByRole('combobox', {name: 'Action', exact: true}).selectOption('message');
  await page.locator('#elementName').blur();
  await page.keyboard.press('Control+d');
  const d = await data(page);
  expect(d.elements).toHaveLength(2);
  expect(d.script.nodes.filter(n => n.type === 'on_click').map(n => n.props.element)).toEqual(d.elements.map(e => e.id));
});

test('animation colour tracks interpolate by channel', async ({page}) => {
  await createUI(page); await add(page, 'Rectangle');
  await page.evaluate(() => {
    const e = SilentUIEditor.doc().elements[0];
    const a = {id: 'colour', name: 'Colour', length: 20, tracks: [{element: e.id, property: 'fill', keys: [{t: 0, v: 0xff0000}, {t: 20, v: 0x0000ff}]}]};
    SilentUIEditor.doc().animations.push(a); SilentUIEditor.S.anim.id = a.id; SilentUIEditor.S.anim.t = 10;
  });
  await page.getByRole('button', {name: 'Animation timeline (A)', exact: true}).click();
  await expect(page.locator('.artboard .el .bg')).toHaveCSS('background-color', 'rgb(128, 0, 128)');
  await expect(page.getByText('Fill colour', {exact: true}).last()).toBeVisible();
});

test('failed browser storage is visible instead of claiming a save', async ({page}) => {
  await createUI(page);
  await page.evaluate(() => { Storage.prototype.setItem = () => { throw new DOMException('Quota exceeded', 'QuotaExceededError'); }; });
  await add(page, 'Text');
  await expect(page.locator('#saveState')).toHaveText('Storage full · export to save');
});

test('rejects invalid JSON without losing the current document', async ({page}) => {
  const before = await data(page);
  await page.keyboard.press('Control+k');
  await page.getByRole('textbox', {name: 'Search commands'}).fill('Code (JSON)');
  await page.keyboard.press('Enter');
  await page.locator('textarea.code').fill('{"elements":42}');
  await page.getByRole('button', {name: 'Apply', exact: true}).click();
  await expect(page.locator('.toast.err')).toContainText('elements must be an array');
  expect((await data(page)).id).toBe(before.id);
});

test('server connection verifies the token and uploads exact JSON', async ({page}) => {
  let upload;
  await page.route('http://silent.test:8163/**', async route => {
    const url = new URL(route.request().url());
    let body = {ok: true};
    if (url.pathname === '/assets/status') body = {state: 'disabled', version: '1.21.11'};
    if (url.pathname === '/api/upload') upload = {data: route.request().postDataJSON(), token: route.request().headers()['x-silentui-token']};
    await route.fulfill({status: 200, json: body});
  });
  await page.locator('#pushBtn').click();
  await page.locator('#srvUrl').fill('http://silent.test:8163/editor/');
  await page.locator('#srvToken').fill('test-token');
  await page.getByRole('button', {name: 'Connect', exact: true}).click();
  await expect(page.getByText('✓ Connected and upload token verified.')).toBeVisible();
  await page.getByRole('button', {name: 'Push example_shop', exact: true}).click();
  await expect(page.getByText('✓ Pushed and reloaded on the server.')).toBeVisible();
  expect(upload.token).toBe('test-token'); expect(upload.data).toEqual(await data(page));
});

test('wrong token gets an actionable error', async ({page}) => {
  await page.route('http://silent.test:8163/**', async route => {
    const ping = route.request().url().endsWith('/api/ping');
    await route.fulfill({status: ping ? 401 : 200, json: ping ? {error: 'Invalid token'} : {state: 'disabled'}});
  });
  await page.locator('#pushBtn').click();
  await page.locator('#srvUrl').fill('http://silent.test:8163');
  await page.locator('#srvToken').fill('wrong');
  await page.getByRole('button', {name: 'Connect', exact: true}).click();
  await expect(page.getByText(/✕ Server reached, but the upload token is invalid/)).toBeVisible();
});
