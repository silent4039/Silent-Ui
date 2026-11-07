const {defineConfig} = require('@playwright/test');
module.exports = defineConfig({
  testDir: './tests/editor',
  fullyParallel: true,
  workers: 2,
  timeout: 30000,
  reporter: 'list',
  use: {baseURL: 'http://127.0.0.1:48163', viewport: {width: 1440, height: 900}, trace: 'retain-on-failure'},
  webServer: {command: 'node tools/editor-server.cjs', url: 'http://127.0.0.1:48163/editor/', reuseExistingServer: !process.env.CI}
});
