-- =============================================================================
-- 迁移脚本 V2026.09.18_002：表单管理列表补「删除」按钮权限
-- -----------------------------------------------------------------------------
-- 版本      : V2026.09.18_002
-- 目标库    : blade（blade_menu / blade_role_menu 所在库）
-- 影响表    : blade_menu、blade_role_menu
-- 变更类型  : 数据（按钮菜单 + 角色授权）
-- 是否幂等  : 是（已存在则跳过，不报错）
-- 是否丢数据: 否
-- 回滚脚本  :
--   DELETE FROM blade_role_menu WHERE menu_id = 2061825613800460311;
--   DELETE FROM blade_menu WHERE id = 2061825613800460311;
--
-- 背景
--   前端 FormManage（/formmode/formmanage）行操作「删除」按钮由
--   usePageButtons() 读取的按钮权限 formmanage_delete 控制；
--   此前库中该表单下只有 formmanage_aae（新增和修改）与 exceldesign，
--   删除按钮一直不显示。
--
--   删除链路（本次一并实现）：
--     DELETE /api/blade-formmode/form-definition/{id}
--       → 先经 Feign 调 blade-workflow 校验表单是否被流程设计绑定
--         （wf_process_definition.form_id）或已产生流程实例（wf_instance.form_id）；
--         被占用则拒绝删除并提示绑定的流程名称；
--       → 未被占用才级联清理：动态主表/明细表/历史表、字段定义、表单布局、
--         字段扩展属性、字段选项，最后物理删除表单定义。
--
-- 说明
--   * 按钮 path 留空：不生成前端路由（app.tsx 对无 path 的菜单直接跳过），
--     仅作为按钮权限经 /menu/buttons 下发。
--   * 授权对象与「新增和修改」(formmanage_aae) 完全一致，避免出现
--     「有编辑无删除」的角色差异。
-- =============================================================================

USE `blade`;

SET @v_parent = (SELECT id FROM blade_menu WHERE code = 'formmanage' AND is_deleted = 0 LIMIT 1);
SET @v_aae    = (SELECT id FROM blade_menu WHERE code = 'formmanage_aae' AND is_deleted = 0 LIMIT 1);
SET @v_menu   = 2061825613800460311;

-- 1) 按钮菜单 ----------------------------------------------------------------
INSERT INTO blade_menu (id, parent_id, code, name, alias, path, source, sort,
                        category, action, is_open, is_component, component_type,
                        is_deleted, tenant_id)
SELECT @v_menu, @v_parent, 'formmanage_delete', '删除', 'delete', '', 'api', 2,
       2, 0, 0, 0, 'bundled', 0, '000000'
FROM DUAL
WHERE @v_parent IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM blade_menu WHERE code = 'formmanage_delete' AND is_deleted = 0);

-- 2) 角色授权（与 formmanage_aae 授权角色一致）---------------------------------
SET @v_rm = 2099465505489362966;

INSERT INTO blade_role_menu (id, role_id, menu_id)
SELECT @v_rm := @v_rm + 1, r.role_id, @v_menu
FROM blade_role_menu r
WHERE r.menu_id = @v_aae
  AND @v_aae IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM blade_role_menu x WHERE x.role_id = r.role_id AND x.menu_id = @v_menu);

-- 3) 校验输出 ----------------------------------------------------------------
SELECT m.id, m.parent_id, m.code, m.name, m.category, m.is_component,
       (SELECT COUNT(*) FROM blade_role_menu rm WHERE rm.menu_id = m.id) AS granted_roles
FROM blade_menu m
WHERE m.code IN ('formmanage', 'formmanage_aae', 'formmanage_delete')
  AND m.is_deleted = 0;
