package com.org.prime.controller;

import com.org.prime.annotations.RateLimited;
import com.org.prime.api.PrimeNumberApi;
import com.org.prime.model.PrimeNumberResponse;
import com.org.prime.service.PrimeNumberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
public class PrimeSearchController implements PrimeNumberApi {

    private final PrimeNumberService primeNumberService;

    @Override
    @RateLimited(scope = "prime-search")
    public ResponseEntity<PrimeNumberResponse> fetchPrime(Long number) {
        log.info("GET /primes/{} is started", number);
        var primeNumberresponse = primeNumberService.getPrimes(number);
        log.info("GET /primes/{} is completed", number);
        return ResponseEntity.ok(primeNumberresponse);
    }

}
