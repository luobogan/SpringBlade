package org.springblade.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;


import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 物流信息实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)

@TableName("mall_logistics")
public class Logistics extends MallTenantEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 物流单号
     */
    private String trackingNo;

    /**
     * 物流公司
     */
    private String logisticsCompany;

    /**
     * 物流状态：PENDING待发货，SHIPPED已发货，IN_TRANSIT运输中，DELIVERED已送达，SIGNED已签收
     */
    @TableField("status")
    private String logisticsStatus;

    /**
     * 最新物流信息
     */
    private String latestInfo;

    /**
     * 物流轨迹JSON
     */
    private String trackingDetails;

    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}





