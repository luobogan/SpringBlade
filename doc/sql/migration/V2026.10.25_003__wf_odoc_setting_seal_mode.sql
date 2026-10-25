-- -----------------------------------------------------------------------------
-- 迁移脚本：公文通用设置新增「电子签章方案」配置
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.25_003
-- 目标库    : blade（dev 实际库；生产拆库则为 blade_workflow）
-- 影响表    : wf_odoc_setting
-- 变更类型  : 改表（DDL，仅补列）
-- 是否幂等  : 是（information_schema 列存在性守卫 + PREPARE 动态 SQL）
-- 是否丢数据: 否（新增列均带默认值，存量行自动取默认值）
-- 回滚脚本  : ALTER TABLE `wf_odoc_setting`
--               DROP COLUMN `seal_mode`,
--               DROP COLUMN `seal_esign_org_id`,
--               DROP COLUMN `seal_esign_default_seal_id`,
--               DROP COLUMN `seal_esign_notify_url`;
-- 设计文档  : doc/esign/电子签章集成技术方案.md
--
-- 背景
--   决策 D2「印章两种方案并存，按配置启用」。原先用印方案只在 Nacos（odoc.esign.seal.mode）
--   配置，运维才能改，业务管理员在「公文通用设置」页看不到也改不了。现把「选哪种签章方案」
--   提升为业务可配项落到本表：
--     seal_mode                  0自研印章(本地) 1第三方电子签章(e签宝)
--     seal_esign_org_id          第三方机构ID（e签宝 orgId），为空取 Nacos odoc.esign.org-id
--     seal_esign_default_seal_id 第三方默认印章ID（e签宝 sealId），为空取 Nacos 默认值
--     seal_esign_notify_url      第三方回调地址，留空取 Nacos odoc.esign.notify-url
--
--   敏感项（appId / appSecret）不入库，仍只在 Nacos 维护，页面只读展示掩码。
-- =============================================================================

SET @t = 'wf_odoc_setting';

-- ① 签章方案
SET @h = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @t AND COLUMN_NAME = 'seal_mode');
SET @d = IF(@h = 0, CONCAT('ALTER TABLE ', @t, ' ADD COLUMN `seal_mode` INT NOT NULL DEFAULT 0 COMMENT ''签章方案 0自研印章(本地) 1第三方电子签章(e签宝)'''), 'SELECT 1');
PREPARE s FROM @d; EXECUTE s; DEALLOCATE PREPARE s;

-- ② 第三方机构ID
SET @h = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @t AND COLUMN_NAME = 'seal_esign_org_id');
SET @d = IF(@h = 0, CONCAT('ALTER TABLE ', @t, ' ADD COLUMN `seal_esign_org_id` VARCHAR(64) DEFAULT NULL COMMENT ''第三方签章机构ID(e签宝 orgId)，为空取Nacos'''), 'SELECT 1');
PREPARE s FROM @d; EXECUTE s; DEALLOCATE PREPARE s;

-- ③ 第三方默认印章ID
SET @h = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @t AND COLUMN_NAME = 'seal_esign_default_seal_id');
SET @d = IF(@h = 0, CONCAT('ALTER TABLE ', @t, ' ADD COLUMN `seal_esign_default_seal_id` VARCHAR(64) DEFAULT NULL COMMENT ''第三方默认印章ID(e签宝 sealId)，为空取Nacos'''), 'SELECT 1');
PREPARE s FROM @d; EXECUTE s; DEALLOCATE PREPARE s;

-- ④ 第三方回调地址
SET @h = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @t AND COLUMN_NAME = 'seal_esign_notify_url');
SET @d = IF(@h = 0, CONCAT('ALTER TABLE ', @t, ' ADD COLUMN `seal_esign_notify_url` VARCHAR(255) DEFAULT NULL COMMENT ''第三方签章回调地址，留空取Nacos'''), 'SELECT 1');
PREPARE s FROM @d; EXECUTE s; DEALLOCATE PREPARE s;
