package org.springblade.mall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 手机号快捷登录/注册 DTO
 */
@Data
public class PhoneRegisterDTO {

    @NotBlank(message = "手机号不能为空")
    @Size(min = 11, max = 11, message = "手机号长度应为 11 位")
    private String phone;

    /**
     * 微信登录 code
     */
    private String code;
}




