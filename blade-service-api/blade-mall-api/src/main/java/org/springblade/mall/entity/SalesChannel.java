package org.springblade.mall.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import java.time.LocalDateTime;

/**
 * 销售渠道实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sales_channel")
public class SalesChannel extends MallTenantEntity {
    
    private String name;
    
    private String code;
    
    private String icon;
    
    private String description;
    
    private String config;
}




