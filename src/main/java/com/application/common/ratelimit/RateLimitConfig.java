package com.application.common.ratelimit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RateLimitConfig {

    /** 다중 인스턴스로 갈 때만 켠다: onz.ratelimit.redis.enabled=true */
    @Bean
    @ConditionalOnProperty(name = "onz.ratelimit.redis.enabled", havingValue = "true")
    public RateLimiter redisRateLimiter(StringRedisTemplate redis) {
        return new RedisRateLimiter(redis);
    }

    /** 기본값. 단일 EC2 전제. */
    @Bean
    @ConditionalOnMissingBean(RateLimiter.class)
    public RateLimiter inMemoryRateLimiter() {
        return new InMemoryRateLimiter();
    }
}
