package org.springblade.mall.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * SKU库存变动日志VO
 */
@Data
public class SkuStockLogVO {

    /**
     * 日志ID
     */
    private Long id;

    /**
     * SKU ID
     */
    private Long skuId;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 变动类型：1入库，2出库，3调整
     */
    private Integer type;

    /**
     * 变动类型文本
     */
    private String typeText;

    /**
     * 变动数量
     */
    private Integer quantity;

    /**
     * 变动前库存
     */
    private Integer beforeStock;

    /**
     * 变动后库存
     */
    private Integer afterStock;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 操作人名称
     */
    private String operatorName;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}




