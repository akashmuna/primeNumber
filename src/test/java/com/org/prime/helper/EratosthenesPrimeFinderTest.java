package com.org.prime.helper;

import com.org.prime.config.PrimeFinderConfig;
import com.org.prime.helper.primenumber.impl.EratosthenesPrimeFinder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EratosthenesPrimeFinderTest {

    @Mock
    private PrimeFinderConfig primeFinderConfig;

    @InjectMocks
    private EratosthenesPrimeFinder primeFinder;

    @Test
    void shouldReturnEmptyListWhenNumberIsBelowTwo() {
        List<Long> result = primeFinder.getAllPrimeNumbersUpTo(1L);

        assertEquals(List.of(), result);
        verifyNoInteractions(primeFinderConfig);
    }

    @Test
    void shouldReturnOnlyTwoWhenLimitIsTwo() {
        configureFinder(100L, "eratosthenes");

        List<Long> result = primeFinder.getAllPrimeNumbersUpTo(2L);

        assertEquals(List.of(2L), result);
    }

    @Test
    void shouldFindAllPrimesUpToThirty() {
        configureFinder(100L, "eratosthenes");

        List<Long> result = primeFinder.getAllPrimeNumbersUpTo(30L);

        assertEquals(
                List.of(2L, 3L, 5L, 7L, 11L, 13L, 17L, 19L, 23L, 29L),
                result
        );
    }

    @Test
    void shouldFindPrimesUpToPerfectSquare() {
        configureFinder(100L, "eratosthenes");

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
        configureFinder(1_000L, "eratosthenes");

        List<Long> knownPrimes = primeFinder.getAllPrimeNumbersUpTo(30L);

        List<Long> result = primeFinder.getPrimeNumbersInRange(31L, 50L, knownPrimes);

        assertEquals(List.of(31L, 37L, 41L, 43L, 47L), result);
    }

    @Test
    void shouldGenerateMissingBasePrimesForRange() {
        configureFinder(1_000L, "eratosthenes");

        List<Long> result = primeFinder.getPrimeNumbersInRange(2L, 30L, List.of());

        assertEquals(
                List.of(2L, 3L, 5L, 7L, 11L, 13L, 17L, 19L, 23L, 29L),
                result
        );
    }

    @Test
    void shouldReturnEmptyListForReversedRange() {
        configureFinder(1_000L, "eratosthenes");

        List<Long> result = primeFinder.getPrimeNumbersInRange(50L, 30L, List.of());

        assertEquals(List.of(), result);
    }

    @Test
    void shouldRejectNumberAboveConfiguredLimit() {
        configureFinder(100L, "eratosthenes");

        assertThrows(
                IllegalArgumentException.class,
                () -> primeFinder.getAllPrimeNumbersUpTo(101L)
        );
    }

    private void configureFinder(long maximumLimit, String algorithm) {
        lenient().when(primeFinderConfig.getMAX_LIMIT()).thenReturn(maximumLimit);
        lenient().when(primeFinderConfig.getAlgorithm()).thenReturn(algorithm);
    }
}