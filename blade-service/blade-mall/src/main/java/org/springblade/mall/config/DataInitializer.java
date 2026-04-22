package org.springblade.mall.config;

import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.mall.entity.Coupon;
import org.springblade.mall.entity.Product;
import org.springblade.mall.mapper.CouponMapper;
import org.springblade.mall.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.*;

/**
 * 数据初始化器
 * 用于在应用启动时初始化演示商品数据
 *
 * 功能说明：
 * 1. 实现 CommandLineRunner 接口，在应用启动时自动执行
 * 2. 使用 @Order(1) 确保在其他初始化之前执行
 * 3. 使用 @Profile({"dev", "test"}) 只在开发和测试环境执行
 * 4. 创建 50-100 条多样化的商品数据
 * 5. 使用事务确保数据一致性
 * 6. 检查数据是否已存在，避免重复插入
 *
 * @author YoupinMall
 * @since 2026-01-28
 */
@Component
@RequiredArgsConstructor
@Order(1)
@Profile({"dev", "test"})
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final ProductMapper productMapper;
    private final CouponMapper couponMapper;

    @Value("${app.data.init.enabled:true}")
    private boolean initDataEnabled;

    // 商品分类ID（假设已存在）
    private static final Long CATEGORY_ELECTRONICS = 5L;   // 数码电子
    private static final Long CATEGORY_FASHION = 8L;       // 时尚服饰
    private static final Long CATEGORY_HOME = 10L;           // 家居生活
    private static final Long CATEGORY_COMPUTER = 6L;       // 电脑办公
    private static final Long CATEGORY_AUTO_PARTS = 35L;     // 汽车配件

    // 品牌ID（假设已存在）
    private static final Long BRAND_APPLE = 1L;
    private static final Long BRAND_SAMSUNG = 2L;
    private static final Long BRAND_NIKE = 3L;
    private static final Long BRAND_ADIDAS = 4L;
    private static final Long BRAND_SONY = 5L;
    private static final Long BRAND_XIAOMI = 6L;
    private static final Long BRAND_HUAWEI = 7L;
    private static final Long BRAND_LOGITECH = 8L;

    /**
     * 执行数据初始化
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(String... args) {
        log.info("========================================");
        log.info("开始初始化演示商品数据...");
        log.info("========================================");

        if (!initDataEnabled) {
            log.info("数据初始化功能已禁用，跳过初始化");
            return;
        }

        // 获取当前租户ID
        String currentTenantId = SecureUtil.getTenantId();
        if (currentTenantId == null) {
            log.warn("未找到当前租户ID，跳过数据初始化");
            return;
        }
        log.info("当前租户ID: {}", currentTenantId);

        try {
            // 检查是否已有数据
            long existingCount = productMapper.selectCount(null);
            if (existingCount > 0) {
                log.warn("数据库中已存在 {} 条商品数据，跳过初始化", existingCount);
                log.info("如需重新初始化，请清空商品表数据");
                return;
            }

            // 生成并插入商品数据
            List<Product> products = generateProducts(currentTenantId);
            int batchSize = 50;
            int totalInserted = 0;

            for (int i = 0; i < products.size(); i += batchSize) {
                int end = Math.min(i + batchSize, products.size());
                List<Product> batch = products.subList(i, end);
                for (Product product : batch) {
                    productMapper.insert(product);
                    totalInserted++;
                }
                log.info("已插入 {}/{} 条商品数据", totalInserted, products.size());
            }

            log.info("========================================");
            log.info("商品数据初始化完成！共插入 {} 条数据", totalInserted);
            log.info("========================================");

            // 初始化优惠券数据
            initCoupons(currentTenantId);

        } catch (Exception e) {
            log.error("商品数据初始化失败！", e);
            throw new RuntimeException("商品数据初始化失败", e);
        }
    }

    /**
     * 生成商品数据列表
     */
    private List<Product> generateProducts(String tenantId) {
        List<Product> products = new ArrayList<>();
        Random random = new Random(42); // 使用固定种子保证可重复性

        // 数码电子类商品 (25个)
        products.addAll(generateElectronicsProducts(random, 1, 25, tenantId));

        // 时尚服饰类商品 (20个)
        products.addAll(generateFashionProducts(random, 26, 45, tenantId));

        // 家居生活类商品 (20个)
        products.addAll(generateHomeProducts(random, 46, 65, tenantId));

        // 电脑办公类商品 (15个)
        products.addAll(generateComputerProducts(random, 66, 80, tenantId));

        // 汽车配件类商品 (20个)
        products.addAll(generateAutoPartsProducts(random, 81, 100, tenantId));

        log.info("生成了 {} 条商品数据", products.size());
        return products;
    }

    /**
     * 生成数码电子类商品
     */
    private List<Product> generateElectronicsProducts(Random random, int startId, int endId, String tenantId) {
        List<Product> products = new ArrayList<>();

        // 产品模板
        String[] productNames = {
            "iPhone 15 Pro Max", "Galaxy S24 Ultra", "Xiaomi 14 Pro", "Huawei Mate 60 Pro",
            "iPad Pro 12.9英寸", "Galaxy Tab S9", "Xiaomi Pad 6 Pro",
            "AirPods Pro 2代", "Galaxy Buds Pro", "Sony WF-1000XM5",
            "Apple Watch Ultra 2", "Galaxy Watch 6", "Huawei Watch GT 4",
            "Sony WH-1000XM5", "Bose QC45", "Xiaomi Buds 3 Pro",
            "iPhone 14 Pro", "Galaxy S23", "iPhone 15",
            "iPad Air 5", "Galaxy Tab A9", "Xiaomi Pad 5",
            "AirPods 3", "Galaxy Buds 2", "Xiaomi Redmi Buds 4"
        };

        String[] descriptions = {
            "旗舰级智能手机，配备最新处理器和摄像系统",
            "极致性能与影像体验，商务办公首选",
            "高性价比全能旗舰，体验未来科技",
            "创新科技与美学设计完美结合",
            "专业级平板电脑，创作办公两不误",
            "沉浸式影音体验，家庭娱乐中心",
            "轻薄便携，满足日常学习娱乐需求",
            "主动降噪耳机，静享纯净音质",
            "真无线立体声，佩戴舒适无感",
            "顶级音质，智能降噪",
            "全能运动手表，健康生活伴侣",
            "智能穿戴，健康监测",
            "专业运动健康，长续航设计",
            "头戴式降噪耳机，无线蓝牙连接",
            "行业领先降噪技术，舒适佩戴体验",
            "高性价比降噪耳机，音质出色"
        };

        Long[] brands = {
            BRAND_APPLE, BRAND_SAMSUNG, BRAND_XIAOMI, BRAND_HUAWEI,
            BRAND_APPLE, BRAND_SAMSUNG, BRAND_XIAOMI,
            BRAND_APPLE, BRAND_SAMSUNG, BRAND_SONY,
            BRAND_APPLE, BRAND_SAMSUNG, BRAND_HUAWEI,
            BRAND_SONY, BRAND_SONY, BRAND_XIAOMI,
            BRAND_APPLE, BRAND_SAMSUNG, BRAND_APPLE,
            BRAND_APPLE, BRAND_SAMSUNG, BRAND_XIAOMI,
            BRAND_APPLE, BRAND_SAMSUNG, BRAND_XIAOMI
        };

        BigDecimal[] prices = {
            new BigDecimal("9999"), new BigDecimal("8999"), new BigDecimal("4999"), new BigDecimal("6999"),
            new BigDecimal("8499"), new BigDecimal("6299"), new BigDecimal("2399"),
            new BigDecimal("1899"), new BigDecimal("1299"), new BigDecimal("2299"),
            new BigDecimal("6499"), new BigDecimal("3499"), new BigDecimal("2699"),
            new BigDecimal("2999"), new BigDecimal("2499"), new BigDecimal("799"),
            new BigDecimal("7999"), new BigDecimal("5999"), new BigDecimal("6499"),
            new BigDecimal("5499"), new BigDecimal("1999"), new BigDecimal("2199"),
            new BigDecimal("1299"), new BigDecimal("899"), new BigDecimal("399")
        };

        for (int i = 0; i < productNames.length && (startId + i) <= endId; i++) {
            Product product = new Product();
            int id = startId + i;

            product.setName(productNames[i]);
            product.setDescription(descriptions[i % descriptions.length]);
            product.setDetailDescription(generateDetailDescription(productNames[i], descriptions[i % descriptions.length]));
            product.setPrice(prices[i]);
            product.setOriginalPrice(prices[i].multiply(new BigDecimal("1.1")).setScale(2, RoundingMode.HALF_UP));
            product.setStock(random.nextInt(500) + 50);
            product.setUnit("件");
            product.setWeight(new BigDecimal(random.nextInt(500) + 50).divide(new BigDecimal("1000"), 3, RoundingMode.HALF_UP));
            product.setMinPurchase(1);
            product.setMaxPurchase(random.nextInt(5) + 5);

            // 销售统计
            product.setSales(random.nextInt(10000) + 100);
            product.setViewCount(random.nextInt(50000) + 5000);
            product.setFavoriteCount(random.nextInt(5000) + 500);

            // 评分
            double rating = 4.0 + random.nextDouble();
            product.setUserRating(new BigDecimal(String.format("%.1f", rating)));
            product.setUserReviewCount(random.nextInt(5000) + 500);

            // 排序
            product.setSortOrder(random.nextInt(1000) + 100);
            product.setProductCode("ELEC" + String.format("%06d", id));

            // 分类和品牌
            product.setCategoryId(CATEGORY_ELECTRONICS);
            product.setBrandId(brands[i]);

            // 图片 - 使用与产品相关的图片
            String imagePrompt = productNames[i].replace(" ", "%20");
            product.setMainImage(String.format("https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=%s%%20product%%20photo&image_size=square", imagePrompt));

            // 状态标识
            product.setIsNew(random.nextInt(3) == 0 ? 1 : 0);
            product.setIsHot(random.nextInt(2) == 0 ? 1 : 0);
            product.setIsRecommend(random.nextInt(3) == 0 ? 1 : 0);
            product.setStatus(1);

            // 时间
            product.setOnShelfTime(LocalDateTime.now().minusDays(random.nextInt(60)));
            product.setCreatedAt(LocalDateTime.now());
            product.setUpdatedAt(LocalDateTime.now());

            // 标签
            product.setTags(generateTags(random, productNames[i]));

            // 设置租户ID
            product.setTenantId(tenantId);

            products.add(product);
        }

        return products;
    }

    /**
     * 生成时尚服饰类商品
     */
    private List<Product> generateFashionProducts(Random random, int startId, int endId, String tenantId) {
        List<Product> products = new ArrayList<>();

        String[] productNames = {
            "Nike Air Max 270", "Adidas Ultraboost 22", "Nike Dunk Low", "Adidas Yeezy Boost 350",
            "Nike Air Force 1", "Adidas Stan Smith", "Nike Jordan 1", "Adidas Superstar",
            "优衣库Airism T恤", "ZARA 连衣裙", "H&M 牛仔裤", "GU 夹克",
            "耐克运动裤", "阿迪达斯卫衣", "匡威帆布鞋", "Vans Old Skool",
            "New Balance 990", "Puma RS-X", "锐步Classic", "彪马Suede"
        };

        String[] descriptions = {
            "经典运动鞋款，舒适透气，日常穿搭必备",
            "顶级缓震科技，跑步健身首选",
            "街头潮流单品，时尚百搭",
            "限量联名款，潮流人士必入",
            "经典百搭款，永不过时",
            "简约设计，舒适有型",
            "传奇篮球鞋，街头文化的象征",
            "经典三叶草，复古时尚",
            "轻凉速干，夏季清爽",
            "优雅设计，展现女性魅力",
            "修身版型，时尚百搭",
            "简约实用，四季可穿",
            "运动休闲，舒适自在",
            "时尚潮流，彰显个性",
            "复古经典，永不过时",
            "街头潮流，年轻必备",
            "顶级工艺，极致舒适",
            "创新设计，未来感十足",
            "经典复古，怀旧风格",
            "经典款型，潮流永恒"
        };

        Long[] brands = {
            BRAND_NIKE, BRAND_ADIDAS, BRAND_NIKE, BRAND_ADIDAS,
            BRAND_NIKE, BRAND_ADIDAS, BRAND_NIKE, BRAND_ADIDAS,
            BRAND_NIKE, BRAND_NIKE, BRAND_NIKE, BRAND_NIKE,
            BRAND_NIKE, BRAND_ADIDAS, BRAND_NIKE, BRAND_NIKE,
            BRAND_NIKE, BRAND_ADIDAS, BRAND_ADIDAS, BRAND_ADIDAS
        };

        BigDecimal[] prices = {
            new BigDecimal("1299"), new BigDecimal("1599"), new BigDecimal("999"), new BigDecimal("2499"),
            new BigDecimal("899"), new BigDecimal("699"), new BigDecimal("1599"), new BigDecimal("799"),
            new BigDecimal("199"), new BigDecimal("399"), new BigDecimal("499"), new BigDecimal("699"),
            new BigDecimal("599"), new BigDecimal("699"), new BigDecimal("599"), new BigDecimal("699"),
            new BigDecimal("1999"), new BigDecimal("899"), new BigDecimal("899"), new BigDecimal("799")
        };

        for (int i = 0; i < productNames.length && (startId + i) <= endId; i++) {
            Product product = new Product();
            int id = startId + i;

            product.setName(productNames[i]);
            product.setDescription(descriptions[i % descriptions.length]);
            product.setDetailDescription(generateDetailDescription(productNames[i], descriptions[i % descriptions.length]));
            product.setPrice(prices[i]);
            product.setOriginalPrice(prices[i].multiply(new BigDecimal("1.15")).setScale(2, RoundingMode.HALF_UP));
            product.setStock(random.nextInt(1000) + 100);
            product.setUnit("件");
            product.setWeight(new BigDecimal(random.nextInt(1000) + 200).divide(new BigDecimal("1000"), 3, RoundingMode.HALF_UP));
            product.setMinPurchase(1);
            product.setMaxPurchase(random.nextInt(10) + 10);

            product.setSales(random.nextInt(20000) + 2000);
            product.setViewCount(random.nextInt(80000) + 8000);
            product.setFavoriteCount(random.nextInt(10000) + 1000);

            double rating = 4.2 + random.nextDouble() * 0.7;
            product.setUserRating(new BigDecimal(String.format("%.1f", rating)));
            product.setUserReviewCount(random.nextInt(8000) + 800);

            product.setSortOrder(random.nextInt(1000) + 100);
            product.setProductCode("FASH" + String.format("%06d", id));

            product.setCategoryId(CATEGORY_FASHION);
            product.setBrandId(brands[i]);

            // 图片 - 使用与产品相关的图片
            String imagePrompt = productNames[i].replace(" ", "%20");
            product.setMainImage(String.format("https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=%s%%20product%%20photo&image_size=square", imagePrompt));

            product.setIsNew(random.nextInt(4) == 0 ? 1 : 0);
            product.setIsHot(random.nextInt(3) == 0 ? 1 : 0);
            product.setIsRecommend(random.nextInt(4) == 0 ? 1 : 0);
            product.setStatus(1);

            product.setOnShelfTime(LocalDateTime.now().minusDays(random.nextInt(90)));
            product.setCreatedAt(LocalDateTime.now());
            product.setUpdatedAt(LocalDateTime.now());

            product.setTags(generateTags(random, productNames[i]));

            // 设置租户ID
            product.setTenantId(tenantId);

            products.add(product);
        }

        return products;
    }

    /**
     * 生成家居生活类商品
     */
    private List<Product> generateHomeProducts(Random random, int startId, int endId, String tenantId) {
        List<Product> products = new ArrayList<>();

        String[] productNames = {
            "小米智能音箱", "小爱音箱Pro", "天猫精灵方糖", "小度在家",
            "美的电饭煲", "九阳豆浆机", "苏泊尔高压锅", "飞利浦吸尘器",
            "乐扣乐扣保鲜盒", "特百惠水杯", "象印保温杯", "膳魔师焖烧杯",
            "小米空气净化器", "飞利浦加湿器", "戴森吸尘器", "海尔扫地机器人",
            "宜家台灯", "飞利浦台灯", "欧普照明", "松下台灯"
        };

        String[] descriptions = {
            "智能语音助手，家居控制中心",
            "高保真音质，智能语音交互",
            "小巧可爱，语音控制更便捷",
            "智能显示屏，视频通话方便",
            "多功能智能电饭煲，一键烹饪",
            "家用豆浆机，营养健康",
            "高压快速烹饪，锁住美味营养",
            "无线吸尘器，清洁更轻松",
            "食品级材质，保鲜更持久",
            "运动水杯，健康饮水",
            "长效保温，随身携带",
            "便携焖烧杯，随时随地享用美食",
            "高效净化，守护家人健康",
            "智能加湿，告别干燥空气",
            "高端吸尘器，清洁效果出众",
            "全自动扫地，解放双手",
            "简约设计，护眼照明",
            "智能调光，护眼更舒适",
            "节能环保，明亮舒适",
            "优质台灯，照明护眼"
        };

        Long[] brands = {
            BRAND_XIAOMI, BRAND_XIAOMI, BRAND_XIAOMI, BRAND_XIAOMI,
            BRAND_HUAWEI, BRAND_HUAWEI, BRAND_HUAWEI, BRAND_SONY,
            BRAND_XIAOMI, BRAND_XIAOMI, BRAND_XIAOMI, BRAND_XIAOMI,
            BRAND_XIAOMI, BRAND_SONY, BRAND_SONY, BRAND_HUAWEI,
            BRAND_XIAOMI, BRAND_SONY, BRAND_XIAOMI, BRAND_SONY
        };

        BigDecimal[] prices = {
            new BigDecimal("299"), new BigDecimal("699"), new BigDecimal("199"), new BigDecimal("1299"),
            new BigDecimal("599"), new BigDecimal("499"), new BigDecimal("399"), new BigDecimal("2999"),
            new BigDecimal("199"), new BigDecimal("129"), new BigDecimal("299"), new BigDecimal("399"),
            new BigDecimal("899"), new BigDecimal("699"), new BigDecimal("4999"), new BigDecimal("2999"),
            new BigDecimal("299"), new BigDecimal("499"), new BigDecimal("199"), new BigDecimal("399")
        };

        for (int i = 0; i < productNames.length && (startId + i) <= endId; i++) {
            Product product = new Product();
            int id = startId + i;

            product.setName(productNames[i]);
            product.setDescription(descriptions[i % descriptions.length]);
            product.setDetailDescription(generateDetailDescription(productNames[i], descriptions[i % descriptions.length]));
            product.setPrice(prices[i]);
            product.setOriginalPrice(prices[i].multiply(new BigDecimal("1.12")).setScale(2, RoundingMode.HALF_UP));
            product.setStock(random.nextInt(800) + 80);
            product.setUnit("件");
            product.setWeight(new BigDecimal(random.nextInt(3000) + 500).divide(new BigDecimal("1000"), 3, RoundingMode.HALF_UP));
            product.setMinPurchase(1);
            product.setMaxPurchase(random.nextInt(8) + 8);

            product.setSales(random.nextInt(15000) + 1500);
            product.setViewCount(random.nextInt(60000) + 6000);
            product.setFavoriteCount(random.nextInt(8000) + 800);

            double rating = 4.3 + random.nextDouble() * 0.6;
            product.setUserRating(new BigDecimal(String.format("%.1f", rating)));
            product.setUserReviewCount(random.nextInt(6000) + 600);

            product.setSortOrder(random.nextInt(1000) + 100);
            product.setProductCode("HOME" + String.format("%06d", id));

            product.setCategoryId(CATEGORY_HOME);
            product.setBrandId(brands[i]);

            // 图片 - 使用与产品相关的图片
            String imagePrompt = productNames[i].replace(" ", "%20");
            product.setMainImage(String.format("https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=%s%%20product%%20photo&image_size=square", imagePrompt));

            product.setIsNew(random.nextInt(5) == 0 ? 1 : 0);
            product.setIsHot(random.nextInt(4) == 0 ? 1 : 0);
            product.setIsRecommend(random.nextInt(5) == 0 ? 1 : 0);
            product.setStatus(1);

            product.setOnShelfTime(LocalDateTime.now().minusDays(random.nextInt(75)));
            product.setCreatedAt(LocalDateTime.now());
            product.setUpdatedAt(LocalDateTime.now());

            product.setTags(generateTags(random, productNames[i]));

            // 设置租户ID
            product.setTenantId(tenantId);

            products.add(product);
        }

        return products;
    }

    /**
     * 生成电脑办公类商品
     */
    private List<Product> generateComputerProducts(Random random, int startId, int endId, String tenantId) {
        List<Product> products = new ArrayList<>();

        String[] productNames = {
            "MacBook Pro 14英寸", "MacBook Air M2", "ThinkPad X1 Carbon",
            "Dell XPS 15", "HP Spectre x360", "Lenovo ThinkBook",
            "Logitech MX Master 3", "罗技G Pro鼠标", "雷蛇 DeathAdder",
            "Cherry MX机械键盘", "Keychron K2键盘", "罗技K845",
            "华为MateView显示器", "戴尔U2723QE", "明基PD3200U",
            "WD 1TB固态硬盘", "三星980 Pro SSD", "金士顿KC3000"
        };

        String[] descriptions = {
            "专业级笔记本电脑，强劲性能，创作者首选",
            "轻薄便携，续航持久，日常办公利器",
            "商务旗舰，安全可靠，企业首选",
            "超窄边框，极致轻薄，视觉震撼",
            "360度翻转，触控办公，灵活多变",
            "高性价比商务本，性能均衡",
            "人体工学设计，精准操控",
            "电竞级游戏鼠标，低延迟高性能",
            "经典游戏鼠标，手感出众",
            "顶级机械键盘，敲击手感极佳",
            "无线机械键盘，多设备连接",
            "入门级机械键盘，性价比之选",
            "4K高清显示器，色彩精准",
            "专业级显示器，色彩还原度极高",
            "设计师专用显示器，色彩丰富",
            "高速固态硬盘，系统秒开",
            "顶级NVMe SSD，读写速度惊人",
            "高性能固态硬盘，稳定可靠"
        };

        Long[] brands = {
            BRAND_APPLE, BRAND_APPLE, BRAND_HUAWEI,
            BRAND_SAMSUNG, BRAND_SAMSUNG, BRAND_HUAWEI,
            BRAND_LOGITECH, BRAND_LOGITECH, BRAND_LOGITECH,
            BRAND_LOGITECH, BRAND_LOGITECH, BRAND_LOGITECH,
            BRAND_HUAWEI, BRAND_SAMSUNG, BRAND_SONY,
            BRAND_SAMSUNG, BRAND_SAMSUNG, BRAND_XIAOMI
        };

        BigDecimal[] prices = {
            new BigDecimal("16999"), new BigDecimal("8999"), new BigDecimal("12999"),
            new BigDecimal("14999"), new BigDecimal("11999"), new BigDecimal("6999"),
            new BigDecimal("799"), new BigDecimal("599"), new BigDecimal("499"),
            new BigDecimal("899"), new BigDecimal("699"), new BigDecimal("399"),
            new BigDecimal("3999"), new BigDecimal("4999"), new BigDecimal("6999"),
            new BigDecimal("699"), new BigDecimal("999"), new BigDecimal("899")
        };

        for (int i = 0; i < productNames.length && (startId + i) <= endId; i++) {
            Product product = new Product();
            int id = startId + i;

            product.setName(productNames[i]);
            product.setDescription(descriptions[i % descriptions.length]);
            product.setDetailDescription(generateDetailDescription(productNames[i], descriptions[i % descriptions.length]));
            product.setPrice(prices[i]);
            product.setOriginalPrice(prices[i].multiply(new BigDecimal("1.08")).setScale(2, RoundingMode.HALF_UP));
            product.setStock(random.nextInt(300) + 30);
            product.setUnit("件");
            product.setWeight(new BigDecimal(random.nextInt(5000) + 500).divide(new BigDecimal("1000"), 3, RoundingMode.HALF_UP));
            product.setMinPurchase(1);
            product.setMaxPurchase(random.nextInt(5) + 3);

            product.setSales(random.nextInt(5000) + 500);
            product.setViewCount(random.nextInt(30000) + 3000);
            product.setFavoriteCount(random.nextInt(3000) + 300);

            double rating = 4.5 + random.nextDouble() * 0.4;
            product.setUserRating(new BigDecimal(String.format("%.1f", rating)));
            product.setUserReviewCount(random.nextInt(3000) + 300);

            product.setSortOrder(random.nextInt(1000) + 100);
            product.setProductCode("COMP" + String.format("%06d", id));

            product.setCategoryId(CATEGORY_COMPUTER);
            product.setBrandId(brands[i]);

            // 图片 - 使用与产品相关的图片
            String imagePrompt = productNames[i].replace(" ", "%20");
            product.setMainImage(String.format("https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=%s%%20product%%20photo&image_size=square", imagePrompt));

            product.setIsNew(random.nextInt(3) == 0 ? 1 : 0);
            product.setIsHot(random.nextInt(4) == 0 ? 1 : 0);
            product.setIsRecommend(random.nextInt(3) == 0 ? 1 : 0);
            product.setStatus(1);

            product.setOnShelfTime(LocalDateTime.now().minusDays(random.nextInt(45)));
            product.setCreatedAt(LocalDateTime.now());
            product.setUpdatedAt(LocalDateTime.now());

            product.setTags(generateTags(random, productNames[i]));

            // 设置租户ID
            product.setTenantId(tenantId);

            products.add(product);
        }

        return products;
    }

    /**
     * 生成汽车配件类商品
     */
    private List<Product> generateAutoPartsProducts(Random random, int startId, int endId, String tenantId) {
        List<Product> products = new ArrayList<>();

        String[] productNames = {
            "米其林静音轮胎", "倍耐力高性能轮胎", "马牌舒适型轮胎", "固特异全天候轮胎",
            "博世刹车盘", "布雷博刹车卡钳", "菲罗多刹车片", "天合刹车油",
            "壳牌全合成机油", "美孚一号机油", "嘉实多极护机油", "长城润滑油",
            "曼牌空气滤清器", "博世机油滤清器", "马勒燃油滤清器", "索菲玛空调滤清器",
            "博世雨刮器", "法雷奥雨刮器", "电装火花塞", "NGK火花塞"
        };

        String[] descriptions = {
            "高品质轮胎，静音舒适，抓地力强",
            "高性能轮胎，操控性出色，适合运动车型",
            "舒适型轮胎，减震效果好，长途驾驶首选",
            "全天候轮胎，适应各种路况，安全可靠",
            "高品质刹车盘，散热性能好，使用寿命长",
            "高性能刹车卡钳，制动效果出色，专业改装首选",
            "优质刹车片，摩擦系数稳定，制动距离短",
            "高品质刹车油，沸点高，安全可靠",
            "全合成机油，润滑性能好，保护发动机",
            "顶级全合成机油，长效保护，延长发动机寿命",
            "高性能机油，清洁能力强，减少发动机积碳",
            "国产品牌润滑油，性价比高，品质可靠",
            "高品质空气滤清器，过滤效果好，保护发动机",
            "专业机油滤清器，过滤精度高，延长机油寿命",
            "优质燃油滤清器，过滤杂质，保护燃油系统",
            "高效空调滤清器，过滤PM2.5，改善车内空气质量",
            "高品质雨刮器，刮拭干净，静音无抖动",
            "专业雨刮器，耐用性强，适应各种天气",
            "高性能火花塞，点火可靠，提升动力",
            "优质火花塞，寿命长，节省燃油"
        };

        Long[] brands = {
            BRAND_HUAWEI, BRAND_SAMSUNG, BRAND_XIAOMI, BRAND_HUAWEI,
            BRAND_SONY, BRAND_SONY, BRAND_XIAOMI, BRAND_SAMSUNG,
            BRAND_APPLE, BRAND_SAMSUNG, BRAND_XIAOMI, BRAND_HUAWEI,
            BRAND_LOGITECH, BRAND_LOGITECH, BRAND_LOGITECH, BRAND_LOGITECH,
            BRAND_SONY, BRAND_SAMSUNG, BRAND_XIAOMI, BRAND_HUAWEI
        };

        BigDecimal[] prices = {
            new BigDecimal("899"), new BigDecimal("1299"), new BigDecimal("999"), new BigDecimal("1099"),
            new BigDecimal("499"), new BigDecimal("2999"), new BigDecimal("299"), new BigDecimal("129"),
            new BigDecimal("399"), new BigDecimal("499"), new BigDecimal("459"), new BigDecimal("199"),
            new BigDecimal("89"), new BigDecimal("69"), new BigDecimal("79"), new BigDecimal("59"),
            new BigDecimal("159"), new BigDecimal("129"), new BigDecimal("199"), new BigDecimal("179")
        };

        for (int i = 0; i < productNames.length && (startId + i) <= endId; i++) {
            Product product = new Product();
            int id = startId + i;

            product.setName(productNames[i]);
            product.setDescription(descriptions[i % descriptions.length]);
            product.setDetailDescription(generateDetailDescription(productNames[i], descriptions[i % descriptions.length]));
            product.setPrice(prices[i]);
            product.setOriginalPrice(prices[i].multiply(new BigDecimal("1.1")).setScale(2, RoundingMode.HALF_UP));
            product.setStock(random.nextInt(600) + 60);
            product.setUnit("件");
            product.setWeight(new BigDecimal(random.nextInt(10000) + 1000).divide(new BigDecimal("1000"), 3, RoundingMode.HALF_UP));
            product.setMinPurchase(1);
            product.setMaxPurchase(random.nextInt(10) + 5);

            product.setSales(random.nextInt(8000) + 800);
            product.setViewCount(random.nextInt(40000) + 4000);
            product.setFavoriteCount(random.nextInt(4000) + 400);

            double rating = 4.4 + random.nextDouble() * 0.5;
            product.setUserRating(new BigDecimal(String.format("%.1f", rating)));
            product.setUserReviewCount(random.nextInt(4000) + 400);

            product.setSortOrder(random.nextInt(1000) + 100);
            product.setProductCode("AUTO" + String.format("%06d", id));

            product.setCategoryId(CATEGORY_AUTO_PARTS);
            product.setBrandId(brands[i]);

            // 图片 - 使用与产品相关的图片
            String imagePrompt = productNames[i].replace(" ", "%20");
            product.setMainImage(String.format("https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=%s%%20product%%20photo&image_size=square", imagePrompt));

            product.setIsNew(random.nextInt(4) == 0 ? 1 : 0);
            product.setIsHot(random.nextInt(3) == 0 ? 1 : 0);
            product.setIsRecommend(random.nextInt(4) == 0 ? 1 : 0);
            product.setStatus(1);

            product.setOnShelfTime(LocalDateTime.now().minusDays(random.nextInt(60)));
            product.setCreatedAt(LocalDateTime.now());
            product.setUpdatedAt(LocalDateTime.now());

            product.setTags(generateTags(random, productNames[i]));

            // 设置租户ID
            product.setTenantId("000000");

            products.add(product);
        }

        return products;
    }

    /**
     * 生成商品详细描述
     */
    private String generateDetailDescription(String productName, String description) {
        return String.format(
            "<h2>%s</h2>" +
            "<p><strong>商品简介：</strong>%s</p>" +
            "<h3>产品特点：</h3>" +
            "<ul>" +
            "<li>精选优质材料，品质保证</li>" +
            "<li>工艺精湛，细节考究</li>" +
            "<li>经过严格的质量检测</li>" +
            "<li>提供完善的售后服务</li>" +
            "</ul>" +
            "<h3>使用场景：</h3>" +
            "<p>适用于日常使用、办公学习、休闲娱乐等多种场景，满足您的不同需求。</p>" +
            "<h3>售后服务：</h3>" +
            "<p>我们提供7天无理由退换货服务，全国联保，让您购物无忧。</p>",
            productName,
            description
        );
    }

    /**
     * 生成商品标签
     */
    private List<String> generateTags(Random random, String productName) {
        List<String> allTags = new ArrayList<>(Arrays.asList(
            "热卖", "新品", "包邮", "正品保证", "七天退换",
            "限时优惠", "品牌精选", "品质保证", "热销推荐", "包邮到家"
        ));

        // 根据商品名称添加特定标签
        if (productName.contains("iPhone") || productName.contains("MacBook") ||
            productName.contains("iPad") || productName.contains("Galaxy")) {
            allTags.add("旗舰产品");
            allTags.add("高端品质");
        }
        if (productName.contains("Pro") || productName.contains("Max")) {
            allTags.add("专业级");
        }
        if (productName.contains("Air") || productName.contains("Lite")) {
            allTags.add("轻薄便携");
        }
        if (productName.contains("降噪")) {
            allTags.add("智能降噪");
        }
        if (productName.contains("运动") || productName.contains("跑步")) {
            allTags.add("运动专用");
        }

        // 随机选择3-5个标签
        Collections.shuffle(allTags);
        int tagCount = 3 + random.nextInt(3);
        return allTags.subList(0, Math.min(tagCount, allTags.size()));
    }

    /**
     * 初始化优惠券数据
     */
    private void initCoupons(String tenantId) {
        log.info("开始初始化优惠券数据...");

        try {
            // 检查是否已有优惠券数据
            long existingCount = couponMapper.selectCount(null);
            if (existingCount > 0) {
                log.warn("数据库中已存在 {} 条优惠券数据，跳过初始化", existingCount);
                return;
            }

            // 生成优惠券数据
            List<Coupon> coupons = new ArrayList<>();

            // 新用户优惠券 NEWUSER100
            Coupon newUserCoupon = new Coupon();
            newUserCoupon.setName("新用户专享券");
            newUserCoupon.setCode("NEWUSER100");
            newUserCoupon.setType(1); // 固定金额
            newUserCoupon.setValue(new BigDecimal("100"));
            newUserCoupon.setMinSpend(new BigDecimal("500"));
            newUserCoupon.setMaxDiscount(new BigDecimal("100"));
            newUserCoupon.setStartTime(LocalDateTime.now().minusDays(30));
            newUserCoupon.setEndTime(LocalDateTime.now().plusDays(365));
            newUserCoupon.setTotalQuantity(10000);
            newUserCoupon.setUsedQuantity(0);
            newUserCoupon.setPerUserLimit(1);
            newUserCoupon.setStatus(1);
            newUserCoupon.setCreatedAt(LocalDateTime.now());
            newUserCoupon.setUpdatedAt(LocalDateTime.now());
            newUserCoupon.setTenantId(tenantId);
            coupons.add(newUserCoupon);

            // 满减优惠券
            Coupon fixedAmountCoupon = new Coupon();
            fixedAmountCoupon.setName("满300减50");
            fixedAmountCoupon.setCode("满300减50");
            fixedAmountCoupon.setType(1); // 固定金额
            fixedAmountCoupon.setValue(new BigDecimal("50"));
            fixedAmountCoupon.setMinSpend(new BigDecimal("300"));
            fixedAmountCoupon.setMaxDiscount(new BigDecimal("50"));
            fixedAmountCoupon.setStartTime(LocalDateTime.now().minusDays(15));
            fixedAmountCoupon.setEndTime(LocalDateTime.now().plusDays(60));
            fixedAmountCoupon.setTotalQuantity(5000);
            fixedAmountCoupon.setUsedQuantity(0);
            fixedAmountCoupon.setPerUserLimit(2);
            fixedAmountCoupon.setStatus(1);
            fixedAmountCoupon.setCreatedAt(LocalDateTime.now());
            fixedAmountCoupon.setUpdatedAt(LocalDateTime.now());
            fixedAmountCoupon.setTenantId(tenantId);
            coupons.add(fixedAmountCoupon);

            // 折扣优惠券
            Coupon discountCoupon = new Coupon();
            discountCoupon.setName("全场9折");
            discountCoupon.setCode("全场9折");
            discountCoupon.setType(2); // 百分比
            discountCoupon.setValue(new BigDecimal("0.9"));
            discountCoupon.setMinSpend(new BigDecimal("100"));
            discountCoupon.setMaxDiscount(new BigDecimal("200"));
            discountCoupon.setStartTime(LocalDateTime.now().minusDays(10));
            discountCoupon.setEndTime(LocalDateTime.now().plusDays(30));
            discountCoupon.setTotalQuantity(3000);
            discountCoupon.setUsedQuantity(0);
            discountCoupon.setPerUserLimit(1);
            discountCoupon.setStatus(1);
            discountCoupon.setCreatedAt(LocalDateTime.now());
            discountCoupon.setUpdatedAt(LocalDateTime.now());
            discountCoupon.setTenantId(tenantId);
            coupons.add(discountCoupon);

            // 插入优惠券数据
            for (Coupon coupon : coupons) {
                couponMapper.insert(coupon);
            }

            log.info("优惠券数据初始化完成！共插入 {} 条数据", coupons.size());
            log.info("已创建优惠券：NEWUSER100, 满300减50, 全场9折");

        } catch (Exception e) {
            log.error("优惠券数据初始化失败！", e);
            throw new RuntimeException("优惠券数据初始化失败", e);
        }
    }
}



