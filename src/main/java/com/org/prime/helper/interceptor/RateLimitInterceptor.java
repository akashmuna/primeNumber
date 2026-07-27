package com.org.prime.helper.interceptor;

import com.github.benmanes.caffeine.cache.Cache;
import com.org.prime.annotations.RateLimited;
import com.org.prime.config.RateLimitProperties;
import com.org.prime.exception.RateLimitExceededException;
import com.org.prime.helper.ratelimit.RateLimitIdentityResolver;
import com.org.prime.model.ratelimit.RateLimitIdentity;
import com.org.prime.model.ratelimit.RateLimitKey;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Enforces identity-aware token-bucket limits on annotated controller methods.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final String LIMIT_HEADER = "X-RateLimit-Limit";
    private static final String REMAINING_HEADER = "X-RateLimit-Remaining";
    private static final String RETRY_AFTER_HEADER = "Retry-After";

    private final RateLimitProperties properties;
    private final RateLimitIdentityResolver identityResolver;
    private final Cache<RateLimitKey, Bucket> rateLimitBuckets;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

        if (!properties.isEnabled() || !(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RateLimited rateLimited = findRateLimit(handlerMethod);

        if (rateLimited == null) {
            return true;
        }

        RateLimitIdentity identity = identityResolver.resolve(request);
        RateLimitKey key = new RateLimitKey(rateLimited.scope(), identity.type(), identity.value());

        Bucket bucket = rateLimitBuckets.get(key, ignored -> createBucket());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        response.setHeader(LIMIT_HEADER, Long.toString(properties.getCapacity()));
        response.setHeader(REMAINING_HEADER, Long.toString(probe.getRemainingTokens()));

        if (probe.isConsumed()) {
            return true;
        }

        long retryAfterSeconds = toCeilingSeconds(probe.getNanosToWaitForRefill());
        response.setHeader(RETRY_AFTER_HEADER, Long.toString(retryAfterSeconds));

        log.info(
                "Rate limit exceeded: scope={}, identityType={}, retryAfterSeconds={}",
                rateLimited.scope(),
                identity.type(),
                retryAfterSeconds
        );

        throw new RateLimitExceededException(retryAfterSeconds);
    }

    private Bucket createBucket() {
        return Bucket.builder()
                .addLimit(limit -> limit
                        .capacity(properties.getCapacity())
                        .refillGreedy(
                                properties.getRefillTokens(),
                                properties.getRefillPeriod()
                        ))
                .build();
    }

    private RateLimited findRateLimit(HandlerMethod handlerMethod) {
        RateLimited methodAnnotation =
                AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), RateLimited.class);

        if (methodAnnotation != null) {
            return methodAnnotation;
        }

        return AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), RateLimited.class);
    }

    private long toCeilingSeconds(long nanoseconds) {
        if (nanoseconds <= 0) {
            return 1L;
        }

        return (nanoseconds - 1L) / 1_000_000_000L + 1L;
    }
}
