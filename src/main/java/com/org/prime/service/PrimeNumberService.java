package com.org.prime.service;

import com.org.prime.model.PrimeNumberResponse;

@FunctionalInterface
public interface PrimeNumberService {
    PrimeNumberResponse getPrimes(Long number);
}
