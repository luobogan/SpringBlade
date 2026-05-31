package org.springblade.formmode.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DynamicTableNameInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springblade.formmode.utils.TableNameContextHolder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis Plus 配置
 *
 * 1. 分页插件
 * 2. 动态表名拦截器（用于 formtable_main_{billid} 动态表）
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 动态表名拦截器
        DynamicTableNameInnerInterceptor dynamicTableInterceptor = new DynamicTableNameInnerInterceptor();
        dynamicTableInterceptor.setTableNameHandler((sql, tableName) -> {
            // 如果表名是动态表占位符，通过 ThreadLocal 获取实际表名
            String actualTableName = TableNameContextHolder.get();
            if (actualTableName != null) {
                return actualTableName;
            }
            return tableName;
        });
        interceptor.addInnerInterceptor(dynamicTableInterceptor);

        // 分页拦截器
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        paginationInterceptor.setOverflow(true);
        paginationInterceptor.setMaxLimit(500L);
        interceptor.addInnerInterceptor(paginationInterceptor);

        return interceptor;
    }

}
