-- ============================================================
-- 数据库迁移脚本：为 Category 和 Product 表添加 ImageFile 关联字段
-- 创建时间：2026-05-09
-- 说明：将图片字段从存储路径字符串改为关联 ImageFile 表的主键 ID
--        同时删除旧的路径字符串字段
-- ============================================================

-- ==================== 1. mall_category 表修改 ====================

-- 1.1 添加 image_id 列（分类图片ID，关联 ImageFile.imagefileid）
ALTER TABLE mall_category
ADD COLUMN image_id BIGINT NULL COMMENT '分类图片ID（关联ImageFile表主键）'
AFTER banner;

-- 1.2 添加 banner_id 列（分类Banner图片ID）
ALTER TABLE mall_category
ADD COLUMN banner_id BIGINT NULL COMMENT '分类Banner图片ID（关联ImageFile表主键）'
AFTER image_id;

-- 1.3 添加 icon_id 列（分类图标ID）
ALTER TABLE mall_category
ADD COLUMN icon_id BIGINT NULL COMMENT '分类图标ID（关联ImageFile表主键）'
AFTER banner_id;

-- 1.4 创建索引以提高查询性能
CREATE INDEX idx_mall_category_image_id ON mall_category(image_id);
CREATE INDEX idx_mall_category_banner_id ON mall_category(banner_id);
CREATE INDEX idx_mall_category_icon_id ON mall_category(icon_id);

-- 1.5 删除旧的路径字符串字段
ALTER TABLE mall_category DROP COLUMN IF EXISTS icon;
ALTER TABLE mall_category DROP COLUMN IF EXISTS image;
ALTER TABLE mall_category DROP COLUMN IF EXISTS banner;

-- ==================== 2. mall_product 表修改 ====================

-- 2.1 添加 main_image_id 列（商品主图ID）
ALTER TABLE mall_product
ADD COLUMN main_image_id BIGINT NULL COMMENT '商品主图ID（关联ImageFile表主键）'
AFTER brand_id;

-- 2.2 创建索引
CREATE INDEX idx_mall_product_main_image_id ON mall_product(main_image_id);

-- 2.3 删除旧的路径字符串字段
ALTER TABLE mall_product DROP COLUMN IF EXISTS main_image;


-- ============================================================
-- 数据迁移说明：
-- 如果已有数据需要迁移，请执行以下步骤：
--
-- 1. 备份现有数据：
--    CREATE TABLE mall_category_backup AS SELECT * FROM mall_category;
--    CREATE TABLE mall_product_backup AS SELECT * FROM mall_product;
--
-- 2. 迁移 Category 图片数据（可选）：
--    UPDATE mall_category c
--    INNER JOIN ImageFile f ON f.filerealpath LIKE CONCAT('%', SUBSTRING_INDEX(c.image, '/', -1), '%')
--    SET c.image_id = f.imagefileid,
--        c.banner_id = CASE WHEN f2.imagefileid IS NOT NULL THEN f2.imagefileid ELSE NULL END,
--        c.icon_id = CASE WHEN f3.imagefileid IS NOT NULL THEN f3.imagefileid ELSE NULL END
--    LEFT JOIN ImageFile f2 ON f2.filerealpath LIKE CONCAT('%', SUBSTRING_INDEX(c.banner, '/', -1), '%')
--    LEFT JOIN ImageFile f3 ON f3.filerealpath LIKE CONCAT('%', SUBSTRING_INDEX(c.icon, '/', -1), '%')
--    WHERE c.image IS NOT NULL AND c.image != '';
--
-- 3. 迁移 Product 主图数据（可选）：
--    UPDATE mall_product p
--    INNER JOIN ImageFile f ON f.filerealpath LIKE CONCAT('%', SUBSTRING_INDEX(p.main_image, '/', -1), '%')
--    SET p.main_image_id = f.imagefileid
--    WHERE p.main_image IS NOT NULL AND p.main_image != '';
--
-- 注意：迁移完成后，旧字段已被删除，所有图片信息通过 ImageFile 表管理。
-- ============================================================
