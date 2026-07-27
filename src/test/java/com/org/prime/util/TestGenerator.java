package com.org.prime.util;

import com.google.common.math.LongMath;
import com.org.prime.model.PrimeNumberResponse;

import java.util.List;
import java.util.stream.LongStream;

public class TestGenerator {

    public static PrimeNumberResponse generateResponse(Long number){
        return new PrimeNumberResponse().initial(number).primes(getAllPrimeNumber(number));
    }

    public static List<Long> getAllPrimeNumber(Long number){
        return LongStream.rangeClosed(2, number)
                .filter(LongMath::isPrime)
                .boxed()
                .toList();
    }
}
