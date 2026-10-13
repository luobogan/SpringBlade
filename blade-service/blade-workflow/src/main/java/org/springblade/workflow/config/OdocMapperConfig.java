package org.springblade.workflow.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * 收发文（odoc）模块 Mapper 扫描兜底配置
 *
 * <p>odoc 相关 Mapper 统一放在 {@code org.springblade.workflow.odoc.mapper} 子包下；
 * 显式声明扫描范围，确保无论框架默认 {@code @MapperScan} 是否覆盖子包都能被注册，
 * 与既有 workflow 模块 Mapper 扫描互不冲突。</p>
 */
@Configuration
@MapperScan("org.springblade.workflow.odoc.mapper")
public class OdocMapperConfig {
}
