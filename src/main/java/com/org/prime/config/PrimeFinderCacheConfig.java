package com.org.prime.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.validation.annotation.Validated;

@Validated
@Getter
@Setter
public class PrimeFinderCacheConfig {
    private int bucketSize;
    private int maximumEntries;
    private int expireAfterAccessMinutes;
}
