/* P2 字段配置驱动 + 自定义字段 —— 联调验证（见改造文档 §13.5）
 * 直连服务：blade-auth 8100（token 端点 POST /token）、blade-system 8106
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

const ACCOUNT = 'p2test1';
const PWD = 'P2@test123';

(async () => {
  const admin = await login('admin', 'ant.design');
  if (!admin.token) { console.log('0.admin LOGIN-FAIL', JSON.stringify(admin)); return; }
  console.log('0.admin LOGIN-OK');

  // 1. 表单 schema
  let r = await api('GET', '/user/add-form-schema?tenantId=000000', admin.token);
  const groups = Array.isArray(r.body?.data) ? r.body.data : [];
  console.log('1.add-form-schema =', r.status, '分组数 =', groups.length);
  groups.forEach((g) =>
    console.log('   └', g.groupCode, g.groupName, '字段:', (g.fields || []).map((f) => `${f.propName}/${f.label}/${f.eleType}${f.required === 1 ? '/必填' : ''}`).join(', ')),
  );
  const field = groups.flatMap((g) => g.fields || [])[0];

  // 2. 建档带 extData
  const old = await api('GET', '/user/detail?account=' + ACCOUNT, admin.token);
  if (old.body?.data?.id) await api('POST', '/user/remove?ids=' + old.body.data.id, admin.token);
  const adminDetail = await api('GET', '/user/detail?account=admin', admin.token);
  const a = adminDetail.body?.data || {};
  const extVal = '总部A座';
  r = await api('POST', '/user/submit', admin.token, {
    tenantId: '000000', account: ACCOUNT, password: sm2.doEncrypt(PWD, PUBLIC_KEY, 0),
    name: 'P2测试昵称', realName: 'P2测试用户', personStatus: 1,
    deptId: a.deptId, roleId: a.roleId, postId: a.postId,
    extData: field ? { [field.fieldId]: extVal } : {},
  });
  console.log('2.submit(带extData) =', r.status, r.body?.msg || r.body?.code);

  const detail = await api('GET', '/user/detail?account=' + ACCOUNT, admin.token);
  const uid = detail.body?.data?.id;
  r = await api('GET', '/user/ext-data?userId=' + uid, admin.token);
  console.log('3.ext-data 回读 =', r.status, JSON.stringify(r.body?.data || {}), '期望值 =', extVal);

  // 4. 停用字段后 schema 应消失（改配置即改表单）——仅打印当前配置，实际停用需改库
  console.log('4.（停用验证需改 blade_hrm_field.status=0 后重跑第 1 步，此处跳过）');

  // 5. 清理
  r = await api('POST', '/user/remove?ids=' + uid, admin.token);
  console.log('5.清理测试用户 =', r.status);
})();
