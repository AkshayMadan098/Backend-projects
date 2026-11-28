package com.vyson.urlshortener.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

/**
 * [Q6] Response Time Middleware (Order 1 - Outermost)
 *
 * Stores the start time when the request passes through this middleware.
 * While coming back after endpoint execution, calculates the elapsed duration
 * and appends the "X-Response-Time" header to the HTTP response.
 *
 * [Q9 BONUS] Also aggregates and logs per-middleware execution timings and adds
 * the "X-Middleware-Timings" header.
 */
@Component
@Order(FilterOrderConstants.RESPONSE_TIME_ORDER)
public class ResponseTimeFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ResponseTimeFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        long startNanos = System.nanoTime();
        request.setAttribute(MiddlewareConstants.REQUEST_START_TIME_ATTR, startNanos);

        // ContentCachingResponseWrapper allows reading/modifying headers before flushing body to client
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        try {
            long preSelfStart = System.nanoTime();
            // Pre-processing overhead
            long preSelfDuration = System.nanoTime() - preSelfStart;
            MiddlewareTimer.record(request, "ResponseTimeFilter", preSelfDuration);

            // Execute downstream middlewares and controller
            filterChain.doFilter(request, responseWrapper);

        } finally {
            long totalNanos = System.nanoTime() - startNanos;
            long totalMs = totalNanos / 1_000_000;

            // [Q6] Add elapsed time to response header
            responseWrapper.setHeader(MiddlewareConstants.HEADER_RESPONSE_TIME, totalMs + "ms");

            // [Q9 BONUS] Generate per-middleware timing breakdown
            String timingsBreakdown = MiddlewareTimer.formatTimings(request, totalNanos);
            responseWrapper.setHeader(MiddlewareConstants.HEADER_MIDDLEWARE_TIMINGS, timingsBreakdown);

            log.info("[RESPONSE TIME] {} {} -> Status: {} | Elapsed: {}ms | Timings: [{}]",
                    request.getMethod(),
                    request.getRequestURI(),
                    responseWrapper.getStatus(),
                    totalMs,
                    timingsBreakdown);

            // Commit cached response body to actual client output stream
            responseWrapper.copyBodyToResponse();
        }
    }
}
