/* 临时验证：服务端分页 + pageSize 兼容 + 组织树过滤。跑完即删。 */
const { sm2 } = require('D:/project/springbladeandreact/ant-design-pro/node_modules/sm-crypto');
const PUBLIC_KEY = '04ac02fe94f4cac62a57a2335cc96a075a1ee41cea3b211bd7acbb9cf579b7e601b9ece2b0cfab64dca268b6942bf556af67cfe226a5cf28d936039c43e4bb12c1';
const BASIC = 'Basic ' + Buffer.from('sword:sword_secret').toString('base64');
const AUTH = 'http://localhost:8100';
const SYS = 'http://localhost:8106';

async function login(account, password, tenantId = '000000') {
  const enc = sm2.doEncrypt(password, PUBLIC_KEY, 0);
  const body = new URLSearchParams({ grantType: 'captcha', tenantId, account, password: enc, scope: 'all' }).toString();
  const r = await fetch(AUTH + '/token', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded', Authorization: BASIC, 'Tenant-Id': tenantId },
    body,
  });
  const j = await r.json().catch(() => null);
  return j?.data?.accessToken;
}

async function api(qs, token) {
  const headers = { 'blade-auth': 'bearer ' + token, Authorization: BASIC, 'Tenant-Id': '000000' };
  const r = await fetch(SYS + '/user/list' + (qs ? '?' + qs : ''), { headers });
  const j = await r.json().catch(() => null);
  const d = j?.data || {};
  return { total: d.total, n: (d.records || []).length, first: (d.records || [])[0]?.account };
}

(async () => {
  const token = await login('admin', 'ant.design');
  if (!token) { console.log('LOGIN-FAIL'); return; }

  let r = await api('tenantId=000000', token);
  console.log('1.第1页(缺省 size=10): total=' + r.total + ' 返回=' + r.n + ' 首行=' + r.first);

  r = await api('tenantId=000000&current=2', token);
  console.log('2.第2页(current=2):    total=' + r.total + ' 返回=' + r.n + ' 首行=' + r.first + (r.first !== 'ny' ? '  ✅翻页生效' : '  ❌与第1页相同'));

  r = await api('tenantId=000000&current=1&pageSize=5', token);
  console.log('3.pageSize=5:          total=' + r.total + ' 返回=' + r.n + (r.n === 5 ? '  ✅pageSize 生效' : '  ❌'));

  r = await api('tenantId=000000&current=2&pageSize=5', token);
  console.log('4.current=2&pageSize=5: 返回=' + r.n + ' 首行=' + r.first);

  r = await api('tenantId=000000&deptId=2000000003&pageSize=10', token);
  console.log('5.组织过滤+分页:        total=' + r.total + ' 返回=' + r.n + '  首行=' + r.first);
})().catch((e) => console.log('ERR', e.message));
