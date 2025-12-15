package com.crudzaso.cityhelp.auth.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis Configuration for CityHelp Auth Service.
 * Configures RedisTemplate with proper serializers for rate limiting.
 *
 * This configuration enables:
 * - Key serialization: String
 * - Value serialization: JSON (Jackson)
 * - Hash key serialization: String
 * - Hash value serialization: JSON (Jackson)
 *
 * @author CityHelp Team
 * @since 1.0.0
 */
@Configuration
public class RedisConfig {

    /**
     * Creates a RedisTemplate bean configured for rate limiting operations.
     * Uses StringRedisSerializer for keys and GenericJackson2JsonRedisSerializer for values.
     *
     * @param connectionFactory the Redis connection factory provided by Spring Boot
     * @return configured RedisTemplate ready for use
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key serializers: Use String serialization for Redis keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Value serializers: Use JSON serialization for complex objects
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        // Initialize template after setting serializers
        template.afterPropertiesSet();

        return template;
    }
}
