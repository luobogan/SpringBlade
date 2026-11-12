-- V2026.11.12_001__doc_engine_setting_wps_restore.sql
-- WPS 统一改造 P1-1.6：恢复知识管理 WPS Web 编辑器设置（当前 dev 库 doc_engine_setting 为 0 行）。
-- 目标库: blade（blade-doc 业务库）。幂等，可重复执行。
--
-- 历史依据：doc/sql/blade/blade-2026-11-04_164016-dump.sql:5064 原有 6 行配置，
--   default_engine=wps / third_platform=wps / wps_app_id / wps_app_secret /
--   wps_jssdk_url / wps_public_base=https://www.zwymding.top
--
-- ⚠️ 安全：app_id / app_secret **不在本脚本明文落库**。
--   P1 过渡期由 application.yml 的 ${WPS_APP_ID}/${WPS_APP_SECRET} 环境变量提供
--   （WpsDocOnlineEditor.getEffectiveWps 在库表缺项时回退 yml），P3.2 统一收口到
--   Nacos blade.wps.* 并轮换密钥。历史 dump 中的明文密钥应视为已泄露处理。

-- ① 默认引擎切 WPS Web（Univer 仍可经 editor=univer 显式选用）
INSERT INTO doc_engine_setting (id, tenant_id, setting_key, setting_value, remark, status, is_deleted)
VALUES (2113497137162887170, '000000', 'default_engine', 'wps', '默认在线编辑方案 WPS Web', 1, 0)
ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value), is_deleted = 0;

-- ② 第三方平台标记（isEnabled 判据；库表无此项时按 yml WPS 开关判断）
INSERT INTO doc_engine_setting (id, tenant_id, setting_key, setting_value, remark, status, is_deleted)
VALUES (2113499007738576898, '000000', 'third_platform', 'wps', '第三方编辑器平台', 1, 0)
ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value), is_deleted = 0;

-- ③ WebOffice SDK 脚本地址（前端 public 目录已部署 web-office-sdk-solution.umd.js）
INSERT INTO doc_engine_setting (id, tenant_id, setting_key, setting_value, remark, status, is_deleted)
VALUES (2113499007952486402, '000000', 'wps_jssdk_url', '/web-office-sdk-solution.umd.js', 'WPS WebOffice umd.js', 1, 0)
ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value), is_deleted = 0;

-- ④ 回调公网 base（WPS 中台按控制台登记地址回调，download/upload 绝对 URL 依赖此项）
INSERT INTO doc_engine_setting (id, tenant_id, setting_key, setting_value, remark, status, is_deleted)
VALUES (2113499008000000100, '000000', 'wps_public_base', 'https://www.zwymding.top', 'WPS WebOffice 回调公网 base', 1, 0)
ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value), is_deleted = 0;

-- ⑤⑥ wps_app_id / wps_app_secret 不落脚本：由环境变量或 Nacos 注入，见文件头说明。
--     若确需库表覆盖，执行前先 SET @wps_app_id / @wps_app_secret，且禁止提交明文到仓库。
