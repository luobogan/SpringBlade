-- 修复门户相关按钮(category=2) parent_id 为 NULL 导致 /menu/buttons 500 的问题
--
-- 现象：GET /api/blade-system/menu/buttons 报 500
--   Cannot invoke "java.lang.Long.longValue()" because the return value of
--   "org.springblade.core.tool.node.INode.getParentId()" is null
-- 原因：ForestNodeManager 建树时对每个节点取 parentId.longValue()，
--   而下列按钮行 parent_id 为 NULL（早期门户迁移 SQL 漏填该列）。
-- 修法：按本项目既有约定（门户下所有按钮统一挂在「门户」根菜单 102115590570770452 下，
--   参见 portal_theme_manage / portal_page_manage 等）补齐 parent_id。

UPDATE blade_menu SET parent_id = 102115590570770452
WHERE is_deleted = 0 AND category = 2
  AND (parent_id IS NULL OR parent_id = '')
  AND id IN (
    102115590570770432, -- portal_setting_manage            门户应用设置按钮
    102115590570770433, -- portal_setting_maint             门户应用设置维护权限
    102115590570770436, -- portal_ts_portal_manage          特色门户管理按钮
    102115590570770437, -- portal_ts_portal_maint           特色门户维护权限
    102115590570770440, -- portal_config_manage             门户配置项管理按钮
    102115590570770441, -- portal_config_maint              门户配置项维护权限
    102115590570770444, -- portal_element_register_manage   元素注册管理按钮
    102115590570770445, -- portal_element_register_maint    元素注册维护权限
    102115590570770448, -- portal_synergy_manage            协同区管理按钮
    102115590570770449  -- portal_synergy_maint             协同区维护权限
  );
