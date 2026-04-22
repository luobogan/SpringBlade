package org.springblade.mall.service;

import org.springblade.mall.vo.WeChatSessionVO;

/**
 * 微信服务接口
 */
public interface WeChatService {
    
    /**
     * 通过微信 code 获取 openid 和 session_key
     * 
     * @param code 微信登录临时凭证
     * @return 微信会话信息
     */
    WeChatSessionVO getSessionByCode(String code);
    
    /**
     * 通过微信 code 获取 session（别名）
     * 
     * @param code 微信登录临时凭证
     * @return 微信会话信息
     */
    WeChatSessionVO getWeChatSession(String code);
    
    /**
     * 解密微信加密数据
     * 
     * @param sessionKey 会话密钥
     * @param encryptedData 加密数据
     * @param iv 加密向量
     * @return 解密后的数据（JSON 字符串）
     */
    String decryptWeChatData(String sessionKey, String encryptedData, String iv);
}



