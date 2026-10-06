-- 消息中心 后端菜单（让前端侧边栏出现"消息中心"）
-- 说明：
--   * 顶层菜单 parent_id=0，path=/message 由前端 formatRoutes 推导组件 ./Message（src/pages/Message）
--   * source 即图标名（ant-design-vue 图标，去掉 Outlined 后缀），此处用 message（MessageOutlined）
--   * component 置空，由前端按 path 推导；component_type=bundled
--   * 需同时写入 blade_role_menu 授权给超级管理员(administrator) 与普通用户(user) 角色，侧边栏才会展示
-- 菜单ID固定值仅用于本地种子；如与其它环境主键冲突，请改用各环境雪花ID后执行。

INSERT INTO blade_menu (id, parent_id, code, name, alias, path, source, sort, category, action, is_open, is_component, component, component_type, remark, status, is_deleted, tenant_id)
VALUES (2107999000000000001, 0, 'message', '消息中心', 'message', '/message', 'message', 11, 1, 0, 1, 0, NULL, 'bundled', '企业统一消息中心', 1, 0, '000000');

-- 授权给超级管理员角色(administrator)
INSERT INTO blade_role_menu (id, menu_id, role_id)
VALUES (2107999000000000002, 2107999000000000001, 1123598816738675201);

-- 授权给普通用户角色(user)
INSERT INTO blade_role_menu (id, menu_id, role_id)
VALUES (2107999000000000003, 2107999000000000001, 1123598816738675202);
