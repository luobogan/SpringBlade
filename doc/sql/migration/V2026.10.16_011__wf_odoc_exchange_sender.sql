-- -----------------------------------------------------------------------------
-- 迁移脚本：内部交换记录增加显式属主 sender_id
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.16_011
-- 目标库    : blade
-- 影响表    : wf_odoc_exchange_inner（加列 + 索引）
-- 变更类型  : DDL（ALTER）
-- 回滚      : ALTER TABLE `wf_odoc_exchange_inner` DROP INDEX `idx_odoc_exc_inner_sender`, DROP COLUMN `sender_id`;
--
-- 背景
--   E2E 发现：dev 环境 TenantEntity.create_user 自动填充为 -1（未取到登录用户），
--   导致「我发出的交换」按 create_user 过滤恒为空。改用发送时显式写入的 sender_id 归属。

ALTER TABLE `wf_odoc_exchange_inner`
    ADD COLUMN `sender_id` BIGINT DEFAULT NULL COMMENT '发送人ID(发出视角归属)';

ALTER TABLE `wf_odoc_exchange_inner`
    ADD INDEX `idx_odoc_exc_inner_sender` (`sender_id`, `direction`, `is_deleted`);
