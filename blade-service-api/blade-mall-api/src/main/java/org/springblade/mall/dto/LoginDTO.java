package org.springblade.mall.dto;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录DTO
 */
@Data
public class LoginDTO {

    @NotBlank(message = "用户名或邮箱不能为空")
    private String email;

    @NotBlank(message = "密码不能为空")
    private String password;

    /**
     * 微信用户唯一标识
     */
    private String wxCode;
}




