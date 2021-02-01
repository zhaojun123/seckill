package com.seckill;

import com.seckill.filter.RedisSecKillBloomFilter;
import com.seckill.filter.SecKillBloomFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class Config {

    @Bean
    @ConditionalOnProperty(value = "secKill.ram.bloom.enable",havingValue = "true")
    public FilterRegistrationBean secKillRamBloomFilter(){
        SecKillBloomFilter secKillBloomFilter = new SecKillBloomFilter();
        FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean();
        filterRegistrationBean.setFilter(secKillBloomFilter);
        filterRegistrationBean.addUrlPatterns("/secKill/ramSecKill");
        return filterRegistrationBean;
    }

    @Bean
    @ConditionalOnProperty(value = "secKill.redis.bloom.enable",havingValue = "true")
    public RedisSecKillBloomFilter redisSecKillBloomFilter(){
        return new RedisSecKillBloomFilter();
    }

    @Bean
    @ConditionalOnProperty(value = "secKill.redis.bloom.enable",havingValue = "true")
    public FilterRegistrationBean secKillRedisBloomFilter(){
        FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean();
        filterRegistrationBean.setFilter(redisSecKillBloomFilter());
        filterRegistrationBean.addUrlPatterns("/secKill/redisSecKill");
        return filterRegistrationBean;
    }

    @Bean
    public RedisTemplate redisTemplate(RedisConnectionFactory factory){
        RedisTemplate redisTemplate = new RedisTemplate();
        RedisSerializer stringSerializer = new StringRedisSerializer();
        redisTemplate.setConnectionFactory(factory);
        redisTemplate.setKeySerializer(stringSerializer);
        redisTemplate.setValueSerializer(stringSerializer);
        redisTemplate.setHashKeySerializer(stringSerializer);
        redisTemplate.setHashValueSerializer(stringSerializer);
        return redisTemplate;
    }


}
