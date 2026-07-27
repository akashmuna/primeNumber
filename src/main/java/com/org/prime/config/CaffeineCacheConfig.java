package com.org.prime.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableCaching
@RequiredArgsConstructor
public class CaffeineCacheConfig {

    private final PrimeFinderConfig primeFinderConfig;

    @Bean
    public CacheManager cacheManager(){
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("primesCache");

        cacheManager.setCaffeine(
                Caffeine.newBuilder()
                        .maximumSize(primeFinderConfig.getPrimeFinderCacheConfig().getMaximumEntries())
                        .expireAfterAccess(Duration.ofMinutes(primeFinderConfig.getPrimeFinderCacheConfig().getExpireAfterAccessMinutes()))
                        .recordStats()
        );
        return cacheManager;
    }
}
