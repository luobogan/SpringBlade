-- =====================================================================
-- 集团树结构 regroup + 公司档案同步（2026-10-09，配合文档 §15.2 / §15.7）
-- ① 公司档案：blade_tenant.tenant_name 同步 E9 hrmcompany.companyname
--    （E9 现值 '维森集团'，按 §10.2 名称归一化规则 → '鼎泰集团'；仅租户 000000，勿动其他演示租户）
-- ② 组织树：根级分公司挂到「鼎泰集团股份有限公司」(id=1000000005, ecology_id=5) 下，
--    并用递归 CTE 全量重算该子树 ancestors（对齐 ecology supsubcomid 层级语义）。
--    注：ecology_id=34/35/36 三个分部在 E9 中本就嵌套（滨江→鼎泰地产、客户成功→鼎泰商务、客户体验中心→鼎泰集团），
--        其父不变，ancestors 随父链重算。
-- ⚠️ 若重跑 EcologyOrgMigration.java，须追加 --regroup-under-group 参数（工具已内置同逻辑），
--    否则 migrateSubcompanies 会把 parent_id 还原为 E9 源数据的平铺结构。
-- 幂等：重复执行结果一致（parent_id/ancestors/tenant_name 均为确定性赋值）。
-- =====================================================================

-- ① 公司档案同步
UPDATE blade_tenant SET tenant_name = '鼎泰集团' WHERE tenant_id = '000000';

-- ② 根级分部挂集团（9 行：ecology_id 6,7,8,9,10,11,12,13,17）
UPDATE blade_dept
   SET parent_id = 1000000005
 WHERE ecology_id IS NOT NULL AND dept_type = 1 AND parent_id = 0 AND id <> 1000000005;

-- ③ 递归重算集团子树 ancestors（集团节点自身='0'，其余=父链）
WITH RECURSIVE tree AS (
  SELECT id, CAST('0' AS CHAR(1000)) AS anc FROM blade_dept WHERE id = 1000000005
  UNION ALL
  SELECT c.id, CONCAT(p.anc, ',', p.id) FROM blade_dept c JOIN tree p ON c.parent_id = p.id
)
UPDATE blade_dept d JOIN tree t ON d.id = t.id SET d.ancestors = t.anc;

-- ===== 核对 =====
SELECT tenant_id, tenant_name FROM blade_tenant WHERE tenant_id = '000000';
SELECT id, ecology_id, dept_name, parent_id, ancestors FROM blade_dept WHERE dept_type = 1 ORDER BY id;
-- 抽查：鼎泰地产(1000000008)直接子节点的 ancestors 应为 0,1000000005,1000000008
SELECT id, dept_name, parent_id, ancestors FROM blade_dept WHERE parent_id = 1000000008 LIMIT 5;
-- 无孤儿（迁移节点 parent 必须存在）
SELECT COUNT(*) AS orphan_cnt FROM blade_dept a LEFT JOIN blade_dept b ON a.parent_id = b.id
 WHERE a.ecology_id IS NOT NULL AND a.parent_id <> 0 AND b.id IS NULL;
