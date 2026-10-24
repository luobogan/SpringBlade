-- =============================================================================
-- 迁移脚本：对象存储菜单备注补充「默认本地存储 / 启用第三方 OSS 才切换」说明
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.24_004
-- 目标库    : blade
-- 影响表    : blade_menu（仅更新 code='resource_oss' 的 remark）
-- 变更类型  : DML
-- 是否幂等  : 是（重复执行结果一致）
-- 是否丢数据: 否
--
-- 说明
--   路径 /resource/oss 默认指向「本地存储」（category='local'）；未显式启用任何
--   第三方 OSS 配置时，该路径下的上传与文件读写一律走本地存储。
--   仅当在页面明确「启用」（status=1）了一条第三方 OSS 配置（如七牛云 category='qiniu'）时，
--   才切换为该配置所对应的第三方 OSS 存储；同一租户仅一条配置生效，
--   停用或删除该第三方配置后，自动回退为默认的本地存储。
-- =============================================================================

USE `blade`;

UPDATE `blade_menu`
SET `remark` = '对象存储管理页（菜单驱动路由；组件 ./pages/Resource/Oss/index.tsx）：配置 CRUD + 启用切换。默认指向本地存储（category=local）；仅当明确启用(status=1)第三方 OSS 配置（如七牛云 category=qiniu）时，才切换为该第三方 OSS 存储，停用/删除后回退本地存储'
WHERE `code` = 'resource_oss' AND `is_deleted` = 0;

-- 校验输出
SELECT `id`, `code`, `path`, `remark`
FROM `blade_menu`
WHERE `code` = 'resource_oss' AND `is_deleted` = 0;
