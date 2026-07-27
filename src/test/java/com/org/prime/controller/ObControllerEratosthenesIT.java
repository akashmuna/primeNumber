package com.org.prime.controller;

import com.org.prime.filters.TraceIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ObControllerEratosthenesIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnAllPrimeNumberAsJson() throws Exception {
        mockMvc.perform(
                        get("/primes/{number}", 11)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.initial").value(11))
                .andExpect(jsonPath("$.primes").isArray())
                .andExpect(jsonPath("$.primes", contains(2, 3, 5, 7, 11)));
    }

    @Test
    void shouldReturnAllPrimeNumberAsXML() throws Exception {
        mockMvc.perform(
                        get("/primes/{number}", 11)
                                .accept(MediaType.APPLICATION_XML)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(xpath("count(/PrimeNumberResponse/primes)").number(5.0))
                .andExpect(xpath("/PrimeNumberResponse/primes[1]").string("2"))
                .andExpect(xpath("/PrimeNumberResponse/primes[5]").string("11"));
    }


    @Test
    void shouldReturnInvalidArgsExceptionWhenInputIsInvalid() throws Exception {
        var numberPassed = "abc";
        mockMvc.perform(
                        get("/primes/{number}", numberPassed)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(String.format("Invalid value '%s' for parameter 'number'; expected Long", numberPassed)))
                .andExpect(jsonPath("$.path").value(String.format("/primes/%s", numberPassed)));
    }

    @Test
    void shouldReturnBadRequestWhenNumberViolatesMinimum() throws Exception {
        long numberPassed = 1L;

        mockMvc.perform(
                        get("/primes/{number}", numberPassed)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().is4xxClientError())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").value("must be greater than or equal to 2")
                )
                .andExpect(jsonPath("$.path").value("/primes/1"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldReturnBadRequestWhenNumberViolatesMaximum() throws Exception {
        long numberPassed = 100_00_001L;

        mockMvc.perform(
                        get("/primes/{number}", numberPassed)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().is4xxClientError())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").value("must be less than or equal to 10000000")
                )
                .andExpect(jsonPath("$.path").value("/primes/10000001"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldIncludeTraceIdInErrorResponse() throws Exception {
        String traceId = "prime-api-it-123";

        mockMvc.perform(
                        get("/primes/{number}", "abc")
                                .header(TraceIdFilter.TRACE_ID_HEADER, traceId)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isBadRequest())
                .andExpect(header().string(
                        TraceIdFilter.TRACE_ID_HEADER,
                        traceId
                ))
                .andExpect(jsonPath("$.traceId").value(traceId))
                .andExpect(jsonPath("$.status").value(400));
    }

}
