-- 用户主题设置列：存储前端「主题设置」抽屉保存的布局主题配置（navTheme / colorPrimary / layout 等 JSON 字符串）
-- 项目未启用 Flyway，请在对应数据库手动执行。
ALTER TABLE `blade_user`
    ADD COLUMN `theme_setting` varchar(2000) NULL DEFAULT NULL COMMENT '主题设置(JSON)';
