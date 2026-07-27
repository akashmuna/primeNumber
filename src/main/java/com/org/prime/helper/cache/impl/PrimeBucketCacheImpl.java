package com.org.prime.helper.cache.impl;

import com.org.prime.config.PrimeFinderConfig;
import com.org.prime.helper.cache.PrimeBucketCache;
import com.org.prime.helper.primenumber.PrimeFinder;
import com.org.prime.model.cache.PrimeSnapshot;
import com.org.prime.util.NumberUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Caffeine-backed, range-aware cache for prime-number snapshots.
 *
 * <p>Each prime-finding algorithm has one cache entry. When a larger bucket is
 * calculated, it replaces the existing lower snapshot because the larger
 * snapshot already contains every prime from the lower snapshot.</p>
 *
 * <p>Requests below the cached limit are served by extracting a prefix of the
 * cached prime list. Requests above the cached limit extend the snapshot only
 * across the missing range.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PrimeBucketCacheImpl implements PrimeBucketCache {

    private final PrimeFinder primeFinder;
    private final PrimeFinderConfig primeFinderConfig;
    private final CacheManager cacheManager;

    private final ReentrantLock extensionLock = new ReentrantLock();

    private static final String CACHE_NAME = "primesCache";

    /**
     * Returns all primes through the requested inclusive limit.
     *
     * @param requestedLimit requested upper limit
     * @return sorted primes through the requested limit
     */
    public List<Long> getPrimesUpTo(Long requestedLimit) {
        Objects.requireNonNull(requestedLimit, "requestedLimit must not be null");

        if (NumberUtils.isLessThanFirstPrime(requestedLimit)) {
            return List.of();
        }

        long maximumLimit = primeFinderConfig.getMAX_LIMIT();
        String cacheKey = String.valueOf(primeFinderConfig.getAlgorithm());

        NumberUtils.validateLimit(requestedLimit, maximumLimit, cacheKey);

        PrimeSnapshot current = getSnapshotCache().get(cacheKey, PrimeSnapshot.class);

        if (current != null && current.coveredThrough() >= requestedLimit) {
            logCacheHit(cacheKey, requestedLimit, current);
            return prefixThrough(current.primes(), requestedLimit);
        }

        return extendAndRead(cacheKey, requestedLimit, maximumLimit);
    }

    /**
     * Extends the current snapshot to the required bucket and stores the new
     * snapshot in Caffeine.
     */
    private List<Long> extendAndRead(String cacheKey, long requestedLimit, long maximumLimit) {
        extensionLock.lock();

        try {
            Cache cache = getSnapshotCache();
            PrimeSnapshot current = cache.get(cacheKey, PrimeSnapshot.class);

            /*
             * Another request may have populated the cache while this request
             * was waiting for the extension lock.
             */
            if (current != null && current.coveredThrough() >= requestedLimit) {
                logCacheHit(cacheKey, requestedLimit, current);
                return prefixThrough(current.primes(), requestedLimit);
            }

            if (current == null) {
                current = PrimeSnapshot.empty();
            }

            long bucketLimit = calculateBucketLimit(requestedLimit, maximumLimit);
            List<Long> allPrimes = extendSnapshot(current, bucketLimit);
            PrimeSnapshot extended = new PrimeSnapshot(bucketLimit, allPrimes);

            log.info(
                    "Updating Caffeine prime cache: key={}, previousLimit={}, "
                            + "newLimit={}, previousPrimeCount={}, newPrimeCount={}",
                    cacheKey,
                    current.coveredThrough(),
                    extended.coveredThrough(),
                    current.primes().size(),
                    extended.primes().size()
            );

            /*
             * Caffeine cache update.
             *
             * The same algorithm key replaces the lower snapshot.
             */
            cache.put(cacheKey, extended);

            return prefixThrough(extended.primes(), requestedLimit);
        } finally {
            extensionLock.unlock();
        }
    }

    /**
     * Extends an existing snapshot or creates the initial snapshot.
     */
    private List<Long> extendSnapshot(PrimeSnapshot current, long bucketLimit) {
        if (current.primes().isEmpty()) {
            return primeFinder.getAllPrimeNumbersUpTo(bucketLimit);
        }

        List<Long> additionalPrimes = primeFinder.getPrimeNumbersInRange(
                current.coveredThrough() + 1L,
                bucketLimit,
                current.primes()
        );

        List<Long> combined = new ArrayList<>(current.primes().size() + additionalPrimes.size());

        combined.addAll(current.primes());
        combined.addAll(additionalPrimes);

        return combined;
    }

    /**
     * Rounds a request up to its bucket boundary without exceeding the
     * configured maximum.
     */
    private long calculateBucketLimit(long requestedLimit, long maximumLimit) {

        var bucketSize = primeFinderConfig.getPrimeFinderCacheConfig().getBucketSize();

        long remainder = requestedLimit % bucketSize;

        if (remainder == 0) {
            return requestedLimit;
        }

        long requiredIncrease = bucketSize - remainder;
        long availableIncrease = maximumLimit - requestedLimit;

        return requestedLimit + Math.min(requiredIncrease, availableIncrease);
    }

    /**
     * Extracts all cached primes less than or equal to the requested limit.
     */
    private List<Long> prefixThrough(List<Long> primes, long requestedLimit) {

        return primes.parallelStream()
                .filter(primeNumb -> primeNumb <= requestedLimit)
                .toList();
    }

    /**
     * Returns the configured Spring cache.
     */
    private Cache getSnapshotCache() {
        Cache cache = cacheManager.getCache(CACHE_NAME);

        if (cache == null) {
            throw new IllegalStateException(
                    String.format("Cache is not configured: %s", CACHE_NAME)
            );
        }

        return cache;
    }

    /**
     * Clears every cached prime snapshot.
     */
    public void evictAll() {
        getSnapshotCache().clear();
    }

    private void logCacheHit(String cacheKey, long requestedLimit, PrimeSnapshot snapshot) {
        log.info(
                "Prime cache hit: key={}, requestedLimit={}, cachedLimit={}, primeCount={}",
                cacheKey,
                requestedLimit,
                snapshot.coveredThrough(),
                snapshot.primes().size()
        );
    }
}