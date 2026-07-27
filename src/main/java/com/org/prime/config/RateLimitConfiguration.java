package com.org.prime.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.org.prime.model.ratelimit.RateLimitKey;
import io.github.bucket4j.Bucket;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures local Caffeine storage for Bucket4j buckets.
 */
@Configuration
public class RateLimitConfiguration {

    @Bean
    public Cache<RateLimitKey, Bucket> rateLimitBuckets(RateLimitProperties properties) {
        return Caffeine.newBuilder()
                .maximumSize(properties.getCacheMaximumEntries())
                .expireAfterAccess(properties.getCacheExpireAfterAccess())
                .build();
    }
}
