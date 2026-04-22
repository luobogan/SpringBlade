package org.springblade.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;


import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 促销规则实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)

@TableName("mall_promotion")
public class Promotion extends MallTenantEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 促销名称
     */
    private String name;

    /**
     * 促销描述
     */
    private String description;

    /**
     * 促销类型：1满减，2折扣，3秒杀，4团购
     */
    private Integer type;

    /**
     * 促销规则JSON
     */
    private String rules;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 创建人ID
     */
    private Long createdBy;
}





