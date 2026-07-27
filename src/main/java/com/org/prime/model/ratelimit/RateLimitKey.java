package com.org.prime.model.ratelimit;

/**
 * Caffeine key for one token bucket.
 *
 * @param scope protected API operation
 * @param identityType identity classification
 * @param identity stable identity value
 */
public record RateLimitKey(
        String scope,
        IdentityType identityType,
        String identity
) {}
