package com.sky.config;

import com.fasterxml.jackson.databind.ser.std.StringSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;


/**
 * Redis配置类,用于配置Redis相关的Bean
 */
@Configuration
@Slf4j
public class RedisConfiguration {

    @Bean
    public RedisTemplate redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        log.info("开始创建RedisTemplate...");
        RedisTemplate redisTemplate = new RedisTemplate();
        //设置连接工厂
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        //设置key的序列化器为String，防止key在redis数据库中乱码
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        return redisTemplate;
    }
}
