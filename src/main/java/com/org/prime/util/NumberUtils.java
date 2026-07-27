package com.org.prime.util;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility Class providing re-usable Math and prime domain level checks
 * <p>
 *     Standardized checks are encapsulated here to promote re-usability
 *     across future features (e.g, direct single-number prime validation endpoints)
 *     and to express domain intent clearly
 * </p>
 */
@UtilityClass
public class NumberUtils {

    public static boolean isLessThanFirstPrime(long number) {
        return number < 2;
    }

    public static boolean isSmallestPrime(long number) {
        return number == 2;
    }

    public static boolean isEven(long number) {
        return number % 2 == 0;
    }

    public static boolean isDivisibleBy(long number, long divisor) {
        return number % divisor == 0;
    }

    public void validateLimit(Long number, long maxAllowedLimit, String algorithm) {
        if (number > maxAllowedLimit) {
            throw new IllegalArgumentException(
                    "Requested limit %d exceeds the maximum supported limit %d for algorithm %s"
                            .formatted(number, maxAllowedLimit, algorithm)
            );
        }
    }

    public static List<Long> collectPrimes(boolean[] isPrime, int limit) {
        List<Long> primesList = new ArrayList<>();
        for (int i = 2; i <= limit ; i++){
            if (isPrime[i]) primesList.add((long) i);
        }
        return primesList;
    }
}
