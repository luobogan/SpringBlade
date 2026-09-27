-- =============================================================================
-- 迁移脚本 V2026.09.27_001：表单管理列表补「导入」按钮权限
-- -----------------------------------------------------------------------------
-- 版本      : V2026.09.27_001
-- 目标库    : blade（blade_menu / blade_role_menu 所在库）
-- 影响表    : blade_menu、blade_role_menu
-- 变更类型  : 数据（按钮菜单 + 角色授权）
-- 是否幂等  : 是（已存在则跳过，不报错）
-- 是否丢数据: 否
-- 回滚脚本  :
--   DELETE FROM blade_role_menu WHERE menu_id = 2061825613800460322;
--   DELETE FROM blade_menu WHERE id = 2061825613800460322;
--
-- 背景
--   前端 FormManage（/formmode/formmanage）新增「导入」按钮（formmanage_import），
--   用于从同机的泛微 ecology 库勾选表单并导入为 blade 表单。
--   按钮权限读取自 blade_menu，需先建菜单项并对齐 formmanage_aae 的授权角色。
-- =============================================================================

USE `blade`;

SET @v_parent = (SELECT id FROM blade_menu WHERE code = 'formmanage' AND is_deleted = 0 LIMIT 1);
SET @v_aae    = (SELECT id FROM blade_menu WHERE code = 'formmanage_aae' AND is_deleted = 0 LIMIT 1);
SET @v_menu   = 2061825613800460322;

-- 1) 按钮菜单 ----------------------------------------------------------------
INSERT INTO blade_menu (id, parent_id, code, name, alias, path, source, sort,
                        category, action, is_open, is_component, component_type,
                        is_deleted, tenant_id)
SELECT @v_menu, @v_parent, 'formmanage_import', '导入', 'import', '', 'api', 3,
       2, 0, 0, 0, 'bundled', 0, '000000'
FROM DUAL
WHERE @v_parent IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM blade_menu WHERE code = 'formmanage_import' AND is_deleted = 0);

-- 2) 角色授权（与 formmanage_aae 授权角色一致）---------------------------------
-- 计数基数取当前 MAX(id)，避免重跑时与已插入的行（不同角色但 id 相同）撞主键
SET @v_rm = (SELECT COALESCE(MAX(id), 2099465505489362966) FROM blade_role_menu);

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
WHERE m.code IN ('formmanage', 'formmanage_aae', 'formmanage_import', 'formmanage_delete')
  AND m.is_deleted = 0;
