package org.springblade.mall.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 促销规则解析工具类
 */
public class PromotionRuleParser {

    private static final Logger log = LoggerFactory.getLogger(PromotionRuleParser.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 促销规则
     */
    public static class PromotionRule {
        // 满减规则
        private BigDecimal fullAmount;      // 满多少
        private BigDecimal reduceAmount;    // 减多少

        // 折扣规则
        private BigDecimal discountRate;    // 折扣率（0-1之间）

        // 秒杀/团购规则
        private BigDecimal promotionPrice;  // 促销价格
        private Integer limitQuantity;      // 限购数量

        // Getter 和 Setter 方法
        public BigDecimal getFullAmount() {
            return fullAmount;
        }

        public void setFullAmount(BigDecimal fullAmount) {
            this.fullAmount = fullAmount;
        }

        public BigDecimal getReduceAmount() {
            return reduceAmount;
        }

        public void setReduceAmount(BigDecimal reduceAmount) {
            this.reduceAmount = reduceAmount;
        }

        public BigDecimal getDiscountRate() {
            return discountRate;
        }

        public void setDiscountRate(BigDecimal discountRate) {
            this.discountRate = discountRate;
        }

        public BigDecimal getPromotionPrice() {
            return promotionPrice;
        }

        public void setPromotionPrice(BigDecimal promotionPrice) {
            this.promotionPrice = promotionPrice;
        }

        public Integer getLimitQuantity() {
            return limitQuantity;
        }

        public void setLimitQuantity(Integer limitQuantity) {
            this.limitQuantity = limitQuantity;
        }
    }

    /**
     * 解析促销规则JSON
     * @param rulesJson 规则JSON字符串
     * @param type 促销类型
     * @return 解析后的规则
     */
    public static PromotionRule parse(String rulesJson, Integer type) {
        try {
            JsonNode root = objectMapper.readTree(rulesJson);
            PromotionRule rule = new PromotionRule();

            switch (type) {
                case 1: // 满减
                    if (root.has("fullAmount")) {
                        rule.setFullAmount(new BigDecimal(root.get("fullAmount").asText()));
                    }
                    if (root.has("reduceAmount")) {
                        rule.setReduceAmount(new BigDecimal(root.get("reduceAmount").asText()));
                    }
                    break;

                case 2: // 折扣
                    if (root.has("discountRate")) {
                        rule.setDiscountRate(new BigDecimal(root.get("discountRate").asText()));
                    }
                    break;

                case 3: // 秒杀
                case 4: // 团购
                    if (root.has("promotionPrice")) {
                        rule.setPromotionPrice(new BigDecimal(root.get("promotionPrice").asText()));
                    }
                    if (root.has("limitQuantity")) {
                        rule.setLimitQuantity(root.get("limitQuantity").asInt());
                    }
                    break;

                default:
                    log.warn("未知的促销类型: {}", type);
            }

            return rule;
        } catch (Exception e) {
            log.error("解析促销规则失败: {}", rulesJson, e);
            return null;
        }
    }

    /**
     * 计算促销价格
     * @param originalPrice 原价
     * @param rule 促销规则
     * @param type 促销类型
     * @return 促销价格
     */
    public static BigDecimal calculatePromotionPrice(BigDecimal originalPrice, PromotionRule rule, Integer type) {
        if (rule == null || originalPrice == null) {
            return originalPrice;
        }

        try {
            switch (type) {
                case 1: // 满减
                    if (rule.getFullAmount() != null && rule.getReduceAmount() != null
                            && originalPrice.compareTo(rule.getFullAmount()) >= 0) {
                        return originalPrice.subtract(rule.getReduceAmount()).max(BigDecimal.ZERO);
                    }
                    return originalPrice;

                case 2: // 折扣
                    if (rule.getDiscountRate() != null) {
                        return originalPrice.multiply(rule.getDiscountRate())
                                .setScale(2, RoundingMode.HALF_UP);
                    }
                    return originalPrice;

                case 3: // 秒杀
                case 4: // 团购
                    if (rule.getPromotionPrice() != null) {
                        return rule.getPromotionPrice();
                    }
                    return originalPrice;

                default:
                    return originalPrice;
            }
        } catch (Exception e) {
            log.error("计算促销价格失败", e);
            return originalPrice;
        }
    }

    /**
     * 验证促销规则JSON格式
     * @param rulesJson 规则JSON字符串
     * @param type 促销类型
     * @return 是否有效
     */
    public static boolean validate(String rulesJson, Integer type) {
        try {
            JsonNode root = objectMapper.readTree(rulesJson);

            switch (type) {
                case 1: // 满减
                    return root.has("fullAmount") && root.has("reduceAmount");

                case 2: // 折扣
                    return root.has("discountRate");

                case 3: // 秒杀
                case 4: // 团购
                    return root.has("promotionPrice");

                default:
                    return false;
            }
        } catch (Exception e) {
            log.error("验证促销规则失败", e);
            return false;
        }
    }
}
