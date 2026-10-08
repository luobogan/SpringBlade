/* P3 状态流转工作流化 + 伴生初始化 —— 联调验证
 * 说明：本环境 Nacos 未启动（8848 无监听）→ 网关 81 无路由，故直连服务：
 *   blade-auth   http://localhost:8100   （token 端点 POST /token）
 *   blade-system http://localhost:8106
 * 前置：blade-system / blade-auth 需用包含 P3 改动的新包重启；
 *       blade-workflow 未启动时不验证「审批闭环」，只验证降级直改与登录拦截。
 */
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
  return { status: r.status, token: j?.data?.accessToken, msg: j?.msg };
}

async function api(method, path, token, body) {
  const headers = { 'blade-auth': 'bearer ' + token, Authorization: BASIC, 'Tenant-Id': '000000' };
  if (body) headers['Content-Type'] = 'application/json';
  const r = await fetch(SYS + path, { method, headers, body: body ? JSON.stringify(body) : undefined });
  const j = await r.json().catch(() => null);
  return { status: r.status, body: j };
}

const ACCOUNT = 'p3test1';
const PWD = 'P3@test123';

(async () => {
  const admin = await login('admin', 'ant.design');
  if (!admin.token) { console.log('0.admin LOGIN-FAIL', JSON.stringify(admin)); return; }
  console.log('0.admin LOGIN-OK');

  // 清理历史残留
  const old = await api('GET', '/user/detail?account=' + ACCOUNT, admin.token);
  if (old.body?.data?.id) await api('POST', '/user/remove?ids=' + old.body.data.id, admin.token);

  // 1. 建档 + P3-3 伴生初始化（信息完善度）
  const encPwd = sm2.doEncrypt(PWD, PUBLIC_KEY, 0);
  const adminDetail = await api('GET', '/user/detail?account=admin', admin.token);
  const a = adminDetail.body?.data || {};
  let r = await api('POST', '/user/submit', admin.token, {
    tenantId: '000000', account: ACCOUNT, password: encPwd,
    name: 'P3测试昵称', realName: 'P3测试用户', personStatus: 1,
    deptId: a.deptId, roleId: a.roleId, postId: a.postId,
  });
  console.log('1.submit =', r.status, r.body?.msg || r.body?.code);
  const detail = await api('GET', '/user/detail?account=' + ACCOUNT, admin.token);
  const uid = detail.body?.data?.id;

  r = await api('GET', '/user/complete-status?userId=' + uid, admin.token);
  const cs = r.body?.data || {};
  console.log('2.完善度 =', r.status, JSON.stringify(cs), '→ 项数 =', Object.keys(cs).length);

  // 3. 可用流转（当前正式 1 → 应命中「离职 1→4」）
  r = await api('GET', '/user/status-flow/available?userId=' + uid, admin.token);
  const av = Array.isArray(r.body?.data) ? r.body.data : [];
  console.log('3.可用流转 =', r.status,
    av.length ? av.map((f) => `${f.fromStatus}->${f.toStatus}(${f.flowName},${f.flowKey ? '审批' : '直改'})`).join(' | ') : '(无/接口未部署)');

  // 4. 降级直改：正式 1 → 临时 2（配置表无此映射，应直接变更）
  r = await api('POST', '/user/status-flow/start', admin.token, { userId: uid, toStatus: 2, opinion: 'P3 联调：直改临时' });
  console.log('4.直改 1→2 =', r.status, r.body?.msg || r.body?.code);

  // 5. 流转记录
  r = await api('GET', '/user/status-flow/records?userId=' + uid, admin.token);
  const recs = Array.isArray(r.body?.data) ? r.body.data : [];
  console.log('5.流转记录 =', r.status, '条数 =', recs.length,
    recs.map((x) => `${x.fromStatus}->${x.toStatus} mode=${x.mode} flowStatus=${x.flowStatus}`).join(' | '));

  // 6. 在职(2 临时) 可登录
  let t = await login(ACCOUNT, PWD);
  console.log('6.在职(临时)登录 =', t.status, t.token ? 'LOGIN-OK' : 'LOGIN-FAIL', t.msg || '');

  // 7. 直改为解聘 4（配置表无 2→4，走直改）
  r = await api('POST', '/user/status-flow/start', admin.token, { userId: uid, toStatus: 4, opinion: 'P3 联调：转解聘' });
  console.log('7.直改 2→4 =', r.status, r.body?.msg || r.body?.code);

  // 8. P3-5 离职登录拦截：解聘账号应被拒
  t = await login(ACCOUNT, PWD);
  console.log('8.解聘账号登录 =', t.status, t.token ? 'LOGIN-OK(未拦截!)' : 'LOGIN-FAIL(已拦截)', t.msg || '');

  // 9. 恢复正式，验证拦截可解除
  await api('POST', '/user/status-flow/start', admin.token, { userId: uid, toStatus: 1, opinion: 'P3 联调：恢复' });
  t = await login(ACCOUNT, PWD);
  console.log('9.恢复正式后登录 =', t.status, t.token ? 'LOGIN-OK' : 'LOGIN-FAIL', t.msg || '');

  // 10. 清理
  r = await api('POST', '/user/remove?ids=' + uid, admin.token);
  console.log('10.清理测试用户 =', r.status);
})();
