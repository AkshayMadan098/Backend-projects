package com.vyson.urlshortener.filter;

import com.vyson.urlshortener.service.BlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * [Q5] Blacklist Middleware (Order 3)
 *
 * Checks the incoming API key against a blacklist dynamically loaded at runtime
 * from an external configuration file. If the API key is blacklisted:
 * - Immediately returns HTTP 403 Forbidden.
 * - Short-circuits the pipeline (does NOT execute endpoint logic or subsequent middlewares).
 */
@Component
@Order(FilterOrderConstants.BLACKLIST_ORDER)
public class BlacklistFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(BlacklistFilter.class);

    private final BlacklistService blacklistService;

    public BlacklistFilter(BlacklistService blacklistService) {
        this.blacklistService = blacklistService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        long selfStart = System.nanoTime();

        String apiKey = extractApiKey(request);

        if (apiKey != null && blacklistService.isBlacklisted(apiKey)) {
            long duration = System.nanoTime() - selfStart;
            MiddlewareTimer.record(request, "BlacklistFilter", duration);

            log.warn("[BLACKLIST BLOCKED] Request {} {} blocked. API key '{}' is blacklisted in runtime config file.",
                    request.getMethod(), request.getRequestURI(), maskApiKey(apiKey));

            // Short-circuit immediately with 403 Forbidden
            FilterResponseHelper.writeErrorResponse(
                    response,
                    HttpStatus.FORBIDDEN,
                    "Forbidden",
                    "Your API key has been revoked or blacklisted due to suspicious or abusive activity."
            );
            return;
        }

        long selfDuration = System.nanoTime() - selfStart;
        MiddlewareTimer.record(request, "BlacklistFilter", selfDuration);

        // Proceed to next filter
        filterChain.doFilter(request, response);
    }

    private String extractApiKey(HttpServletRequest request) {
        String key = request.getHeader(MiddlewareConstants.HEADER_API_KEY);
        if (key != null && !key.isBlank()) {
            return key.trim();
        }
        String paramKey = request.getParameter("apiKey");
        if (paramKey != null && !paramKey.isBlank()) {
            return paramKey.trim();
        }
        paramKey = request.getParameter("api_key");
        if (paramKey != null && !paramKey.isBlank()) {
            return paramKey.trim();
        }
        return null;
    }

    private String maskApiKey(String key) {
        if (key == null || key.length() < 8) return "****";
        return key.substring(0, 7) + "..." + key.substring(key.length() - 4);
    }
}
