-- V2026.10.22_004__nacos_blade_doc_config.sql
-- 内容引擎 blade-doc 微服务 · Nacos 配置中心接入（部署）
-- 目标库: nacos（config_info 表）。作用：让 blade-doc 注册/取数，且网关 /blade-doc/** 可达。
--
-- ============ ① blade-doc-dev.yaml（数据源 + blade.doc）============
-- 方式A（推荐·最稳）：Nacos 控制台 → 配置管理 → 配置列表 → 新增
--     data_id = blade-doc-dev.yaml, group = DEFAULT_GROUP, 格式 = YAML，内容见下方 @content。
--     控制台自动计算 md5。
-- 方式B（备选）：执行下方 INSERT...SELECT...WHERE NOT EXISTS（幂等，md5=MD5(content)）。
-- 数据源复用 ${blade.datasource.dev.*}（与 blade-workflow 同源，指向 blade 库）；doc_* 表已由
-- blade_doc_* 迁移脚本建好。

USE `nacos`;

SET @content = 'server:
  port: 8120
spring:
  datasource:
    url: ${blade.datasource.dev.url}
    username: ${blade.datasource.dev.username}
    password: ${blade.datasource.dev.password}
blade:
  doc:
    max-upload-size: 5
    url-sign-secret: doc-dev-secret-change-me
    url-sign-ttl-seconds: 300
    field-encrypt-key: doc-field-enc-dev-key
';

INSERT INTO config_info (data_id, group_id, content, md5)
SELECT 'blade-doc-dev.yaml', 'DEFAULT_GROUP', @content, MD5(@content)
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM config_info
  WHERE data_id = 'blade-doc-dev.yaml' AND group_id = 'DEFAULT_GROUP'
);

-- ============ ② 网关路由（必须·控制台编辑 blade.yaml，勿整文件覆盖）============
-- Nacos 控制台 → 配置管理 → 编辑 blade.yaml（DEFAULT_GROUP）→ 在 spring.cloud.gateway.routes
-- 列表【追加】以下一条（与既有 blade-workflow 同级缩进）：
--
--       - id: blade-doc
--         uri: lb://blade-doc
--         predicates:
--           - Path=/blade-doc/**
--         filters:
--           - StripPrefix=1
--
-- StripPrefix=1 剥掉 /blade-doc 前缀，命中 blade-doc 控制器的 /doc/** 资源路径。
-- 切勿用 SQL 整列覆盖 blade.yaml（running 配置含大量路由/数据源，覆盖会中断全站路由）。
