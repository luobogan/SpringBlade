package org.springblade.mall.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springblade.mall.config.WeChatConfig;
import org.springblade.mall.service.WeChatService;
import org.springblade.mall.vo.WeChatSessionVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.net.URLEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 微信服务实现类
 */
@Service
public class WeChatServiceImpl implements WeChatService {

    private static final Logger log = LoggerFactory.getLogger(WeChatServiceImpl.class);

    private static final String WECHAT_LOGIN_URL =
        "https://api.weixin.qq.com/sns/jscode2session";

    @Autowired
    private WeChatConfig weChatConfig;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public WeChatSessionVO getSessionByCode(String code) {
        try {
            // 检查配置是否有效
            String appid = weChatConfig.getAppid();
            String secret = weChatConfig.getSecret();

            if (appid == null || appid.isEmpty() || secret == null || secret.isEmpty()) {
                log.error("WeChat config is not properly set. Please check application.yml");
                return null;
            }

            // 构建请求URL
            String url = String.format(
                "%s?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                WECHAT_LOGIN_URL,
                appid,
                secret,
                URLEncoder.encode(code, StandardCharsets.UTF_8)
            );

            log.info("Requesting WeChat login with URL: {}", url.replace(secret, "***"));

            // 发送请求获取响应（以String形式获取）
            String response = restTemplate.getForObject(url, String.class);
            log.debug("WeChat API response: {}", response);

            if (response == null || response.isEmpty()) {
                log.error("WeChat API returned empty response");
                return null;
            }

            // 解析JSON响应
            WeChatSessionVO session = objectMapper.readValue(response, WeChatSessionVO.class);

            // 检查是否有错误
            if (session.getErrcode() != null && session.getErrcode() != 0) {
                log.error("WeChat API error: {} - {}", session.getErrcode(), session.getErrmsg());
                return null;
            }

            // 检查openid是否存在
            if (session.getOpenid() == null || session.getOpenid().isEmpty()) {
                log.error("WeChat API did not return openid");
                return null;
            }

            log.info("WeChat login success, openid: {}", session.getOpenid());
            return session;

        } catch (Exception e) {
            log.error("Failed to get WeChat session: {}", e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public WeChatSessionVO getWeChatSession(String code) {
        return getSessionByCode(code);
    }

    @Override
    public String decryptWeChatData(String sessionKey, String encryptedData, String iv) {
        try {
            // Base64 解码
            byte[] sessionKeyBytes = Base64.getDecoder().decode(sessionKey);
            byte[] encryptedDataBytes = Base64.getDecoder().decode(encryptedData);
            byte[] ivBytes = Base64.getDecoder().decode(iv);

            // 创建密钥和 IV
            SecretKeySpec keySpec = new SecretKeySpec(sessionKeyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

            // AES 解密
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            byte[] decryptedBytes = cipher.doFinal(encryptedDataBytes);
            String decryptedJson = new String(decryptedBytes, StandardCharsets.UTF_8);

            log.debug("Decrypted WeChat data: {}", decryptedJson);
            return decryptedJson;

        } catch (Exception e) {
            log.error("Failed to decrypt WeChat data: {}", e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}



