package org.springblade.pay.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("org.springblade.pay.mapper")
public class PayConfiguration {
}
