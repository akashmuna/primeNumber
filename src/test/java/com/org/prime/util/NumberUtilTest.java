package com.org.prime.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class NumberUtilTest {

    @Test
    public void test_isLessThanFirstPrime(){
        assertTrue(NumberUtils.isLessThanFirstPrime(1));
        assertFalse(NumberUtils.isLessThanFirstPrime(3));
    }

    @Test
    public void test_isSmallestPrime(){
        assertTrue(NumberUtils.isSmallestPrime(2));
        assertFalse(NumberUtils.isSmallestPrime(3));
    }

    @Test
    public void test_isEven(){
        assertTrue(NumberUtils.isEven(12));
        assertFalse(NumberUtils.isEven(15));
    }

    @Test
    public void test_isDivisibleBy(){
        assertTrue(NumberUtils.isDivisibleBy(12L,2L));
        assertFalse(NumberUtils.isDivisibleBy(15L,7L));
    }

    @Test
    public void test_collectPrimes(){
        //Given
        boolean[] isPrime = new boolean[5];
        Arrays.fill(isPrime, true);
        isPrime[0] = false;
        isPrime[1] = false;

        assertEquals(3, NumberUtils.collectPrimes(isPrime, 4).size());
    }

    @Test
    public void test_validateLimit(){
        IllegalArgumentException exception =  assertThrows(
                IllegalArgumentException.class,
                () ->NumberUtils.validateLimit(12L,2L, "algorithm")
        );
        assertTrue(exception.getMessage().contains("exceeds the maximum supported"));
    }
}
