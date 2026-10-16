/* F06/F07 门户路由可达性验证（重启 dev server 后运行）
 * 登录 admin → 访问 /portal/home 与 /portal/defaultportal → 收集控制台报错与正文
 */
const path = require('path');
const PW = process.env.PLAYWRIGHT_MODULE_DIR;
const CHROME = process.env.PLAYWRIGHT_CHROMIUM_PATH;
const { chromium } = require(path.join(PW, 'playwright'));

const BASE = 'http://localhost:8000';

(async () => {
  const browser = await chromium.launch({ executablePath: CHROME, args: ['--no-sandbox'] });
  const ctx = await browser.newContext();
  const page = await ctx.newPage();
  const errors = [];
  page.on('console', (m) => { if (m.type() === 'error') errors.push(m.text()); });
  page.on('pageerror', (e) => errors.push('PAGEERROR: ' + e.message));

  // ---- 登录（ant-design-pro 登录表单：账号/密码用中文 placeholder 前缀匹配）----
  await page.goto(BASE + '/user/login', { waitUntil: 'networkidle' }).catch(() => {});
  await page.waitForSelector('input[placeholder^="用户名"]', { timeout: 10000 }).catch(() => {});
  await page.fill('input[placeholder^="租户ID"]', '000000').catch(() => {});
  await page.fill('input[placeholder^="用户名"]', 'admin').catch((e) => console.log('fill user err', e.message));
  await page.fill('input[placeholder^="密码"]', 'ant.design').catch((e) => console.log('fill pwd err', e.message));
  await page.click('button[type="submit"]').catch((e) => console.log('click login err', e.message));
  await page.waitForFunction(() => {
    try { return !!(localStorage.getItem('sword-token') || localStorage.getItem('sword-access-token')); } catch (e) { return false; }
  }, { timeout: 15000 }).catch(() => {});
  await page.waitForTimeout(1500);
  const token = await page.evaluate(() => localStorage.getItem('sword-token') || localStorage.getItem('sword-access-token') || '');
  console.log('LOGIN', token ? 'OK' : 'FAIL');

  // ---- 逐个路由验证 ----
  for (const route of ['/portal/home', '/portal/defaultportal']) {
    errors.length = 0;
    await page.goto(BASE + route, { waitUntil: 'networkidle' }).catch(() => {});
    await page.waitForTimeout(2500);
    const bodyText = await page.evaluate(() => (document.body.innerText || '').slice(0, 300));
    const is404 = /页面不存在|404|Page Not Found|不存在/.test(bodyText);
    const loadFail = errors.some((e) => /组件导入失败|页面组件加载失败|全部候选未命中|Cannot find module/.test(e));
    const onLogin = /流程引擎系统/.test(bodyText);
    const ok = !is404 && !loadFail && bodyText.trim().length > 0 && !onLogin;
    console.log(`ROUTE ${route} => ${ok ? 'PASS' : 'FAIL'} | 404=${is404} loadFail=${loadFail} onLoginPage=${onLogin}`);
    console.log('   body:', bodyText.replace(/\s+/g, ' ').slice(0, 140));
    const rel = errors.filter((e) => /组件导入失败|页面组件加载失败|全部候选未命中|Cannot find module/.test(e)).slice(0, 2);
    if (rel.length) console.log('   loadErr:', rel.join(' || '));
  }
  await browser.close();
})().catch((e) => { console.log('SCRIPT-ERR', e.message); process.exit(1); });
