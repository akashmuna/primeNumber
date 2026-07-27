package com.org.prime.util;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.LongFunction;

import static com.org.prime.util.TestGenerator.getAllPrimeNumber;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SegmentedPrimeRangeSupportUtilTest {

    @Test
    void shouldRejectNullKnownPrimes() {
        LongFunction<List<Long>> generator = mockGenerator();

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> SegmentedPrimeRangeSupportUtil.findPrimes(2L, 10L, null, generator)
        );

        assertEquals("knownPrimes must not be null", exception.getMessage());
    }

    @Test
    void shouldRejectNullBasePrimeGenerator() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> SegmentedPrimeRangeSupportUtil.findPrimes(2L, 10L, List.of(), null)
        );

        assertEquals("basePrimeGenerator must not be null", exception.getMessage());
    }

    @Test
    void shouldReturnEmptyListForReversedRange() {
        LongFunction<List<Long>> generator = mockGenerator();

        List<Long> result =
                SegmentedPrimeRangeSupportUtil.findPrimes(20L, 10L, List.of(), generator);

        assertEquals(List.of(), result);
    }

    @Test
    void shouldReturnEmptyListWhenUpperLimitIsBelowTwo() {
        LongFunction<List<Long>> generator = mockGenerator();

        List<Long> result =
                SegmentedPrimeRangeSupportUtil.findPrimes(-10L, 1L, List.of(), generator);

        assertEquals(List.of(), result);
    }

    @Test
    void shouldReturnTwoForSingleNumberRange() {
        LongFunction<List<Long>> generator = mockGenerator();

        List<Long> result =
                SegmentedPrimeRangeSupportUtil.findPrimes(2L, 2L, List.of(), generator);

        assertEquals(List.of(2L), result);
    }

    @Test
    void shouldUseKnownPrimesWhenTheyCoverBasePrimeLimit() {
        LongFunction<List<Long>> generator = mockGenerator();

        List<Long> knownPrimes = List.of(2L, 3L, 5L, 7L);

        List<Long> result =
                SegmentedPrimeRangeSupportUtil.findPrimes(11L, 20L, knownPrimes, generator);

        assertEquals(List.of(11L, 13L, 17L, 19L), result);
    }

    @Test
    void shouldGenerateBasePrimesWhenKnownRangeIsInsufficient() {
        LongFunction<List<Long>> generator = mockGenerator();

        when(generator.apply(5L)).thenReturn(List.of(2L, 3L, 5L));

        List<Long> result =
                SegmentedPrimeRangeSupportUtil.findPrimes(2L, 30L, List.of(), generator);

        assertEquals(List.of(2L, 3L, 5L, 7L, 11L, 13L, 17L, 19L, 23L, 29L), result);

        verify(generator).apply(5L);
        verifyNoMoreInteractions(generator);
    }

    @Test
    void shouldHandlePrimeWhenNextMultipleIsOutsideSegment() {
        LongFunction<List<Long>> generator = mockGenerator();

        List<Long> knownPrimes =
                List.of(2L, 3L, 5L, 7L);

        List<Long> result =
                SegmentedPrimeRangeSupportUtil.findPrimes(101L, 101L, knownPrimes, generator);

        assertEquals(List.of(101L), result);
        verifyNoInteractions(generator);
    }

    @Test
    void shouldHandleCompositeSingleNumberRange() {
        LongFunction<List<Long>> generator = mockGenerator();

        List<Long> knownPrimes = List.of(2L, 3L, 5L, 7L);

        List<Long> result =
                SegmentedPrimeRangeSupportUtil.findPrimes(100L, 100L, knownPrimes, generator);

        assertEquals(List.of(), result);
    }

    @Test
    void shouldProcessMoreThanOneSegment() {
        LongFunction<List<Long>> generator = mockGenerator();
        List<Long> basePrimes = getAllPrimeNumber(1_048L);

        when(generator.apply(1_048L)).thenReturn(basePrimes);

        List<Long> result =
                SegmentedPrimeRangeSupportUtil.findPrimes(2L, 1_100_000L, List.of(), generator);

        assertFalse(result.isEmpty());
        assertEquals(2L, result.getFirst());
        assertTrue(result.contains(1_000_003L));
        assertFalse(result.contains(1_000_000L));
        assertFalse(result.contains(1_100_000L));

        assertTrue(result.getLast() <= 1_100_000L);

        verify(generator).apply(1_048L);
        verifyNoMoreInteractions(generator);
    }

    @Test
    void shouldReturnAnImmutableResult() {
        LongFunction<List<Long>> generator = mockGenerator();

        when(generator.apply(3L)).thenReturn(List.of(2L, 3L));

        List<Long> result =
                SegmentedPrimeRangeSupportUtil.findPrimes(2L, 10L, List.of(), generator);

        assertEquals(List.of(2L, 3L, 5L, 7L), result);

        assertThrows(UnsupportedOperationException.class, () -> result.add(11L));
    }

    @SuppressWarnings("unchecked")
    private LongFunction<List<Long>> mockGenerator() {
        return mock(LongFunction.class);
    }
}
