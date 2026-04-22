package org.springblade.mall.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 微信登录DTO
 */
@Data
public class WeChatLoginDTO {

    /**
     * 微信登录code
     */
    @NotBlank(message = "微信登录code不能为空")
    private String code;
}




