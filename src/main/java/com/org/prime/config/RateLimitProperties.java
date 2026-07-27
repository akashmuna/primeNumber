package com.org.prime.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Token-bucket rate-limit configuration.
 * <p>
 *  enabled: whether rate limiting is enabled
 *  capacity: maximum number of tokens in each bucket
 *  refillTokens: tokens added during each refill period
 *  refillPeriod: duration of one refill period
 *  cacheMaximumEntries: maximum number of identity buckets
 *  cacheExpireAfterAccess: expiry for inactive identity buckets
 * </p>
 */
@ConfigurationProperties(prefix = "prime.rate-limit")
@Configuration
@Getter
@Setter
public class RateLimitProperties {

    private boolean enabled;
    private long capacity;
    private long refillTokens;
    private Duration refillPeriod;
    private long cacheMaximumEntries;
    private Duration cacheExpireAfterAccess;
}
