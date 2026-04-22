package org.springblade.mall.utils;

import org.springblade.mall.entity.ProductSku;
import org.springblade.mall.entity.ProductSpecAttribute;
import org.springblade.mall.entity.ProductSpecValue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * SKU矩阵生成工具类
 */
public class SkuMatrixGenerator {

    /**
     * SKU规格组合
     */
    public static class SpecCombination {
        private Long attributeId;
        private String attributeName;
        private Long valueId;
        private String value;
        private String image;

        public SpecCombination(Long attributeId, String attributeName, Long valueId, String value, String image) {
            this.attributeId = attributeId;
            this.attributeName = attributeName;
            this.valueId = valueId;
            this.value = value;
            this.image = image;
        }

        // Getters
        public Long getAttributeId() { return attributeId; }
        public String getAttributeName() { return attributeName; }
        public Long getValueId() { return valueId; }
        public String getValue() { return value; }
        public String getImage() { return image; }
    }

    /**
     * 生成SKU矩阵
     * @param productId 商品ID
     * @param attributes 规格属性列表
     * @param valuesMap 规格属性值映射（attributeId -> values）
     * @param basePrice 基础价格
     * @param baseStock 基础库存
     * @return SKU列表
     */
    public static List<ProductSku> generateSkuMatrix(
            Long productId,
            List<ProductSpecAttribute> attributes,
            java.util.Map<Long, List<ProductSpecValue>> valuesMap,
            BigDecimal basePrice,
            Integer baseStock) {

        List<ProductSku> skuList = new ArrayList<>();

        if (attributes.isEmpty()) {
            return skuList;
        }

        // 获取所有规格的组合
        List<List<SpecCombination>> combinations = generateCombinations(attributes, valuesMap);

        // 为每种组合生成SKU
        int skuIndex = 1;
        for (List<SpecCombination> combination : combinations) {
            ProductSku sku = createSkuFromCombination(
                    productId, combination, skuIndex++, basePrice, baseStock);
            skuList.add(sku);
        }

        return skuList;
    }

    /**
     * 生成规格组合
     */
    private static List<List<SpecCombination>> generateCombinations(
            List<ProductSpecAttribute> attributes,
            java.util.Map<Long, List<ProductSpecValue>> valuesMap) {

        List<List<SpecCombination>> result = new ArrayList<>();

        if (attributes.isEmpty()) {
            return result;
        }

        // 从第一个属性开始递归生成组合
        generateCombinationsRecursive(
                attributes, valuesMap, 0, new ArrayList<>(), result);

        return result;
    }

    /**
     * 递归生成规格组合
     */
    private static void generateCombinationsRecursive(
            List<ProductSpecAttribute> attributes,
            java.util.Map<Long, List<ProductSpecValue>> valuesMap,
            int index,
            List<SpecCombination> current,
            List<List<SpecCombination>> result) {

        if (index == attributes.size()) {
            // 已经处理完所有属性，保存当前组合
            result.add(new ArrayList<>(current));
            return;
        }

        ProductSpecAttribute attribute = attributes.get(index);
        List<ProductSpecValue> values = valuesMap.get(attribute.getId());

        if (values == null || values.isEmpty()) {
            // 当前属性没有值，跳过
            generateCombinationsRecursive(
                    attributes, valuesMap, index + 1, current, result);
        } else {
            // 遍历当前属性的所有值
            for (ProductSpecValue value : values) {
                SpecCombination combination = new SpecCombination(
                        attribute.getId(),
                        attribute.getName(),
                        value.getId(),
                        value.getValue(),
                        value.getImage()
                );
                current.add(combination);
                generateCombinationsRecursive(
                        attributes, valuesMap, index + 1, current, result);
                current.remove(current.size() - 1); // 回溯
            }
        }
    }

    /**
     * 根据规格组合创建SKU
     */
    private static ProductSku createSkuFromCombination(
            Long productId,
            List<SpecCombination> combination,
            int skuIndex,
            BigDecimal basePrice,
            Integer baseStock) {

        ProductSku sku = new ProductSku();
        sku.setProductId(productId);
        sku.setStatus(1);
        sku.setCreatedAt(LocalDateTime.now());
        sku.setUpdatedAt(LocalDateTime.now());

        // 生成SKU编码
        String skuCode = generateSkuCode(productId, skuIndex);
        sku.setSkuCode(skuCode);

        // 设置规格值
        StringBuilder skuNameBuilder = new StringBuilder();
        for (int i = 0; i < combination.size(); i++) {
            SpecCombination spec = combination.get(i);
            skuNameBuilder.append(spec.getValue());
            if (i < combination.size() - 1) {
                skuNameBuilder.append("+");
            }

            // 设置spec1, spec2, spec3, spec4
            switch (i) {
                case 0:
                    sku.setSpec1(spec.getValue());
                    break;
                case 1:
                    sku.setSpec2(spec.getValue());
                    break;
                case 2:
                    sku.setSpec3(spec.getValue());
                    break;
                case 3:
                    sku.setSpec4(spec.getValue());
                    break;
            }

            // 使用第一个有图片的规格值图片作为SKU图片
            if (sku.getImage() == null && spec.getImage() != null && !spec.getImage().isEmpty()) {
                sku.setImage(spec.getImage());
            }
        }

        sku.setSkuName(skuNameBuilder.toString());
        sku.setPrice(basePrice);
        sku.setStock(baseStock);

        return sku;
    }

    /**
     * 生成SKU编码
     */
    private static String generateSkuCode(Long productId, int index) {
        return String.format("SKU-%d-%04d", productId, index);
    }

    /**
     * 计算SKU矩阵数量
     */
    public static int calculateSkuCount(
            List<ProductSpecAttribute> attributes,
            java.util.Map<Long, List<ProductSpecValue>> valuesMap) {

        if (attributes.isEmpty()) {
            return 0;
        }

        int count = 1;
        for (ProductSpecAttribute attribute : attributes) {
            List<ProductSpecValue> values = valuesMap.get(attribute.getId());
            if (values != null && !values.isEmpty()) {
                count *= values.size();
            }
        }

        return count;
    }
}
