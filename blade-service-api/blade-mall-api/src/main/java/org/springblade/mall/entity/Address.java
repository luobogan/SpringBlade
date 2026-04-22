package org.springblade.mall.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;



/**
 * 地址实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)

@TableName("mall_address")
public class Address extends MallTenantEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 收货人姓名
     */
    private String name;

	    /**
     * 收货人姓名
     */
    private String consignee;

    /**
     * 收货人电话
     */
    private String phone;

    /**
     * 省份
     */
    private String province;

    /**
     * 城市
     */
    private String city;

    /**
     * 区县
     */
    private String district;

    /**
     * 详细地址
     */
    private String detail;

    /**
     * 邮政编码
     */
    private String postalCode;


	private String detailAddress;

    /**
     * 是否默认地址
     */
    @TableField("is_default")
    private Boolean isDefault;
}





