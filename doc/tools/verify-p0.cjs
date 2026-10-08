/* P0+第8章 改造联调验证 */
const { sm2 } = require('E:/project/springbladeandreact/ant-design-pro/node_modules/sm-crypto');
const PUBLIC_KEY = '04ac02fe94f4cac62a57a2335cc96a075a1ee41cea3b211bd7acbb9cf579b7e601b9ece2b0cfab64dca268b6942bf556af67cfe226a5cf28d936039c43e4bb12c1';
const BASIC = 'Basic ' + Buffer.from('sword:sword_secret').toString('base64');
const H = { 'blade-auth': '', Authorization: BASIC, 'Tenant-Id': '000000', 'Content-Type': 'application/json' };
async function api(method, url, body) {
  const headers = body ? H : { 'blade-auth': H['blade-auth'], Authorization: BASIC, 'Tenant-Id': '000000' };
  const r = await fetch('http://localhost:8000' + url, { method, headers, body: body ? JSON.stringify(body) : undefined });
  const j = await r.json().catch(() => null);
  return { status: r.status, body: j };
}
(async () => {
  const enc = sm2.doEncrypt('ant.design', PUBLIC_KEY, 0);
  const body = new URLSearchParams({ grantType: 'captcha', tenantId: '000000', account: 'admin', password: enc, scope: 'all' }).toString();
  const lr = await fetch('http://localhost:8000/api/blade-auth/token', { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded', Authorization: BASIC, 'Tenant-Id': '000000' }, body });
  const token = (await lr.json())?.data?.accessToken;
  if (!token) { console.log('LOGIN-FAIL'); return; }
  H['blade-auth'] = 'bearer ' + token;
  console.log('LOGIN-OK');
  let r = await api('POST', '/api/blade-system/user/check?field=account&value=admin&tenantId=000000');
  console.log('1.check account=admin available =', JSON.stringify(r.body?.data), '(expect false)');
  r = await api('POST', '/api/blade-system/user/check?field=workCode&value=EMP_NOPE_9999&tenantId=000000');
  console.log('2.check workCode free =', JSON.stringify(r.body?.data), '(expect true)');
  r = await api('GET', '/api/blade-system/user/work-code/next');
  console.log('3.nextWorkCode =', JSON.stringify(r.body?.data), '(expect EMP+date+0001)');
  r = await api('POST', '/api/blade-system/dept/submit', { deptName: 'P0测试分部', deptType: 1, parentId: 0, sort: 99 });
  console.log('4.dept submit 分部 =', r.status, r.body?.msg || r.body?.data);
  const listR = await api('GET', '/api/blade-system/dept/list?deptName=P0测试分部');
  const sub = (listR.body?.data || []).find((d) => d.deptName === 'P0测试分部');
  console.log('  分部 id =', sub?.id, 'deptType =', sub?.deptType);
  r = await api('POST', '/api/blade-system/dept/submit', { deptName: 'P0测试部门', deptType: 2, parentId: sub?.id, sort: 99 });
  console.log('5.dept submit 部门 =', r.status, r.body?.msg || r.body?.data);
  const listR2 = await api('GET', '/api/blade-system/dept/list?deptName=P0测试部门');
  const dept = (listR2.body?.data || []).find((d) => d.deptName === 'P0测试部门');
  console.log('  部门 id =', dept?.id, 'subcompany_id =', dept?.subcompanyId, '(expect', sub?.id, ')');
  r = await api('POST', '/api/blade-system/user/submit', { tenantId: '000000', account: 'p0test1', password: '123456', name: 'p0昵称', realName: 'P0测试用户', personStatus: 1, deptId: dept?.id, workCode: '', phone: '', email: '' });
  console.log('6.user submit =', r.status, r.body?.msg || r.body?.code);
  const detail = await api('GET', '/api/blade-system/user/detail?account=p0test1');
  const u = detail.body?.data;
  console.log('  回读 workCode =', u?.workCode, '(expect auto)', ' personStatus =', u?.personStatus, ' deptId =', u?.deptId);
  if (u?.workCode) {
    r = await api('POST', '/api/blade-system/user/check?field=workCode&value=' + encodeURIComponent(u.workCode) + '&tenantId=000000');
    console.log('7.check 生成工号 available =', JSON.stringify(r.body?.data), '(expect false)');
  }
  r = await api('POST', '/api/blade-system/dept/cancel?ids=' + dept?.id);
  console.log('8.dept cancel 部门(有人) =', JSON.stringify(r.body?.msg || r.body?.data), '(期望被拦截)');
  const udel = await api('POST', '/api/blade-system/user/remove?ids=' + u?.id);
  console.log('9.user remove =', udel.status, '(工号应已置空)');
  r = await api('POST', '/api/blade-system/dept/cancel?ids=' + dept?.id);
  console.log('10.dept cancel 部门(无人) =', JSON.stringify(r.body?.msg || r.body?.data), '(期望成功)');
  r = await api('POST', '/api/blade-system/dept/cancel?ids=' + sub?.id);
  console.log('11.dept cancel 分部(子已封存) =', JSON.stringify(r.body?.msg || r.body?.data), '(期望成功)');
  r = await api('POST', '/api/blade-system/dept/is-canceled?ids=' + sub?.id);
  console.log('12.dept is-canceled 分部 =', JSON.stringify(r.body?.msg || r.body?.data), '(期望成功)');
})();
