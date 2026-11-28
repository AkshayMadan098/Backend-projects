package com.vyson.urlshortener.filter;

import com.vyson.urlshortener.entity.RequestLog;
import com.vyson.urlshortener.repository.RequestLogRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * [Q1 & Q2] Request Logging Middleware (Order 2)
 *
 * Logs details for incoming HTTP requests:
 * 1. Timestamp
 * 2. HTTP method and URL
 * 3. User-Agent
 * 4. Client IP address (with X-Forwarded-For proxy support)
 *
 * Persists the log record to the SQLite database table "request_logs" via RequestLogRepository
 * and outputs the log event through SLF4J / application log file.
 *
 * [Q2] Supports route-level filtering:
 * Through shouldNotFilter() and AntPathMatcher, routes can be included/excluded selectively
 * (e.g. skipping static resources, health checks, or targeting only specific API routes).
 */
@Component
@Order(FilterOrderConstants.REQUEST_LOGGING_ORDER)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final RequestLogRepository requestLogRepository;

    @Value("${logging.middleware.enabled:true}")
    private boolean loggingEnabled;

    @Value("${logging.middleware.include-patterns:/**}")
    private String includePatterns;

    @Value("${logging.middleware.exclude-patterns:/health,/favicon.ico}")
    private String excludePatterns;

    public RequestLoggingFilter(RequestLogRepository requestLogRepository) {
        this.requestLogRepository = requestLogRepository;
    }

    /**
     * [Q2] Determines whether this logging middleware should skip filtering for the given request route.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!loggingEnabled) {
            return true;
        }

        String path = request.getRequestURI();

        // Check exclusions (e.g., /health, static files)
        List<String> excludes = Arrays.stream(excludePatterns.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        for (String pattern : excludes) {
            if (PATH_MATCHER.match(pattern, path)) {
                return true; // Skip logging for this route
            }
        }

        // Check inclusions
        List<String> includes = Arrays.stream(includePatterns.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        boolean matched = false;
        for (String pattern : includes) {
            if (PATH_MATCHER.match(pattern, path)) {
                matched = true;
                break;
            }
        }

        return !matched; // Skip if not matched by any include pattern
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        long selfStart = System.nanoTime();

        LocalDateTime timestamp = LocalDateTime.now();
        String method = request.getMethod();
        String url = extractFullUrl(request);
        String userAgent = request.getHeader("User-Agent");
        String clientIp = extractClientIp(request);

        long preDuration = System.nanoTime() - selfStart;

        try {
            // Forward down the chain
            filterChain.doFilter(request, response);
        } finally {
            long postStart = System.nanoTime();

            int status = response.getStatus();
            Long durationMs = null;
            Object startTimeAttr = request.getAttribute(MiddlewareConstants.REQUEST_START_TIME_ATTR);
            if (startTimeAttr instanceof Long startNanos) {
                durationMs = (System.nanoTime() - startNanos) / 1_000_000;
            }

            // 1. Save log record into database table (Q1)
            try {
                RequestLog logEntity = new RequestLog(
                        timestamp,
                        method,
                        url,
                        userAgent != null ? userAgent : "Unknown",
                        clientIp,
                        status,
                        durationMs
                );
                requestLogRepository.save(logEntity);
            } catch (Exception ex) {
                log.error("Failed to save request log to database: {}", ex.getMessage());
            }

            // 2. Output to log file / SLF4J (Q1)
            log.info("[HTTP LOG] Timestamp: {} | Method: {} | URL: {} | IP: {} | User-Agent: {} | Status: {} | Duration: {}ms",
                    timestamp, method, url, clientIp, userAgent, status, durationMs);

            long postDuration = System.nanoTime() - postStart;
            MiddlewareTimer.record(request, "RequestLoggingFilter", preDuration + postDuration);
        }
    }

    private String extractFullUrl(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        return query != null ? uri + "?" + query : uri;
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // Take the first IP if forwarded through multiple reverse proxies
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
