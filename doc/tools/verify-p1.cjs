/* P1 表单交互与密码策略对齐 —— 联调验证（见改造文档 §11.2）
 * 说明：本环境 Nacos 未启动（8848 无监听）→ 网关 81 无路由，故直连服务验证：
 *   blade-auth   http://localhost:8100   （token 端点为 POST /token）
 *   blade-system http://localhost:8106
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

const TEST_ACCOUNT = 'p1test1';
const TEST_PWD = 'P1@test123';

(async () => {
  const admin = await login('admin', 'ant.design');
  if (!admin.token) { console.log('0.admin LOGIN-FAIL', JSON.stringify(admin)); return; }
  console.log('0.admin LOGIN-OK');

  // 1. P1-5 部门树数据权限裁剪
  let r = await api('GET', '/dept/tree-scope', admin.token);
  const tree = r.body?.data;
  console.log('1.tree-scope =', r.status, ' 节点数 =', Array.isArray(tree) ? tree.length : 'N/A');

  // 取 admin 的组织/角色/岗位作为测试用户模板
  const adminDetail = await api('GET', '/user/detail?account=admin', admin.token);
  const a = adminDetail.body?.data || {};
  console.log('  admin deptId =', a.deptId, ' roleId =', a.roleId, ' postId =', a.postId);

  // 清理历史残留
  const old = await api('GET', '/user/detail?account=' + TEST_ACCOUNT, admin.token);
  if (old.body?.data?.id) {
    await api('POST', '/user/remove?ids=' + old.body.data.id, admin.token);
  }

  // 2. P1-2 密码 SM2 加密提交（模拟前端 Crypto.encryptPassword）
  const encPwd = sm2.doEncrypt(TEST_PWD, PUBLIC_KEY, 0);
  r = await api('POST', '/user/submit', admin.token, {
    tenantId: '000000', account: TEST_ACCOUNT, password: encPwd,
    name: 'P1测试昵称', realName: 'P1测试用户', personStatus: 1,
    deptId: a.deptId, roleId: a.roleId, postId: a.postId,
  });
  console.log('2.submit(SM2加密密码) =', r.status, r.body?.msg || r.body?.code);

  const detail = await api('GET', '/user/detail?account=' + TEST_ACCOUNT, admin.token);
  const uid = detail.body?.data?.id;
  console.log('  回读 id =', uid, ' personStatus =', detail.body?.data?.personStatus, ' workCode =', detail.body?.data?.workCode);

  // 3. 用明文密码登录新账号 —— 证明后端 SM2 解密 + Digest 存储与登录链路一致
  const t1 = await login(TEST_ACCOUNT, TEST_PWD);
  console.log('3.新账号明文登录 =', t1.status, t1.token ? 'LOGIN-OK' : 'LOGIN-FAIL', t1.msg || '');

  // 4. P1-3 默认密码配置：reset-password 后可用 blade.default-password(123456) 登录
  r = await api('POST', '/user/reset-password?userIds=' + uid, admin.token);
  console.log('4.reset-password =', r.status, r.body?.msg || r.body?.code);
  const t2 = await login(TEST_ACCOUNT, '123456');
  console.log('  重置后用默认密码 123456 登录 =', t2.status, t2.token ? 'LOGIN-OK' : 'LOGIN-FAIL', t2.msg || '');

  // 5. 清理
  r = await api('POST', '/user/remove?ids=' + uid, admin.token);
  console.log('5.清理测试用户 =', r.status);
})();
