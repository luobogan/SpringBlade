-- V2026.11.12_002__wps_settings_single_source.sql
-- WPS 配置源收口：以「编辑器设置」页面 + doc_engine_setting 库表为唯一配置源，
-- blade-doc / blade-workflow 两个服务共用 WpsSettingsResolver 读取，页面保存即生效、无需重启。
-- 幂等，可重复执行。目标库: blade
--
-- 背景：此前 WPS 凭证散落在 ① doc_engine_setting 库表 ② blade-doc/application.yml 的
--   blade.doc.wps.* + blade.wps.* ③ blade-workflow/application-dev.yml 的 blade.wps.*，
--   改配置要动三处且极易漏；两个 yml 的明文段已删除，值全部落库。
--
-- ⚠️ 安全：app_secret / url_sign_secret 明文入库（用户 2026-11-12 明确确认接受），
--   这些值已在仓库历史与 blade-2026-11-04_164016-dump.sql 中泄露，仅作 dev 过渡；
--   P3.2 应在 WPS 控制台轮换并经本页面回填，切勿把轮换后的高熵密钥提交到仓库。

-- ① 凭证与脚本地址（原 yml 明文值，保证迁移后行为不变）
INSERT INTO doc_engine_setting (id, tenant_id, setting_key, setting_value, remark, status, is_deleted)
VALUES
  (2113499007801491458, '000000', 'wps_app_id',     'SX20261023ZYNJCZ',                  'WPS 开放平台应用 ID',        1, 0),
  (2113499007868600321, '000000', 'wps_app_secret', 'UquMoKZlDLGqbfnZUxNxFYMQglHvLIRY', 'WPS 开放平台应用密钥',       1, 0),
  (2113499007900000001, '000000', 'wps_url_sign_secret',        'doc-dev-secret-change-me', '内容/回调 URL 签名密钥（doc 与公文共用）', 1, 0),
  (2113499007900000002, '000000', 'wps_url_sign_ttl_seconds',   '300',                       'URL 签名有效期（秒）',        1, 0)
ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value), is_deleted = 0, status = 1;

-- ② 已有 4 项（default_engine / third_platform / wps_jssdk_url / wps_public_base）
--    由 V2026.11.12_001__doc_engine_setting_wps_restore.sql 写入，此处补齐幂等兜底
INSERT INTO doc_engine_setting (id, tenant_id, setting_key, setting_value, remark, status, is_deleted)
VALUES
  (2113497137162887170, '000000', 'default_engine',  'wps',                            '默认在线编辑方案 WPS Web',   1, 0),
  (2113499007738576898, '000000', 'third_platform',  'wps',                            '第三方编辑器平台',           1, 0),
  (2113499007952486402, '000000', 'wps_jssdk_url',   '/web-office-sdk-solution.umd.js','WebOffice umd.js（前端 public）', 1, 0),
  (2113499008000000100, '000000', 'wps_public_base', 'https://www.zwymding.top',      'WPS WebOffice 回调公网 base', 1, 0)
ON DUPLICATE KEY UPDATE is_deleted = 0, status = 1;

-- ③ 安全开关刻意不入表、不经页面暴露：由 WpsProperties 启动期默认值控制
--    （callback-auth-strict=true / callback-auth-bind-resource=false），
--    避免「页面关闭硬校验 + 网关已放行 /v3/3rd/**」组合成回调端点裸奔。
