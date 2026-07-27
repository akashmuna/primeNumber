package com.org.prime;

import com.org.prime.helper.primenumber.PrimeFinder;
import com.org.prime.service.PrimeNumberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.Objects;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@DisplayName("PrimeService Cache Integration Tests")
class PrimeServiceApplicationTests {

	@Autowired
	private PrimeNumberService primeNumberService;

	@Autowired
	private CacheManager cacheManager;

	//Spy on the PrimeFinder bean to find how many times it is called
	@MockitoSpyBean
	private PrimeFinder primeFinder;

	@BeforeEach
	void setUp(){
		Objects.requireNonNull(cacheManager.getCache("primesCache")).clear();
	}

	@Test
	@DisplayName("Should hit cache on second invocation")
	void testPrimeNumbersCachingBehavior() {
		Long testingNumber = 2000L;

		//First Call - Cache Miss
		primeNumberService.getPrimes(testingNumber);
		verify(primeFinder).getAllPrimeNumbersUpTo(testingNumber);

		//Second Call - Cache Hit the Call stays at 1
		primeNumberService.getPrimes(testingNumber);
		verify(primeFinder, times(1)).getAllPrimeNumbersUpTo(testingNumber);

		//Third Call - Cache Hit the Call still stays at 1
		primeNumberService.getPrimes(testingNumber);
		verify(primeFinder, times(1)).getAllPrimeNumbersUpTo(testingNumber);

	}

}
