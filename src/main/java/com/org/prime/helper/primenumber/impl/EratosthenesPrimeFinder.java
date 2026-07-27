package com.org.prime.helper.primenumber.impl;

import com.org.prime.config.PrimeFinderConfig;
import com.org.prime.helper.primenumber.PrimeFinder;
import com.org.prime.util.NumberUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Finds prime numbers using the Sieve of Eratosthenes.
 *
 * <p>Complete searches use a traditional sieve, while incremental range
 * searches use a memory-bounded segmented sieve.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "prime.finder",
        name = "algorithm",
        havingValue = "eratosthenes",
        matchIfMissing = true
)
public class EratosthenesPrimeFinder implements PrimeFinder {

    private final PrimeFinderConfig primeFinderConfig;

    /**
     * Returns all primes from {@code 2} through the supplied inclusive limit.
     *
     * @param number inclusive upper limit
     * @return primes through the supplied limit in ascending order
     */
    @Override
    public List<Long> getAllPrimeNumbersUpTo(Long number) {
        long startNanos = System.nanoTime();

        log.info("Eratosthenes calculation started: number={}", number);

        try {
            if (NumberUtils.isLessThanFirstPrime(number)) {
                if (log.isDebugEnabled()) {
                    log.debug("Number {} is below the first prime; returning an empty list", number);
                }

                return Collections.emptyList();
            }

            validateLimit(number);

            int limit = Math.toIntExact(number);
            boolean[] isPrime = initializeSieveArray(limit);

            applySieveLogic(isPrime, limit);

            List<Long> primes = NumberUtils.collectPrimes(isPrime, limit);

            log.info("Eratosthenes calculation completed: limit={}, primeCount={}", limit, primes.size());

            return primes;

        } finally {
            logCompletion("Eratosthenes calculation", number, startNanos);
        }
    }

    /**
     * Applies the Sieve of Eratosthenes.
     */
    private void applySieveLogic(boolean[] isPrime, int limit) {
        int sqrtLimit = (int) Math.sqrt(limit);

        for (int prime = 2; prime <= sqrtLimit; prime++) {
            if (isPrime[prime]) {
                markMultiplesAsComposite(isPrime, prime, limit);
            }
        }
    }

    /**
     * Marks multiples of a confirmed prime as composite.
     */
    private void markMultiplesAsComposite(boolean[] isPrime, int prime, int limit) {
        for (int multiple = prime * prime; multiple <= limit; multiple += prime) {
            isPrime[multiple] = false;
        }
    }

    /**
     * Creates and initializes the prime-state array.
     */
    private boolean[] initializeSieveArray(int limit) {
        boolean[] isPrime = new boolean[limit + 1];

        Arrays.fill(isPrime, true);
        isPrime[0] = false;
        isPrime[1] = false;

        return isPrime;
    }

    /**
     * Validates an upper limit against the active configuration.
     */
    private void validateLimit(long number) {
        NumberUtils.validateLimit(number, primeFinderConfig.getMAX_LIMIT(), primeFinderConfig.getAlgorithm());
    }

    /**
     * Logs the completion time and duration of a complete calculation.
     */
    private void logCompletion(String operation, Long number, long startNanos) {
        long durationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);

        log.info("{} finished: number={}, durationMs={}", operation, number, durationMillis);
    }
}