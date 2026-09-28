-- ============================================================
-- 个人设置「按钮权限」(category=2)
-- 目的：用户头像下拉框里的「个人设置」选项按按钮权限显隐。
--       挂在 account_settings 页面菜单下；授权给 administrator + user。
--       撤销某角色的该按钮授权后，下拉框「个人设置」即自动隐藏。
-- 幂等：重复执行安全（NOT EXISTS 防护）。
-- ============================================================
USE blade;

SET @parent_id = (SELECT id FROM blade_menu WHERE code = 'account_settings' AND is_deleted = 0);
SET @btn_id    = (SELECT UUID_SHORT());

INSERT INTO blade_menu
  (id, parent_id, code, name, alias, path, source, category, action, is_open, remark, sort, is_deleted, tenant_id)
SELECT
  @btn_id,
  @parent_id,
  'account_setting',
  '个人设置',
  'account_setting',
  '',
  'setting',
  2,
  0,
  1,
  '用户下拉框-个人设置按钮权限',
  1,
  0,
  '000000'
WHERE NOT EXISTS (
  SELECT 1 FROM blade_menu WHERE code = 'account_setting' AND is_deleted = 0
);

-- 授权：超级管理员 + 普通用户（默认可见；撤销授权即隐藏下拉项）
INSERT INTO blade_role_menu (id, menu_id, role_id)
SELECT UUID_SHORT(),
       (SELECT id FROM blade_menu WHERE code = 'account_setting' AND is_deleted = 0),
       r.id
FROM blade_role r
WHERE r.role_alias IN ('administrator', 'user')
  AND r.is_deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM blade_role_menu rm
    WHERE rm.menu_id = (SELECT id FROM blade_menu WHERE code = 'account_setting' AND is_deleted = 0)
      AND rm.role_id = r.id
  );

-- 校验
SELECT m.id,
       m.parent_id,
       m.code,
       m.name,
       m.category,
       m.is_open,
       (SELECT COUNT(*) FROM blade_role_menu rm WHERE rm.menu_id = m.id) AS granted_roles
FROM blade_menu m
WHERE m.code = 'account_setting'
  AND m.is_deleted = 0;
