-- V2026.11.12_001__doc_engine_menu_rebuild.sql
-- 重建 DocEngine（内容引擎 / 知识管理）菜单记录。
-- 背景：DocEngine 菜单记录（blade_menu 的 docengine_* 行）此前从未被任何脚本创建，
--       仅由开发期手动入库，导致 dev 库重置/重编号后整组菜单消失；而 V2026.10.22_006/009/010/012/013
--       的 blade_role_menu 授权仍指向这些不存在的 menu_id（悬空）。本脚本补齐菜单记录，
--       并新建「内容引擎」父菜单 + 给父菜单补角色授权，使菜单树完整显示。
-- 幂等：INSERT IGNORE 防重复执行（PK 冲突即跳过）。
-- 目标库: blade

-- ① 父菜单：内容引擎
INSERT IGNORE INTO blade_menu
  (id, parent_id, code, name, alias, path, source, sort, category, action, is_open, is_component, component, component_type, status, is_deleted, tenant_id)
VALUES
  (2113999900000000001, 0, 'docengine', '内容引擎', '内容引擎', '/docengine', 'file-text', 100, 1, 0, 1, 0, NULL, 'bundled', 1, 0, '000000');

-- ② 子菜单（复用原 menu_id，使既有 blade_role_menu 悬空授权自动生效）
INSERT IGNORE INTO blade_menu
  (id, parent_id, code, name, alias, path, source, sort, category, action, is_open, is_component, component, component_type, status, is_deleted, tenant_id)
VALUES
  (2113246706520207361, 2113999900000000001, 'docengine_search',        '文档检索', 'docengine_search',        '/docengine/search',        'file-text', 1, 1, 0, 1, 0, NULL, 'bundled', 1, 0, '000000'),
  (2113246706654425089, 2113999900000000001, 'docengine_mould',         '模板管理', 'docengine_mould',         '/docengine/mould',         'file-text', 2, 1, 0, 1, 0, NULL, 'bundled', 1, 0, '000000'),
  (2113246706784448514, 2113999900000000001, 'docengine_recycle',       '回收站',   'docengine_recycle',       '/docengine/recycle',       'file-text', 3, 1, 0, 1, 0, NULL, 'bundled', 1, 0, '000000'),
  (2113246706851557378, 2113999900000000001, 'docengine_editor',        '在线编辑', 'docengine_editor',        '/docengine/editor',        'file-text', 4, 1, 0, 1, 0, NULL, 'bundled', 1, 0, '000000'),
  (2113288204980760577, 2113999900000000001, 'docengine_categoryconfig','目录配置', 'docengine_categoryconfig','/docengine/categoryconfig','file-text', 5, 1, 0, 1, 0, NULL, 'bundled', 1, 0, '000000'),
  (2113299654440824834, 2113999900000000001, 'docengine_detail',        '文档详情', 'docengine_detail',        '/docengine/detail',        'file-text', 6, 2, 0, 1, 1, NULL, 'bundled', 1, 0, '000000'), -- category=2/isComponent=1：组件型，由列表页跳转进入
  (2113308963321065473, 2113999900000000001, 'docengine_stat',          '文档统计', 'docengine_stat',          '/docengine/stat',          'file-text', 7, 1, 0, 1, 0, NULL, 'bundled', 1, 0, '000000'),
  (2113424331842695169, 2113999900000000001, 'docengine_center',        '知识中心', 'docengine_center',        '/docengine/center',        'file-text', 8, 1, 0, 1, 0, NULL, 'bundled', 1, 0, '000000');

-- ③ 父菜单角色授权（子菜单授权已由 V2026.10.22_006/009/010/012/013 存在，仅补父）
INSERT IGNORE INTO blade_role_menu (id, menu_id, role_id)
VALUES (2113999900000000001, 2113999900000000001, 4000000002);

-- ⚠️ 前端注意（非本 SQL 范围）：ant-design-pro/src/app.tsx 的菜单路由纯靠 path 推导 module，
--    toPascalCase('docengine') = 'Docengine'，而磁盘目录是 'DocEngine'（内部大写）。
--    Windows(NTFS 大小写不敏感) dev 环境可裸跑；Linux 生产构建会解析失败(404)。
--    生产部署前需在 app.tsx 的 PAGE_DIR_ALIAS / commonWords 补 docengine→DocEngine 映射。
