package com.org.prime.model.ratelimit;

/**
 * Trusted identity used as part of a rate-limit key.
 *
 * @param type identity type
 * @param value stable identity value
 */
public record RateLimitIdentity(
        IdentityType type,
        String value
){}