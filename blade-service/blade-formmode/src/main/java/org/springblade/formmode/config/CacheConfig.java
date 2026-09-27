package org.springblade.formmode.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * 缓存配置
 *
 * 替换 ecology 的 ModeCacheManager 本地缓存
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager formmodeCacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(1))
            .prefixCacheNameWith("formmode:")
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            // 注：Spring Data Redis（本项目 Spring Boot 4.1.0）已将
            // GenericJackson2JsonRedisSerializer 标记为 @Deprecated(forRemoval=true)。
            // 改用官方推荐入口 RedisSerializer.json()：JSON 行为等价（同样写入 @class 类型信息，
            // 已缓存旧数据不受影响），且将来底层实现被替换时本类无需再改。
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.json()))
            .disableCachingNullValues();

        return RedisCacheManager.builder(factory)
            .cacheDefaults(config)
            .build();
    }

}
