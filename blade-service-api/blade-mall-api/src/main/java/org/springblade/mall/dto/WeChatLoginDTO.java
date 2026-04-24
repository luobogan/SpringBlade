package org.springblade.mall.dto;

import lombok.Data;

/**
 * 微信登录 DTO
 */
@Data
public class WeChatLoginDTO {
    /**
     * 微信授权 code
     */
    private String code;

    /**
     * 租户ID
     */
    private String tenantId;
}
