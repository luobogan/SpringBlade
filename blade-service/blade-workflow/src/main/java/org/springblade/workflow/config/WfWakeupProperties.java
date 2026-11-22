package org.springblade.workflow.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * S4 外部唤醒入口配置（2026-11-18）。
 *
 * <p>外部入口默认关闭；启用前必须配置 secret 并在网关 skip-url / xss.skip-url 放行路径。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "blade.workflow.wakeup")
public class WfWakeupProperties {

    /** 外部 permitAll 入口开关（默认关，零行为变化） */
    private boolean externalEnabled = false;

    /** HMAC-SHA256 共享密钥（external-enabled=true 时必填） */
    private String secret = "";
}
