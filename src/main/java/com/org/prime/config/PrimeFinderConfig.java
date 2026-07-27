package com.org.prime.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "prime.finder")
@Validated
@Getter
@Setter
public class PrimeFinderConfig {

    private long MAX_LIMIT;
    private String algorithm;
    private PrimeFinderCacheConfig primeFinderCacheConfig;
}
