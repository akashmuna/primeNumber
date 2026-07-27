package com.org.prime.service.impl;

import com.org.prime.helper.cache.PrimeBucketCache;
import com.org.prime.model.PrimeNumberResponse;
import com.org.prime.service.PrimeNumberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Prime-number service backed by a bucketed high-watermark cache.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PrimeNumberServiceImpl implements PrimeNumberService {

    private final PrimeBucketCache primeBucketCache;

    /**
     * Returns all prime numbers through the requested inclusive limit.
     *
     * @param number requested upper limit
     * @return response containing the requested limit and its prime numbers
     */
    @Override
    public PrimeNumberResponse getPrimes(Long number) {
        List<Long> primeNumbers = primeBucketCache.getPrimesUpTo(number);

        PrimeNumberResponse response = new PrimeNumberResponse(number, primeNumbers);

        if (log.isDebugEnabled()) {
            log.debug("Generated prime response: number={}, primeCount={}", number, primeNumbers.size());
        }

        return response;
    }
}
