package com.org.prime.helper.primenumber.impl;

import com.org.prime.config.PrimeFinderConfig;
import com.org.prime.helper.primenumber.PrimeFinder;
import com.org.prime.util.NumberUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Pluggable Injection for PrimeFinder Service based on Conditional Property.
 * By Default Eratosthenes Algorithm is used if Nothing is passed
 * Finds all prime numbers up to an inclusive upper limit using the
 * Sieve of Atkin algorithm.
 *
 * <p>Complete searches use Atkin's quadratic forms. Incremental extensions use
 * the shared segmented sieve because the standard Atkin sieve cannot begin
 * directly at an arbitrary lower boundary.</p>
 *
 * <p>The algorithm identifies possible prime numbers by applying three
 * quadratic equations and toggling entries according to their remainder
 * modulo {@code 12}. It then removes multiples of prime squares.</p>
 *
 * <p>The implementation uses an array proportional to the requested limit.
 * Consequently, the configured maximum limit is validated before the array is
 * allocated. The quadratic-form stage of this implementation performs
 * approximately {@code O(n)} iterations and uses {@code O(n)} additional
 * memory.</p>
 *
 * <p>Calculation start time, completion time, and elapsed duration are logged
 * at INFO level. Detailed calculation information is logged at DEBUG level.</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "prime.finder.algorithm", havingValue = "atkin")
@RequiredArgsConstructor
public class AtkinPrimeFinder implements PrimeFinder {

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

        log.info("Atkin calculation started: number={}", number);

        try {
            if (NumberUtils.isLessThanFirstPrime(number)) {
                if (log.isDebugEnabled()) {
                    log.debug("Number {} is below the first prime; returning an empty list", number);
                }

                return Collections.emptyList();
            }

            validateLimit(number);

            if (NumberUtils.isSmallestPrime(number)) {
                return List.of(2L);
            }

            int limit = Math.toIntExact(number);
            boolean[] isPrime = new boolean[limit + 1];

            setBasePrimes(isPrime, limit);
            applyQuadraticForms(isPrime, limit);
            eliminateSquareMultiples(isPrime, limit);

            List<Long> primes = NumberUtils.collectPrimes(isPrime, limit);

            if (log.isDebugEnabled()) {
                log.debug("Atkin calculation completed: limit={}, primeCount={}", limit, primes.size());
            }

            return primes;
        } finally {
            logCompletion(number, startNanos);
        }
    }

    /**
     * Explicitly marks the base primes {@code 2} and {@code 3}.
     */
    private void setBasePrimes(boolean[] isPrime, int limit) {
        if (limit >= 2) {
            isPrime[2] = true;
        }

        if (limit >= 3) {
            isPrime[3] = true;
        }
    }

    /**
     * Applies the three quadratic forms used by the Sieve of Atkin.
     */
    private void applyQuadraticForms(boolean[] isPrime, int limit) {
        int sqrtLimit = (int) Math.sqrt(limit);

        for (int x = 1; x <= sqrtLimit; x++) {
            for (int y = 1; y <= sqrtLimit; y++) {
                toggleFirstQuadraticCandidate(isPrime, limit, x, y);
                toggleSecondQuadraticCandidate(isPrime, limit, x, y);
                toggleThirdQuadraticCandidate(isPrime, limit, x, y);
            }
        }
    }

    /**
     * Applies the quadratic form {@code 4x² + y²}.
     */
    private void toggleFirstQuadraticCandidate(boolean[] isPrime, int limit, int x, int y) {
        long candidate = 4L * x * x + (long) y * y;

        if (candidate <= limit && (candidate % 12 == 1 || candidate % 12 == 5)) {
            int index = Math.toIntExact(candidate);
            isPrime[index] = !isPrime[index];
        }
    }

    /**
     * Applies the quadratic form {@code 3x² + y²}.
     */
    private void toggleSecondQuadraticCandidate(boolean[] isPrime, int limit, int x, int y) {
        long candidate = 3L * x * x + (long) y * y;

        if (candidate <= limit && candidate % 12 == 7) {
            int index = Math.toIntExact(candidate);
            isPrime[index] = !isPrime[index];
        }
    }

    /**
     * Applies the quadratic form {@code 3x² - y²}.
     */
    private void toggleThirdQuadraticCandidate(boolean[] isPrime, int limit, int x, int y) {
        long candidate = 3L * x * x - (long) y * y;

        if (x > y && candidate <= limit && candidate % 12 == 11) {
            int index = Math.toIntExact(candidate);
            isPrime[index] = !isPrime[index];
        }
    }

    /**
     * Removes multiples of prime squares.
     */
    private void eliminateSquareMultiples(boolean[] isPrime, int limit) {
        int sqrtLimit = (int) Math.sqrt(limit);

        for (int candidate = 5; candidate <= sqrtLimit; candidate++) {
            if (isPrime[candidate]) {
                int square = candidate * candidate;

                for (int multiple = square; multiple <= limit; multiple += square) {
                    isPrime[multiple] = false;
                }
            }
        }
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
    private void logCompletion(Long number, long startNanos) {
        long durationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);

        log.info("Atkin calculation finished: number={}, durationMs={}", number, durationMillis);
    }
}