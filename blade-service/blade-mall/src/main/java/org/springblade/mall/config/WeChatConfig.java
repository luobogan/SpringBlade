package org.springblade.mall.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 微信小程序配置
 */
@Configuration
@ConfigurationProperties(prefix = "wechat.miniapp")
public class WeChatConfig {
    
    /**
     * 小程序AppID
     */
    private String appid;
    
    /**
     * 小程序AppSecret
     */
    private String secret;

    // Getter 和 Setter 方法
    public String getAppid() {
        return appid;
    }

    public void setAppid(String appid) {
        this.appid = appid;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }
}



