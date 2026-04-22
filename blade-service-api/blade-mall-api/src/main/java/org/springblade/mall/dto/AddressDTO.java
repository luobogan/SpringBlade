package org.springblade.mall.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.Pattern;
import java.io.Serializable;

/**
 * 地址 DTO
 */
@Data
public class AddressDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 地址ID（更新时需要）
     */
    private Long id;

    /**
     * 收货人姓名
     */
    @NotBlank(message = "收货人姓名不能为空")
    private String name;

    /**
     * 收货人电话
     */
    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /**
     * 省份
     */
    @NotBlank(message = "省份不能为空")
    private String province;

    /**
     * 城市
     */
    @NotBlank(message = "城市不能为空")
    private String city;

    /**
     * 区县
     */
    @NotBlank(message = "区县不能为空")
    private String district;

    /**
     * 详细地址
     */
    @NotBlank(message = "详细地址不能为空")
    private String detail;

    /**
     * 邮政编码
     */
    private String postalCode;

    /**
     * 是否设置为默认地址
     */
    private Boolean isDefault;
}




