package org.springblade.order.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("org.springblade.order.mapper")
public class OrderConfiguration {
}
