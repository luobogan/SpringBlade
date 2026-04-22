package org.springblade.mall.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 分类数据初始化器
 * 用于初始化分类和品牌数据
 */
@Component
@Order(0)
@Profile({"dev", "test"})
public class CategoryDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CategoryDataInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public CategoryDataInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("========================================");
        log.info("开始初始化分类和品牌数据...");
        log.info("========================================");

        try {
            // 检查分类表是否为空
            Integer categoryCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM mall_category", Integer.class
            );

            if (categoryCount == null || categoryCount == 0) {
                log.info("分类表为空，开始初始化分类数据...");
                initCategoryData();
                initBrandData();
                log.info("分类和品牌数据初始化完成！");
            } else {
                log.info("分类表已有 {} 条数据，跳过初始化", categoryCount);
            }

        } catch (Exception e) {
            log.error("分类数据初始化失败！", e);
            throw new RuntimeException("分类数据初始化失败", e);
        }
    }

    /**
     * 初始化分类数据
     */
    private void initCategoryData() {
        String[] categoryData = {
                "('数码电子', '数码电子分类', 0, 1, 'phone', 1, 1, 'http://example.com/icon1.png', 'http://example.com/image1.png')",
                "('时尚服饰', '时尚服饰分类', 0, 1, 'clothes', 2, 1, 'http://example.com/icon2.png', 'http://example.com/image2.png')",
                "('家居生活', '家居生活分类', 0, 1, 'home', 3, 1, 'http://example.com/icon3.png', 'http://example.com/image3.png')",
                "('电脑办公', '电脑办公分类', 0, 1, 'computer', 4, 1, 'http://example.com/icon4.png', 'http://example.com/image4.png')"
        };

        String insertSql = "INSERT INTO mall_category (name, description, parent_id, level, icon, sort, status, image, banner) VALUES " +
                String.join(", ", categoryData);

        jdbcTemplate.execute(insertSql);
        log.info("成功插入 {} 条分类数据", categoryData.length);
    }

    /**
     * 初始化品牌数据
     */
    private void initBrandData() {
        String[] brandData = {
                "('Apple', 'https://picsum.photos/200/200?random=10', 'https://www.apple.com', '苹果公司', '苹果品牌故事', 1, 1, 1)",
                "('Samsung', 'https://picsum.photos/200/200?random=11', 'https://www.samsung.com', '三星电子', '三星品牌故事', 2, 1, 1)",
                "('Xiaomi', 'https://picsum.photos/200/200?random=12', 'https://www.mi.com', '小米科技', '小米品牌故事', 3, 1, 1)",
                "('Huawei', 'https://picsum.photos/200/200?random=13', 'https://www.huawei.com', '华为技术', '华为品牌故事', 4, 1, 1)"
        };

        String insertSql = "INSERT INTO mall_brand (name, logo, website, description, story, sort, is_recommend, status) VALUES " +
                String.join(", ", brandData);

        jdbcTemplate.execute(insertSql);
        log.info("成功插入 {} 条品牌数据", brandData.length);
    }
}



