package org.idea.qiyu.live.framework.redis.starter.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@ConditionalOnClass(RedisTemplate.class)
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        //使用自定义的 Jackson 序列化器，将对象序列化为 JSON。
        IGenericJackson2JsonRedisSerializer valueSerializer = new IGenericJackson2JsonRedisSerializer();
        //使用字符串序列化器，将 Redis 的键序列化为字符串格式。
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        //设置 Redis 键的序列化方式为字符串。
        redisTemplate.setKeySerializer(stringRedisSerializer);
        //设置 Redis 值的序列化方式为 JSON。
        redisTemplate.setValueSerializer(valueSerializer);
        //设置 Redis 哈希类型（hash map）中 key 的序列化方式为字符串。
        redisTemplate.setHashKeySerializer(stringRedisSerializer);
        //设置 Redis 哈希类型中 value 的序列化方式为 JSON。
        redisTemplate.setHashValueSerializer(valueSerializer);
        //初始化方法，确保 RedisTemplate 的所有属性都设置完成。
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }
}
