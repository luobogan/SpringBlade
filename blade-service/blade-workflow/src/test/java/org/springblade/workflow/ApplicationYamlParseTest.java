package org.springblade.workflow;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 主配置可解析性守卫（2026-11-18，§9.16 启动阻断修复后补）。
 *
 * <p>`application.yml` 的缩进/父键错误（如 S4 wakeup 配置缺 `wakeup:` 父键）在 spring 启动期才抛
 * snakeyaml 异常，导致整个服务不可用。本测试在单测阶段 load 一遍主配置，把阻断前移到 CI。</p>
 */
class ApplicationYamlParseTest {

    @Test
    void applicationYml_可被Spring解析() {
        assertThatCode(() -> {
            YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
            List<PropertySource<?>> sources = loader.load("application",
                new ClassPathResource("application.yml"));
            if (sources == null || sources.isEmpty()) {
                throw new IllegalStateException("application.yml 解析结果为空");
            }
        }).doesNotThrowAnyException();
    }
}
