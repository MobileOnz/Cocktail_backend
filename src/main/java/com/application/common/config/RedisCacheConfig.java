//package com.application.common.config;
//
//import com.fasterxml.jackson.annotation.JsonTypeInfo;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.SerializationFeature;
//import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
//import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
//import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
//import org.springframework.cache.CacheManager;
//import org.springframework.cache.annotation.EnableCaching;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Primary;
//import org.springframework.data.redis.cache.RedisCacheConfiguration;
//import org.springframework.data.redis.cache.RedisCacheManager;
//import org.springframework.data.redis.connection.RedisConnectionFactory;
//import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
//import org.springframework.data.redis.serializer.RedisSerializationContext;
//import org.springframework.data.redis.serializer.StringRedisSerializer;
//
//import java.time.Duration;
//
///**
// * Redis 캐시 설정
// *
// * 이슈 #4, #5: Redis 캐싱을 통한 성능 최적화
// * - 태그 조회 API
// * - 칵테일 목록 조회 API
// */
//@Configuration
//@EnableCaching
//public class RedisCacheConfig {
//
//    @Bean
//    @Primary
//    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
//        // 커스텀 ObjectMapper 생성
//        ObjectMapper objectMapper = new ObjectMapper();
//
//        // Java 8 시간 API 지원 (LocalDateTime 등)
//        objectMapper.registerModule(new JavaTimeModule());
//        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
//
//        // 실패하는 속성은 무시 (역직렬화 시 유연성 확보)
//        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
//
//        // 타입 정보를 포함하되, 매우 제한적으로만 사용
//        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
//                .allowIfSubType(Object.class)
//                .build();
//
//        objectMapper.activateDefaultTyping(
//                ptv,
//                ObjectMapper.DefaultTyping.EVERYTHING,
//                JsonTypeInfo.As.PROPERTY
//        );
//
//        // GenericJackson2JsonRedisSerializer 사용
//        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);
//
//        // Redis 캐시 기본 설정
//        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
//                .entryTtl(Duration.ofMinutes(10))  // 기본 TTL: 10분
//                .serializeKeysWith(
//                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
//                )
//                .serializeValuesWith(
//                        RedisSerializationContext.SerializationPair.fromSerializer(serializer)
//                );
//
//        return RedisCacheManager.builder(connectionFactory)
//                .cacheDefaults(cacheConfig)
//                .build();
//    }
//
//}
