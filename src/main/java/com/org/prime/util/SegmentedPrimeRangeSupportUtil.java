package com.org.prime.util;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.LongFunction;

/**
 * Shared segmented-sieve implementation for incremental prime-range searches.
 */
@UtilityClass
public class SegmentedPrimeRangeSupportUtil {

    private static final int SEGMENT_SIZE = 1_000_000;

    /**
     * Finds primes inside an inclusive range.
     *
     * @param fromInclusive first number to examine
     * @param toInclusive last number to examine
     * @param knownPrimes sorted primes already known below the range
     * @param basePrimeGenerator function capable of generating missing base primes
     * @return newly discovered primes in ascending order
     */
    public static List<Long> findPrimes(
            long fromInclusive,
            long toInclusive,
            List<Long> knownPrimes,
            LongFunction<List<Long>> basePrimeGenerator
    ) {
        Objects.requireNonNull(knownPrimes, "knownPrimes must not be null");
        Objects.requireNonNull(basePrimeGenerator, "basePrimeGenerator must not be null");

        if (fromInclusive > toInclusive || toInclusive < 2) {
            return List.of();
        }

        long effectiveStart = Math.max(2L, fromInclusive);
        long basePrimeLimit = (long) Math.sqrt(toInclusive);
        List<Long> basePrimes = resolveBasePrimes(fromInclusive, basePrimeLimit, knownPrimes, basePrimeGenerator);
        List<Long> discoveredPrimes = new ArrayList<>();

        for (long segmentStart = effectiveStart; segmentStart <= toInclusive;) {
            long segmentEnd = calculateSegmentEnd(segmentStart, toInclusive);
            collectSegmentPrimes(segmentStart, segmentEnd, basePrimes, discoveredPrimes);

            if (segmentEnd == toInclusive) {
                break;
            }

            segmentStart = segmentEnd + 1L;
        }

        return List.copyOf(discoveredPrimes);
    }

    /**
     * Uses cached primes when they cover the required square-root limit.
     * Otherwise, it generates the missing base-prime range.
     */
    private static List<Long> resolveBasePrimes(
            long fromInclusive,
            long basePrimeLimit,
            List<Long> knownPrimes,
            LongFunction<List<Long>> basePrimeGenerator
    ) {
        if (basePrimeLimit < 2) {
            return List.of();
        }

        if (fromInclusive > basePrimeLimit) {
            return knownPrimes.stream().takeWhile(prime -> prime <= basePrimeLimit).toList();
        }

        return basePrimeGenerator.apply(basePrimeLimit);
    }

    /**
     * Calculates the inclusive end of a bounded segment.
     */
    private static long calculateSegmentEnd(long segmentStart, long toInclusive) {
        long maximumSegmentOffset = SEGMENT_SIZE - 1L;

        if (segmentStart > toInclusive - maximumSegmentOffset) {
            return toInclusive;
        }

        return segmentStart + maximumSegmentOffset;
    }

    /**
     * Finds all primes within one segment.
     */
    private static void collectSegmentPrimes(
            long segmentStart,
            long segmentEnd,
            List<Long> basePrimes,
            List<Long> destination
    ) {
        int segmentLength = Math.toIntExact(segmentEnd - segmentStart + 1L);
        boolean[] composite = new boolean[segmentLength];

        for (long prime : basePrimes) {
            if (prime > segmentEnd / prime) {
                break;
            }

            long firstMultiple = firstCompositeMultiple(segmentStart, prime);

            if (firstMultiple <= segmentEnd) {
                markSegmentMultiples(composite, segmentStart, segmentEnd, firstMultiple, prime);
            }
        }

        for (int index = 0; index < composite.length; index++) {
            if (!composite[index]) {
                destination.add(segmentStart + index);
            }
        }
    }

    /**
     * Finds the first relevant multiple of a prime inside a segment.
     */
    private static long firstCompositeMultiple(long segmentStart, long prime) {
        long remainder = segmentStart % prime;
        long alignedMultiple = remainder == 0 ? segmentStart : segmentStart + prime - remainder;
        long primeSquare = prime * prime;

        return Math.max(primeSquare, alignedMultiple);
    }

    /**
     * Marks multiples of one base prime as composite.
     */
    private static void markSegmentMultiples(
            boolean[] composite,
            long segmentStart,
            long segmentEnd,
            long firstMultiple,
            long prime
    ) {
        for (long multiple = firstMultiple; multiple <= segmentEnd;) {
            composite[Math.toIntExact(multiple - segmentStart)] = true;

            if (multiple > segmentEnd - prime) {
                break;
            }

            multiple += prime;
        }
    }
}