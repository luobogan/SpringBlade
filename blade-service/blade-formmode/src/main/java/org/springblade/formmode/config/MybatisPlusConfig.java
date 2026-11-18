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
            // 只替换「业务动态表」（formtable_main* / formtable_detail*），
            // 绝不能把权限/配置表（moderightinfo / modefieldauthorize / modedatasharerule …）
            // 也替换成业务表名——那会让动态表上下文内的所有权限查询报 Unknown column。
            String actualTableName = TableNameContextHolder.get();
            if (actualTableName != null && tableName != null
                && (tableName.startsWith("formtable_main") || tableName.startsWith("formtable_detail"))) {
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
