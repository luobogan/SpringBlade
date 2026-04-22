package org.springblade.mall.config;

import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import com.wechat.pay.java.service.refund.RefundService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 微信支付配置类
 * 
 * @author youpinmall
 * @date 2026-03-05
 */
@Configuration
@RequiredArgsConstructor
public class WeChatPayConfig {

    private static final Logger log = LoggerFactory.getLogger(WeChatPayConfig.class);

    private final WeChatPayProperties properties;

    /**
     * 创建微信支付配置
     * 
     * @return Config
     */
    @Bean
    @ConditionalOnProperty(prefix = "wechat.pay", name = "enabled", havingValue = "true", matchIfMissing = false)
    public Config wechatPayConfig() {
        log.info("初始化微信支付配置");
        
        // 验证配置
        validateConfig();
        
        try {
            // 使用 RSAAutoCertificateConfig 自动管理证书
            Config config = new RSAAutoCertificateConfig.Builder()
                .merchantId(properties.getMchId())
                .privateKeyFromPath(properties.getPrivateKeyPath())
                .merchantSerialNumber(properties.getMchSerialNo())
                .apiV3Key(properties.getApiV3Key())
                .build();
            
            log.info("微信支付配置初始化成功");
            return config;
        } catch (Exception e) {
            log.error("微信支付配置初始化失败：{}", e.getMessage(), e);
            throw new RuntimeException("微信支付配置初始化失败：" + e.getMessage(), e);
        }
    }

    /**
     * 验证微信支付配置
     */
    private void validateConfig() {
        if (properties.getMchId() == null || properties.getMchId().isEmpty()) {
            throw new IllegalArgumentException("微信支付配置错误：商户号 (mchId) 不能为空");
        }
        if (properties.getMchSerialNo() == null || properties.getMchSerialNo().isEmpty()) {
            throw new IllegalArgumentException("微信支付配置错误：商户证书序列号 (mchSerialNo) 不能为空");
        }
        if (properties.getApiV3Key() == null || properties.getApiV3Key().isEmpty()) {
            throw new IllegalArgumentException("微信支付配置错误：APIv3 密钥 (apiV3Key) 不能为空");
        }
        if (properties.getPrivateKeyPath() == null || properties.getPrivateKeyPath().isEmpty()) {
            throw new IllegalArgumentException("微信支付配置错误：私钥路径 (privateKeyPath) 不能为空");
        }
        if (properties.getAppid() == null || properties.getAppid().isEmpty()) {
            throw new IllegalArgumentException("微信支付配置错误：小程序 AppID(appid) 不能为空");
        }
        
        log.info("微信支付配置验证通过");
    }

    /**
     * 创建微信支付 JSAPI 服务扩展
     * 
     * @return JsapiServiceExtension
     */
    @Bean
    @ConditionalOnProperty(prefix = "wechat.pay", name = "enabled", havingValue = "true", matchIfMissing = false)
    public JsapiServiceExtension jsapiServiceExtension() {
        log.info("初始化微信支付 JSAPI 服务");
        
        JsapiServiceExtension service = new JsapiServiceExtension.Builder()
            .config(wechatPayConfig())
            .build();
        
        log.info("微信支付 JSAPI 服务初始化成功");
        return service;
    }

    /**
     * 创建微信支付 Native 服务
     * 
     * @return NativePayService
     */
    @Bean
    @ConditionalOnProperty(prefix = "wechat.pay", name = "enabled", havingValue = "true", matchIfMissing = false)
    public NativePayService nativePayService() {
        log.info("初始化微信支付 Native 服务");
        
        NativePayService service = new NativePayService.Builder()
            .config(wechatPayConfig())
            .build();
        
        log.info("微信支付 Native 服务初始化成功");
        return service;
    }

    /**
     * 创建微信支付退款服务
     * 
     * @return RefundService
     */
    @Bean
    @ConditionalOnProperty(prefix = "wechat.pay", name = "enabled", havingValue = "true", matchIfMissing = false)
    public RefundService refundService() {
        log.info("初始化微信支付退款服务");
        
        RefundService service = new RefundService.Builder()
            .config(wechatPayConfig())
            .build();
        
        log.info("微信支付退款服务初始化成功");
        return service;
    }

    // Getter 方法
    public String getAppid() {
        return properties.getAppid();
    }

    public String getMchId() {
        return properties.getMchId();
    }

    public String getNotifyUrl() {
        return properties.getNotifyUrl();
    }

    public String getApiV3Key() {
        return properties.getApiV3Key();
    }
}



