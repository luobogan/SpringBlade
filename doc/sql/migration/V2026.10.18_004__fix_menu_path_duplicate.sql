-- ============================================================
-- V2026.10.18_004__fix_menu_path_duplicate.sql
-- 修复「表单建模 / 流程建模」两个顶级菜单 path 重复（均为 /formmode）
-- 导致前端 formatRoutes 生成的菜单 key 冲突：点击一个、另一个也跟着展开。
--
-- 处理：流程建模改用唯一 path /workflowmodeling；表单建模保持不变，
--       现有表单相关路由不受影响。
--
-- 执行后必须：
--   1) 重启 blade-system（菜单走 SYS_CACHE 缓存，不重启仍会下发旧 path）
--   2) 前端重新登录 / 清除 localStorage（/menu/routes 有前端缓存），否则拉到的还是旧菜单
-- ============================================================

-- 1) 流程建模改用唯一 path（父级 path 仅用于菜单 key + 点击展开，不单独导航，改它安全）
UPDATE blade_menu
   SET path = '/workflowmodeling'
 WHERE is_deleted = 0
   AND name = '流程建模'
   AND path = '/formmode';

-- 2) 若流程建模的子菜单 path 为相对值（依赖父 path 拼接），补成绝对路径，
--    避免改父 path 后子菜单路由失效。
--    绝对路径（如 /workflow/center）不受此句影响，已被 path NOT LIKE '/%' 过滤。
--    若②核查显示子菜单本就是绝对路径，可跳过此句。
UPDATE blade_menu
   SET path = CONCAT('/workflowmodeling/', path)
 WHERE is_deleted = 0
   AND parent_id = (
     SELECT id FROM (
       SELECT id FROM blade_menu
        WHERE is_deleted = 0 AND name = '流程建模' AND path = '/workflowmodeling'
     ) AS _t
   )
   AND path NOT LIKE '/%';
