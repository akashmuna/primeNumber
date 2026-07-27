package com.org.prime.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    public static final String TRACE_ID_ATTRIBUTE =
            TraceIdFilter.class.getName() + ".traceId";

    private static final String MDC_TRACE_ID = "traceId";

    /*
     * Prevent clients from injecting control characters or excessively long
     * values into logs and response headers.
     */
    private static final Pattern VALID_TRACE_ID =
            Pattern.compile("[A-Za-z0-9._-]{1,64}");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String traceId = resolveTraceId(request);

        request.setAttribute(TRACE_ID_ATTRIBUTE, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);

        String previousTraceId = MDC.get(MDC_TRACE_ID);

        try {
            MDC.put(MDC_TRACE_ID, traceId);
            filterChain.doFilter(request, response);
        } finally {
            if (previousTraceId == null) {
                MDC.remove(MDC_TRACE_ID);
            } else {
                MDC.put(MDC_TRACE_ID, previousTraceId);
            }
        }
    }

    private String resolveTraceId(HttpServletRequest request) {
        Object existing = request.getAttribute(TRACE_ID_ATTRIBUTE);

        if (existing instanceof String traceId) {
            return traceId;
        }

        String suppliedTraceId = request.getHeader(TRACE_ID_HEADER);

        if (suppliedTraceId != null
                && VALID_TRACE_ID.matcher(suppliedTraceId).matches()) {
            return suppliedTraceId;
        }

        return UUID.randomUUID()
                .toString()
                .replace("-", "");
    }

    /*
     * Re-establish MDC when asynchronous processing continues on another
     * thread.
     */
    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    /*
     * Preserve the trace ID during Servlet container error dispatches.
     */
    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }
}
