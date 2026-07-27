package com.org.prime.model.cache;

import java.util.List;

/**
 * Immutable snapshot containing all prime numbers through a calculated limit.
 *
 * @param coveredThrough inclusive upper limit covered by this snapshot
 * @param primes sorted prime numbers through the covered limit
 */
public record PrimeSnapshot(long coveredThrough, List<Long> primes) {
    public PrimeSnapshot {
        primes = List.copyOf(primes);
    }

    public static PrimeSnapshot empty() {
        return new PrimeSnapshot(1L, List.of());
    }
}
