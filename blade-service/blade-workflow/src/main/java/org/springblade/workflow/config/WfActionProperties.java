package org.springblade.workflow.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 「节点前/后附加操作」执行器的安全开关与参数。
 *
 * <p>附加操作支持 {@code http(s)://}、{@code sql:}/{@code dml:}、{@code field:}/{@code set:} 三类前缀。
 * 其中 HTTP 调用与 SQL 执行属高危能力，<b>默认关闭</b>，需显式开启；未开启时按「未配置」跳过，不影响流转。</p>
 *
 * <p>示例（application.yml）：</p>
 * <pre>{@code
 * blade:
 *   workflow:
 *     action:
 *       http-enabled: false      # 是否允许 http(s) 附加操作
 *       sql-enabled: false       # 是否允许 sql/dml 附加操作
 *       http-timeout-ms: 5000
 *       url-whitelist:           # http 前缀白名单（为空表示不限制）
 *         - https://inner.example.com
 * }</pre>
 */
@Data
@Component
@ConfigurationProperties(prefix = "blade.workflow.action")
public class WfActionProperties {

    /** 是否允许执行 http(s) 附加操作（默认关闭） */
    private boolean httpEnabled = false;

    /** 是否允许执行 sql/dml 附加操作（默认关闭） */
    private boolean sqlEnabled = false;

    /** HTTP 连接/读取超时（毫秒） */
    private int httpTimeoutMs = 5000;

    /** HTTP 目标地址白名单前缀（为空表示不限制） */
    private List<String> urlWhitelist = new ArrayList<>();

}
