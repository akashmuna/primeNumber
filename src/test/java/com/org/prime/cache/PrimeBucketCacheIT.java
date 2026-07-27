package com.org.prime.cache;

import com.org.prime.helper.primenumber.PrimeFinder;
import com.org.prime.model.PrimeNumberResponse;
import com.org.prime.service.PrimeNumberService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("it")
public class PrimeBucketCacheIT {

    @Autowired
    private PrimeNumberService primeNumberService;

    @MockitoSpyBean
    private PrimeFinder primeFinder;

    @Test
    void shouldCalculateBucketsAndReuseCachedPrimeSnapshots() {
        /*
         * Request 51 maps to bucket 2000.
         * The complete bucket should be calculated once.
         */
        PrimeNumberResponse firstResponse =
                primeNumberService.getPrimes(51L);

        assertPrimeResponse(firstResponse, 51L);
        assertEquals(
                List.of(
                        2L, 3L, 5L, 7L, 11L,
                        13L, 17L, 19L, 23L, 29L,
                        31L, 37L, 41L, 43L, 47L
                ),
                firstResponse.getPrimes()
        );

        verify(primeFinder, times(1))
                .getAllPrimeNumbersUpTo(2000L);

        clearInvocations(primeFinder);

        /*
         * Request 1000 is already covered by the cached 2000 snapshot.
         * The PrimeFinder must not be called.
         */
        PrimeNumberResponse cachedLowerResponse =
                primeNumberService.getPrimes(1000L);

        assertPrimeResponse(cachedLowerResponse, 1000L);
        assertEquals(2L, cachedLowerResponse.getPrimes().getFirst());
        assertEquals(997L, cachedLowerResponse.getPrimes().getLast());
        assertEquals(168, cachedLowerResponse.getPrimes().size());

        verifyNoInteractions(primeFinder);

        /*
         * Request 4500 maps to bucket 6000.
         * Only the missing 2001–6000 range should be calculated.
         */
        PrimeNumberResponse extendedResponse =
                primeNumberService.getPrimes(4500L);

        assertPrimeResponse(extendedResponse, 4500L);
        assertTrue(
                extendedResponse.getPrimes().stream()
                        .allMatch(prime -> prime <= 4500L)
        );

        verify(primeFinder, times(1))
                .getPrimeNumbersInRange(
                        eq(2001L),
                        eq(6000L),
                        anyList()
                );

        verify(primeFinder, never())
                .getAllPrimeNumbersUpTo(6000L);

        clearInvocations(primeFinder);

        /*
         * These requests are all covered by the cached 6000 snapshot.
         */
        PrimeNumberResponse repeatedResponse = primeNumberService.getPrimes(4500L);

        PrimeNumberResponse smallerResponse = primeNumberService.getPrimes(51L);

        PrimeNumberResponse bucketBoundaryResponse = primeNumberService.getPrimes(6000L);

        assertPrimeResponse(repeatedResponse, 4500L);
        assertPrimeResponse(smallerResponse, 51L);
        assertPrimeResponse(bucketBoundaryResponse, 6000L);

        verifyNoInteractions(primeFinder);
    }

    private void assertPrimeResponse(PrimeNumberResponse response, long requestedLimit) {
        assertNotNull(response);
        assertEquals(requestedLimit, response.getInitial());
        assertNotNull(response.getPrimes());

        assertTrue(response.getPrimes()
                .stream()
                .allMatch(prime -> prime <= requestedLimit)
        );
    }
}