package com.application.common.cache;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CacheLogger {

    private final CacheManager cacheManager;

    @Scheduled(fixedRate = 60 * 1000L)
    public void logCacheStats(){
        for (String cacheName : cacheManager.getCacheNames()) {
            CaffeineCache cache = (CaffeineCache) cacheManager.getCache(cacheName);

            if(cache != null){
                com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = cache.getNativeCache();

                CacheStats stats = nativeCache.stats();
                log.info("[CACHE STATS] mappingTastes => hits: {}, misses: {}, loadSuccess: {}",
                        stats.hitCount(), stats.missCount(), stats.loadSuccessCount());
            }

        }
    }

}
