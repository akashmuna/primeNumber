package com.org.prime.service;

import com.org.prime.helper.cache.PrimeBucketCache;
import com.org.prime.model.PrimeNumberResponse;
import com.org.prime.service.impl.PrimeNumberServiceImpl;
import com.org.prime.util.TestGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PrimeNumberServiceTest {

    @Mock
    PrimeBucketCache primeBucketCache;

    @InjectMocks
    PrimeNumberServiceImpl primeNumberService;

    @Test
    public void test_checkPrimeNumber(){
        var number = 10L;
        var listOfAvailablePrimes = Arrays.asList( 2L, 3L, 5L, 7L);
        var allPrimeResponse = new PrimeNumberResponse(number, listOfAvailablePrimes);

        when(primeBucketCache.getPrimesUpTo(number)).thenReturn(TestGenerator.getAllPrimeNumber(number));
        assertEquals(primeNumberService.getPrimes(number), allPrimeResponse);
    }
}
