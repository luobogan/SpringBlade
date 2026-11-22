-- =============================================================================
-- 迁移脚本 V2026.11.19_001：新增「表单数据」通用菜单（方案 A）
-- -----------------------------------------------------------------------------
-- 版本      : V2026.11.19_001
-- 目标库    : blade（blade_menu / blade_role_menu 所在库）
-- 影响表    : blade_menu、blade_role_menu
-- 变更类型  : 数据（普通菜单 + 角色授权）
-- 是否幂等  : 是（已存在则跳过）
-- 是否丢数据: 否
--
-- 背景
--   表单建模创建表单时不生成 blade_menu，用户配完权限后没有「点菜单看表单数据」的通路。
--   现有唯一数据列表页 /formmode/formview?formId=xxx 是表单管理页「查看」按钮跳转，
--   普通用户无 formmanage 菜单权限则无法到达。
--   本脚本新增一条普通菜单「表单数据」，前端 FormView.tsx 在无 formId 时展示表单选择器，
--   用户选中表单后带 formId 跳转数据列表。一张菜单服务所有表单。
--
-- 回滚脚本  :
--   DELETE FROM blade_role_menu WHERE menu_id = 2113470000000000003;
--   DELETE FROM blade_menu WHERE id = 2113470000000000003;
-- =============================================================================

USE `blade`;

SET @v_parent = (SELECT id FROM blade_menu WHERE code = 'formmode_root' AND is_deleted = 0 LIMIT 1);
-- 兼容：不同环境父菜单 code 可能是 'mode'
SET @v_parent = COALESCE(@v_parent, (SELECT id FROM blade_menu WHERE code = 'mode' AND path = '/formmode' AND is_deleted = 0 LIMIT 1));
SET @v_manage = (SELECT id FROM blade_menu WHERE code = 'formmanage' AND is_deleted = 0 LIMIT 1);
SET @v_menu   = 2113470000000000003;

-- 1) 普通菜单：表单数据 -------------------------------------------------------
INSERT INTO blade_menu (id, parent_id, code, name, alias, path, source, sort,
                        category, action, is_open, is_component, component_type,
                        is_deleted, tenant_id)
SELECT @v_menu, @v_parent, 'formdata_view', '表单数据', 'formdata', '/formmode/formview', 'api', 5,
       1, 0, 1, 0, 'bundled', 0, '000000'
FROM DUAL
WHERE @v_parent IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM blade_menu WHERE code = 'formdata_view' AND is_deleted = 0);

-- 2) 角色授权：与「表单列表」(formmanage) 角色一致 -----------------------------
SET @v_rm = (SELECT COALESCE(MAX(id), 2113470000000000005) FROM blade_role_menu);

INSERT INTO blade_role_menu (id, role_id, menu_id)
SELECT (@v_rm := @v_rm + 1), r.role_id, @v_menu
FROM blade_role_menu r
WHERE r.menu_id = @v_manage
  AND @v_manage IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM blade_role_menu x WHERE x.role_id = r.role_id AND x.menu_id = @v_menu);
