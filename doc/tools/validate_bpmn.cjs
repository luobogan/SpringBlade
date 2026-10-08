/* BPMN 结构校验（skill 自带 validate_bpmn.py 的 JS 等价实现，本机无可用 python）
 * 用法：node validate_bpmn.cjs <file.bpmn20.xml>
 * 检查：注释双连字符、id 唯一、连线端点可解析、网关 default、UserTask 必带 wf:node、DI 完整性
 */
const fs = require('fs');

const file = process.argv[2];
if (!file) {
  console.log('USAGE: node validate_bpmn.cjs <file.bpmn20.xml>');
  process.exit(1);
}
const s = fs.readFileSync(file, 'utf8');
const errors = [];
const warns = [];

// 1. XML 注释不得含连续双连字符
const comments = s.match(/<!--[\s\S]*?-->/g) || [];
comments.forEach((c, i) => {
  if (c.slice(4, -3).includes('--')) errors.push(`注释 #${i + 1} 含连续双连字符 '--'`);
});

// 2. id 唯一
const ids = [...s.matchAll(/\sid="([^"]+)"/g)].map((m) => m[1]);
const dup = [...new Set(ids.filter((v, i) => ids.indexOf(v) !== i))];
if (dup.length) errors.push('重复 id: ' + dup.join(', '));

// 3. 连线端点可解析
const refs = [...s.matchAll(/(?:sourceRef|targetRef)="([^"]+)"/g)].map((m) => m[1]);
const unresolved = [...new Set(refs)].filter((r) => !ids.includes(r));
if (unresolved.length) errors.push('连线端点未定义: ' + unresolved.join(', '));

// 4. 网关 default 必须指向存在的 sequenceFlow
const flows = [...s.matchAll(/<sequenceFlow id="([^"]+)"/g)].map((m) => m[1]);
[...s.matchAll(/<exclusiveGateway id="([^"]+)"([^>]*)\/?>/g)].forEach((m) => {
  const def = /default="([^"]+)"/.exec(m[2]);
  if (!def) warns.push(`排他网关 ${m[1]} 未设置 default 出口`);
  else if (!flows.includes(def[1])) errors.push(`网关 ${m[1]} 的 default 指向不存在: ${def[1]}`);
});

// 5. 每个 UserTask 必须带 wf:node（springblade 发布门禁 nodeExtsComplete）
const tasks = [...s.matchAll(/<userTask id="([^"]+)"[^>]*>([\s\S]*?)<\/userTask>/g)];
if (tasks.length === 0) warns.push('未发现 userTask');
tasks.forEach((t) => {
  if (!/<wf:node\b/.test(t[2])) errors.push(`UserTask ${t[1]} 缺少 wf:node 扩展（发布会被拒绝）`);
});

// 6. DI 完整性
const shapes = [...s.matchAll(/<bpmndi:BPMNShape bpmnElement="([^"]+)"/g)].map((m) => m[1]);
const nodeEls = [...new Set([...ids].filter((i) => !flows.includes(i) && i !== 'BPMNDiagram_1' && i !== 'BPMNPlane_1'))];
const noShape = nodeEls.filter((i) => !shapes.includes(i));
if (noShape.length) warns.push('缺少 BPMNShape（不影响部署，画布不渲染）: ' + noShape.join(', '));
const edges = [...s.matchAll(/<bpmndi:BPMNEdge bpmnElement="([^"]+)"/g)].map((m) => m[1]);
const noEdge = flows.filter((f) => !edges.includes(f));
if (noEdge.length) warns.push('缺少 BPMNEdge: ' + noEdge.join(', '));

console.log('FILE    : ' + file);
console.log('process : ' + ((/<process id="([^"]+)"/.exec(s) || [])[1] || 'N/A'));
console.log('nodes   : ' + tasks.length + ' userTask, ' + flows.length + ' sequenceFlow');
console.log('ERRORS  : ' + (errors.length ? '\n  - ' + errors.join('\n  - ') : 'none'));
console.log('WARNINGS: ' + (warns.length ? '\n  - ' + warns.join('\n  - ') : 'none'));
process.exit(errors.length ? 1 : 0);
