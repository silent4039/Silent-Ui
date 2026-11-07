const {test, expect} = require('@playwright/test');

test.beforeEach(async ({page}) => {
  await page.goto('/editor/?offline=1');
  await expect(page.locator('.artboard')).toBeVisible();
});

test('wave builder staggers one pulse per element', async ({page}) => {
  const result = await page.evaluate(() => {
    const E = window.SilentUIEditor;
    const els = [0, 1, 2, 3].map(i => ({id: 'a' + i, type: 'image', x: 0, y: 100 - i * 30, w: 20, h: 10, sprite: 'chev', scale: 1}));
    const {anim, changes} = E.buildWave(els, {effect: 'color', from: '#cfcfcf', to: '#29b6ff', pulse: 10, delay: 3, gap: 14, loop: true, autoplay: true, name: 'w'});
    return {length: anim.length, tracks: anim.tracks.map(t => ({el: t.element, prop: t.property, keys: t.keys.map(k => [k.t, k.v])})), tint: changes.every(c => c.set.tintSprite)};
  });
  expect(result.length).toBe(3 * 3 + 10 + 14);
  expect(result.tint).toBe(true);
  expect(result.tracks.map(t => t.prop)).toEqual(['fill', 'fill', 'fill', 'fill']);
  // the third element peaks 5 ticks after it starts at tick 6
  expect(result.tracks[2].keys).toContainEqual([11, 0x29b6ff]);
  for (const t of result.tracks) {
    const ticks = t.keys.map(k => k[0]);
    expect([...ticks].sort((a, b) => a - b)).toEqual(ticks);
    expect(new Set(ticks).size).toBe(ticks.length);
  }
});

test('hologram, chest GUI and guide arrow templates load, validate and export', async ({page}) => {
  const errors = [];
  page.on('pageerror', e => errors.push(e.message));
  const out = await page.evaluate(() => {
    const E = window.SilentUIEditor;
    return ['tplHologram', 'tplChestGui', 'tplGuidePath'].map(name => {
      const d = E.normalize(E[name]());
      E.S.docs.push(d); E.S.cur = E.S.docs.length - 1; render();
      const json = JSON.parse(E.exportJson());
      return {name, type: json.type, warnings: E.validate(d), slots: (json.slots || []).length, anims: json.animations.length};
    });
  });
  expect(errors).toEqual([]);
  for (const t of out) expect(t.warnings).toEqual([]);
  expect(out.find(t => t.name === 'tplChestGui')).toMatchObject({type: 'chest', slots: 8});
  expect(out.find(t => t.name === 'tplGuidePath').anims).toBe(1);
});

test('pop-up node floats text in the preview', async ({page}) => {
  await page.evaluate(() => {
    const E = window.SilentUIEditor;
    E.S.docs.push(E.normalize(E.tplChestGui())); E.S.cur = E.S.docs.length - 1; render(); startPreview();
    pvExec({type: 'popup', props: {text: '<gold>+100', at: 'front', ticks: '30', rise: '0.6', scale: '1'}, next: {}}, {});
  });
  await expect(page.locator('.pv-popup')).toHaveCount(1);
  await expect(page.locator('.pv-popup')).toHaveCount(0, {timeout: 4000});
});
