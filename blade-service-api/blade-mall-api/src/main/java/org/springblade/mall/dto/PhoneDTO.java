package org.springblade.mall.dto;

import lombok.Data;

/**
 * 微信手机号获取 DTO
 */
@Data
public class PhoneDTO {
    /**
     * 微信登录 code
     */
    private String code;
    
    /**
     * 加密数据
     */
    private String encryptedData;
    
    /**
     * 加密向量
     */
    private String iv;
}




