package com.org.prime.exception;

import com.org.prime.filters.TraceIdFilter;
import com.org.prime.model.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /*
     * Handles values such as:
     *   /prime/abc
     *   /prime/999999999999999999999999
     *
     * Spring cannot convert these values to Long.
     */
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        var message = "Invalid request parameter";

        if (exception instanceof MethodArgumentTypeMismatchException mismatch) {
            var expectedType = mismatch.getRequiredType() == null
                    ? "the required type"
                    : mismatch.getRequiredType().getSimpleName();

            message = "Invalid value '%s' for parameter '%s'; expected %s"
                    .formatted(
                            mismatch.getValue(),
                            mismatch.getName(),
                            expectedType
                    );
        }

        return response(
                HttpStatus.BAD_REQUEST,
                message,
                headers,
                request
        );
    }

    /*
     * Handles Spring MVC exceptions, including validation failures and 406.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception,
            Object body,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        /*
         * Do not return ErrorResponse for 406. If the client's Accept header
         * rejects JSON and XML, Spring cannot serialize that error body either.
         */
        if (status.value() == HttpStatus.NOT_ACCEPTABLE.value()) {
            return new ResponseEntity<>(null, headers, status);
        }

        String message = status.is5xxServerError()
                ? "Unexpected server error"
                : defaultMessage(status);

        if (status.is5xxServerError()) {
            log.error("Spring MVC request processing failed", exception);
        }

        return response(status, message, headers, request);
    }

    /*
     * Catch genuinely unexpected application failures.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpectedException(Exception exception, WebRequest request) {

        log.error("Unexpected error while processing request", exception);

        return response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unexpected server error",
                HttpHeaders.EMPTY,
                request
        );
    }

    /*
     * Needed when validation is performed through an AOP @Validated proxy.
     * Spring MVC method validation exceptions are handled by the parent class.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException exception, WebRequest request) {

        String message = exception.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .findFirst()
                .orElse("Request validation failed");

        return response(
                HttpStatus.UNPROCESSABLE_CONTENT,
                message,
                HttpHeaders.EMPTY,
                request
        );
    }

    /**
     * Handles exhausted token buckets.
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Object> handleRateLimitExceeded(RateLimitExceededException exception, WebRequest request) {
        HttpHeaders headers = new HttpHeaders();

        headers.set(HttpHeaders.RETRY_AFTER, Long.toString(exception.getRetryAfterSeconds()));

        return response(
                HttpStatus.TOO_MANY_REQUESTS,
                "Rate limit exceeded",
                headers,
                request
        );
    }


    private ResponseEntity<Object> response(
            HttpStatusCode status,
            String message,
            HttpHeaders headers,
            WebRequest request) {

        ErrorResponse body = new ErrorResponse();
        body.setTimestamp(Instant.now());
        body.setStatus(status.value());
        body.setMessage(message);
        body.setPath(requestPath(request));
        body.setTraceId(traceId(request));

        return new ResponseEntity<>(body, headers, status);
    }

    private static String requestPath(WebRequest request) {
        if (request instanceof ServletWebRequest servletRequest) {
            HttpServletRequest httpRequest = servletRequest.getRequest();
            return httpRequest.getRequestURI();
        }

        return "";
    }

    private static String defaultMessage(HttpStatusCode statusCode) {
        HttpStatus status = HttpStatus.resolve(statusCode.value());

        return status == null
                ? "Request processing failed"
                : status.getReasonPhrase();
    }

    private static String traceId(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            Object value = servletWebRequest.getRequest()
                    .getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE);

            if (value instanceof String traceId) {
                return traceId;
            }
        }

        return null;
    }
}
