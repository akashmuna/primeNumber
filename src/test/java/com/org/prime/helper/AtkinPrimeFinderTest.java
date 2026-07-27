package com.org.prime.helper;

import com.org.prime.config.PrimeFinderConfig;
import com.org.prime.helper.primenumber.PrimeFinder;
import com.org.prime.helper.primenumber.impl.AtkinPrimeFinder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AtkinPrimeFinderTest {

    @Mock
    private PrimeFinderConfig primeFinderConfig;

    @InjectMocks
    private AtkinPrimeFinder primeFinder;

    @Test
    void shouldReturnEmptyListWhenNumberIsBelowTwo() {
        List<Long> result = primeFinder.getAllPrimeNumbersUpTo(1L);

        assertEquals(List.of(), result);
        verifyNoInteractions(primeFinderConfig);
    }

    @Test
    void shouldReturnOnlyTwoWhenLimitIsTwo() {
        configureFinder(100L, "atkin");

        List<Long> result = primeFinder.getAllPrimeNumbersUpTo(2L);

        assertEquals(List.of(2L), result);
    }

    @Test
    void shouldFindAllPrimesUpToOneHundred() {
        configureFinder(100L, "atkin");

        List<Long> result = primeFinder.getAllPrimeNumbersUpTo(100L);

        assertEquals(
                List.of(
                        2L, 3L, 5L, 7L, 11L, 13L, 17L, 19L,
                        23L, 29L, 31L, 37L, 41L, 43L, 47L,
                        53L, 59L, 61L, 67L, 71L, 73L, 79L,
                        83L, 89L, 97L
                ),
                result
        );
    }

    @Test
    void shouldRemovePrimeSquareMultiples() {
        configureFinder(100L, "atkin");

        List<Long> result = primeFinder.getAllPrimeNumbersUpTo(49L);

        assertEquals(
                List.of(
                        2L, 3L, 5L, 7L, 11L, 13L, 17L, 19L,
                        23L, 29L, 31L, 37L, 41L, 43L, 47L
                ),
                result
        );
    }

    @Test
    void shouldFindOnlyNewPrimesInsideRange() {
        configureFinder(1_000L, "atkin");

        List<Long> knownPrimes = primeFinder.getAllPrimeNumbersUpTo(100L);

        List<Long> result = primeFinder.getPrimeNumbersInRange(101L, 130L, knownPrimes);

        assertEquals(List.of(101L, 103L, 107L, 109L, 113L, 127L), result);
    }

    @Test
    void shouldGenerateMissingBasePrimesForRange() {
        configureFinder(1_000L, "atkin");

        List<Long> result = primeFinder.getPrimeNumbersInRange(2L, 30L, List.of());

        assertEquals(
                List.of(2L, 3L, 5L, 7L, 11L, 13L, 17L, 19L, 23L, 29L),
                result
        );
    }

    @Test
    void shouldReturnEmptyListForReversedRange() {
        configureFinder(1_000L, "atkin");

        List<Long> result = primeFinder.getPrimeNumbersInRange(50L, 30L, List.of());

        assertEquals(List.of(), result);
    }

    @Test
    void shouldRejectNumberAboveConfiguredLimit() {
        configureFinder(100L, "atkin");

        assertThrows(
                IllegalArgumentException.class,
                () -> primeFinder.getAllPrimeNumbersUpTo(101L)
        );
    }

    @Test
    void shouldUseDefaultRangeCalculationForNewAdapter() {
        PrimeFinder libraryAdapter =
                number -> LongStream.rangeClosed(2L, number)
                        .filter(candidate -> BigInteger.valueOf(candidate)
                                .isProbablePrime(20))
                        .boxed()
                        .toList();

        List<Long> primes = libraryAdapter.getPrimeNumbersInRange(
                11L,
                30L,
                List.of(2L, 3L, 5L, 7L)
        );

        assertEquals(List.of(11L, 13L, 17L, 19L, 23L, 29L), primes);
    }

    private void configureFinder(long maximumLimit, String algorithm) {
        lenient().when(primeFinderConfig.getMAX_LIMIT()).thenReturn(maximumLimit);
        lenient().when(primeFinderConfig.getAlgorithm()).thenReturn(algorithm);
    }
}