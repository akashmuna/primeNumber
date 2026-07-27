package com.org.prime.helper.primenumber;

import com.org.prime.util.SegmentedPrimeRangeSupportUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * PrimeFinder is an Interface-only dependency
 * <p>
 *     Any external Library or new Class just implements PrimeFinder directly.
 *     No class Hierarchy Constraints.
 * </p>
 */
public interface PrimeFinder {
    /**
     * Returns all primes through an inclusive upper limit.
     *
     * @param number inclusive upper limit
     * @return sorted prime numbers through the supplied limit
     */
    List<Long> getAllPrimeNumbersUpTo(Long number);

    /**
     * Finds newly discovered primes within an inclusive range using the
     * common segmented-sieve implementation.
     */
    default List<Long> getPrimeNumbersInRange(
            long fromInclusive,
            long toInclusive,
            List<Long> knownPrimes) {

        Logger logger = LoggerFactory.getLogger(getClass());
        String strategyName = getClass().getSimpleName();
        long startNanos = System.nanoTime();

        logger.info("{} range calculation started: from={}, to={}",
                strategyName, fromInclusive, toInclusive);

        try {
            List<Long> primes = SegmentedPrimeRangeSupportUtil.findPrimes(
                    fromInclusive,
                    toInclusive,
                    knownPrimes,
                    this::getAllPrimeNumbersUpTo);

            if (logger.isDebugEnabled()) {
                logger.debug("{} range calculation completed: from={}, to={}, primeCount={}",
                        strategyName, fromInclusive, toInclusive, primes.size());
            }

            return primes;
        } finally {
            long durationMillis =
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);

            logger.info("{} range calculation finished: from={}, to={}, durationMs={}",
                    strategyName, fromInclusive, toInclusive, durationMillis);
        }
    }
}
