package com.org.prime.exception;

import com.org.prime.filters.TraceIdFilter;
import com.org.prime.model.ErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
        import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void shouldHandleMethodArgumentTypeMismatchWithExpectedType() {
        WebRequest request = servletRequest(
                "/primes/abc",
                "test-trace-id"
        );

        MethodArgumentTypeMismatchException exception =
                new MethodArgumentTypeMismatchException(
                        "abc",
                        Long.class,
                        "number",
                        mock(MethodParameter.class),
                        new NumberFormatException("Invalid number")
                );

        ResponseEntityResult result = responseResult(
                Objects.requireNonNull(exceptionHandler.handleTypeMismatch(
                        exception,
                        HttpHeaders.EMPTY,
                        HttpStatus.BAD_REQUEST,
                        request
                ))
        );

        assertEquals(HttpStatus.BAD_REQUEST, result.status());
        assertEquals(400, result.body().getStatus());
        assertEquals(
                "Invalid value 'abc' for parameter 'number'; expected Long",
                result.body().getMessage()
        );
        assertEquals("/primes/abc", result.body().getPath());
        assertEquals("test-trace-id", result.body().getTraceId());
        assertNotNull(result.body().getTimestamp());
    }

    @Test
    void shouldHandleMethodArgumentTypeMismatchWithoutExpectedType() {
        MethodArgumentTypeMismatchException exception =
                new MethodArgumentTypeMismatchException(
                        "abc",
                        null,
                        "number",
                        mock(MethodParameter.class),
                        new NumberFormatException("Invalid number")
                );

        ResponseEntityResult result = responseResult(
                Objects.requireNonNull(exceptionHandler.handleTypeMismatch(
                        exception,
                        HttpHeaders.EMPTY,
                        HttpStatus.BAD_REQUEST,
                        servletRequest("/primes/abc", null)
                ))
        );

        assertEquals(HttpStatus.BAD_REQUEST, result.status());
        assertEquals(
                "Invalid value 'abc' for parameter 'number'; "
                        + "expected the required type",
                result.body().getMessage()
        );
        assertNull(result.body().getTraceId());
    }

    @Test
    void shouldHandleGenericTypeMismatch() {
        TypeMismatchException exception =
                new TypeMismatchException("abc", Long.class);

        WebRequest nonServletRequest = mock(WebRequest.class);

        ResponseEntityResult result = responseResult(
                Objects.requireNonNull(exceptionHandler.handleTypeMismatch(
                        exception,
                        HttpHeaders.EMPTY,
                        HttpStatus.BAD_REQUEST,
                        nonServletRequest
                ))
        );

        assertEquals(HttpStatus.BAD_REQUEST, result.status());
        assertEquals("Invalid request parameter", result.body().getMessage());
        assertEquals("", result.body().getPath());
        assertNull(result.body().getTraceId());
    }

    @Test
    void shouldReturnEmptyBodyForNotAcceptableResponse() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Test-Header", "test-value");

        var response = exceptionHandler.handleExceptionInternal(
                new RuntimeException("Not acceptable"),
                null,
                headers,
                HttpStatus.NOT_ACCEPTABLE,
                servletRequest("/primes/11", null)
        );

        assert response != null;
        assertEquals(HttpStatus.NOT_ACCEPTABLE, response.getStatusCode());
        assertNull(response.getBody());
        assertEquals("test-value", response.getHeaders().getFirst("X-Test-Header"));
    }

    @Test
    void shouldHandleKnownClientError() {
        ResponseEntityResult result = responseResult(
                Objects.requireNonNull(exceptionHandler.handleExceptionInternal(
                        new RuntimeException("Not found"),
                        null,
                        HttpHeaders.EMPTY,
                        HttpStatus.NOT_FOUND,
                        servletRequest("/missing", null)
                ))
        );

        assertEquals(HttpStatus.NOT_FOUND, result.status());
        assertEquals(404, result.body().getStatus());
        assertEquals("Not Found", result.body().getMessage());
        assertEquals("/missing", result.body().getPath());
    }

    @Test
    void shouldHandleUnknownHttpStatus() {
        HttpStatusCode unknownStatus = HttpStatusCode.valueOf(499);

        ResponseEntityResult result = responseResult(
                Objects.requireNonNull(exceptionHandler.handleExceptionInternal(
                        new RuntimeException("Unknown status"),
                        null,
                        HttpHeaders.EMPTY,
                        unknownStatus,
                        servletRequest("/primes/11", new Object())
                ))
        );

        assertEquals(499, result.status().value());
        assertEquals(499, result.body().getStatus());
        assertEquals("Request processing failed", result.body().getMessage());
        assertNull(result.body().getTraceId());
    }

    @Test
    void shouldHandleInternalServerError() {
        ResponseEntityResult result = responseResult(
                Objects.requireNonNull(exceptionHandler.handleExceptionInternal(
                        new RuntimeException("Internal Server Exception"),
                        null,
                        HttpHeaders.EMPTY,
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        servletRequest("/primes/11", "server-error-id")
                ))
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.status());
        assertEquals(500, result.body().getStatus());
        assertEquals("Unexpected server error", result.body().getMessage());
        assertEquals("server-error-id", result.body().getTraceId());
    }

    @Test
    void shouldHandleUnexpectedException() {
        ResponseEntityResult result = responseResult(
                exceptionHandler.handleUnexpectedException(
                        new RuntimeException("Unexpected failure"),
                        servletRequest(
                                "/primes/11",
                                "unexpected-error-id"
                        )
                )
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.status());
        assertEquals(500, result.body().getStatus());
        assertEquals(
                "Unexpected server error",
                result.body().getMessage()
        );
        assertEquals("/primes/11", result.body().getPath());
        assertEquals("unexpected-error-id", result.body().getTraceId());
    }

    @Test
    void shouldHandleConstraintViolationUsingFirstMessage() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        when(violation.getMessage())
                .thenReturn("must be greater than or equal to 2");

        ConstraintViolationException exception =
                new ConstraintViolationException(Set.of(violation));

        ResponseEntityResult result = responseResult(
                exceptionHandler.handleConstraintViolation(exception, servletRequest("/primes/1", "validation-id")));

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, result.status());
        assertEquals(422, result.body().getStatus());
        assertEquals("must be greater than or equal to 2", result.body().getMessage());
        assertEquals("/primes/1", result.body().getPath());
        assertEquals("validation-id", result.body().getTraceId());
    }

    @Test
    void shouldUseDefaultMessageForEmptyConstraintViolations() {
        ConstraintViolationException exception =
                new ConstraintViolationException(Set.of());

        ResponseEntityResult result = responseResult(
                exceptionHandler.handleConstraintViolation(
                        exception,
                        servletRequest("/primes/1", null)
                )
        );

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, result.status());
        assertEquals("Request validation failed", result.body().getMessage());
        assertNull(result.body().getTraceId());
    }

    private ServletWebRequest servletRequest(String requestUri, Object traceId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(requestUri);

        if (traceId != null) {
            request.setAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE, traceId);
        }

        return new ServletWebRequest(request);
    }

    private ResponseEntityResult responseResult(org.springframework.http.ResponseEntity<Object> response) {
        assertNotNull(response.getBody());
        assertInstanceOf(ErrorResponse.class, response.getBody());

        return new ResponseEntityResult(response.getStatusCode(), (ErrorResponse) response.getBody());
    }
}
