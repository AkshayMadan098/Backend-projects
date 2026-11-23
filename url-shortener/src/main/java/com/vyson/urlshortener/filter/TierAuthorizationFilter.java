package com.vyson.urlshortener.filter;

import com.vyson.urlshortener.entity.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * [Q4] Tier Authorization Middleware (Order 5 - Innermost before Controller)
 *
 * In the bulk endpoint (and any future enterprise-restricted routes), verifies that
 * the authenticated user is on the "enterprise" tier.
 *
 * [Q7] Consumes the User object attached to the request attributes by ApiKeyAuthenticationFilter,
 * avoiding duplicate database queries.
 *
 * If the user's tier is not "enterprise", short-circuits the pipeline immediately returning
 * HTTP 403 Forbidden with a clear upgrade message.
 */
@Component
@Order(FilterOrderConstants.TIER_AUTHORIZATION_ORDER)
public class TierAuthorizationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TierAuthorizationFilter.class);
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    // Routes requiring enterprise tier authorization
    private static final List<String> ENTERPRISE_ROUTE_PATTERNS = List.of(
            "/shorten/bulk",
            "/shorten/bulk/**",
            "/v1/shorten/bulk",
            "/v1/shorten/bulk/**",
            "/v2/shorten/bulk",
            "/v2/shorten/bulk/**",
            "/api/v1/shorten/bulk",
            "/api/v1/shorten/bulk/**",
            "/api/v2/shorten/bulk",
            "/api/v2/shorten/bulk/**"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        for (String pattern : ENTERPRISE_ROUTE_PATTERNS) {
            if (PATH_MATCHER.match(pattern, path)) {
                return false; // Run authorization filter on enterprise routes
            }
        }
        return true; // Skip on non-enterprise routes
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        long selfStart = System.nanoTime();

        // [Q7] Retrieve user details previously set in request attributes by ApiKeyAuthenticationFilter
        Object userAttr = request.getAttribute(MiddlewareConstants.AUTHENTICATED_USER_ATTR);

        if (!(userAttr instanceof User user)) {
            long duration = System.nanoTime() - selfStart;
            MiddlewareTimer.record(request, "TierAuthorizationFilter", duration);

            log.warn("[TIER AUTH FAILED] No authenticated user found in request attributes for enterprise endpoint {}.",
                    request.getRequestURI());
            FilterResponseHelper.writeErrorResponse(
                    response,
                    HttpStatus.UNAUTHORIZED,
                    "Unauthorized",
                    "API key is required for bulk creation"
            );
            return;
        }

        // [Q4] Enterprise tier authorization check
        if (!"enterprise".equalsIgnoreCase(user.getTier())) {
            long duration = System.nanoTime() - selfStart;
            MiddlewareTimer.record(request, "TierAuthorizationFilter", duration);

            log.warn("[TIER AUTH REJECTED] User '{}' with tier '{}' attempted to access enterprise endpoint {}.",
                    user.getEmail(), user.getTier(), request.getRequestURI());

            FilterResponseHelper.writeErrorResponse(
                    response,
                    HttpStatus.FORBIDDEN,
                    "Forbidden",
                    "Bulk creation is only available for enterprise tier users. Please upgrade your plan."
            );
            return;
        }

        long selfDuration = System.nanoTime() - selfStart;
        MiddlewareTimer.record(request, "TierAuthorizationFilter", selfDuration);

        log.debug("[TIER AUTH GRANTED] Enterprise tier confirmed for user '{}'. Proceeding to endpoint.", user.getEmail());

        // Forward to controller
        filterChain.doFilter(request, response);
    }
}
