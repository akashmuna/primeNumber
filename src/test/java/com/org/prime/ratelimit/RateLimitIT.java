package com.org.prime.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.org.prime.model.ratelimit.RateLimitKey;
import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.security.Principal;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "prime.rate-limit.enabled=true",
                "prime.rate-limit.capacity=2",
                "prime.rate-limit.refill-tokens=1",
                "prime.rate-limit.refill-period=1h",
                "prime.rate-limit.cache-maximum-entries=100",
                "prime.rate-limit.cache-expire-after-access=1h"
        }
)
@AutoConfigureMockMvc
@ActiveProfiles("it")
public class RateLimitIT {

    private static final String PRIME_ENDPOINT =
            "/primes/{number}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Cache<RateLimitKey, Bucket> rateLimitBuckets;

    @BeforeEach
    void clearRateLimitBuckets() {
        rateLimitBuckets.invalidateAll();
        rateLimitBuckets.cleanUp();
    }

    @Test
    void shouldRateLimitPerAuthenticatedIdentityRatherThanOnlyIp()
            throws Exception {

        RequestPostProcessor alice =
                authenticatedUser("alice", "192.0.2.20");

        /*
         * Alice's first request consumes one of two tokens.
         */
        mockMvc.perform(
                        get(PRIME_ENDPOINT, 11L)
                                .with(alice)
                                .accept(APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(header().string("X-RateLimit-Limit", "2"))
                .andExpect(header().string("X-RateLimit-Remaining", "1"));

        /*
         * Alice's second request consumes the final token.
         */
        mockMvc.perform(
                        get(PRIME_ENDPOINT, 13L)
                                .with(alice)
                                .accept(APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(header().string("X-RateLimit-Limit", "2"))
                .andExpect(header().string("X-RateLimit-Remaining", "0"));

        /*
         * Alice's third request is rejected.
         */
        mockMvc.perform(
                        get(PRIME_ENDPOINT, 17L)
                                .with(alice)
                                .accept(APPLICATION_JSON)
                )
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("X-RateLimit-Limit", "2"))
                .andExpect(header().string("X-RateLimit-Remaining", "0"))
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.message")
                        .value("Rate limit exceeded"));

        /*
         * Bob uses the same IP but has a different authenticated identity.
         * He receives a separate token bucket and is not rejected.
         */
        mockMvc.perform(
                        get(PRIME_ENDPOINT, 17L)
                                .with(authenticatedUser(
                                        "bob",
                                        "192.0.2.20"
                                ))
                                .accept(APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(header().string("X-RateLimit-Remaining", "1"));
    }

    private RequestPostProcessor authenticatedUser(String username, String remoteAddress) {
        return request -> {
            Principal principal = () -> username;

            request.setUserPrincipal(principal);
            request.setRemoteAddr(remoteAddress);

            return request;
        };
    }
}
