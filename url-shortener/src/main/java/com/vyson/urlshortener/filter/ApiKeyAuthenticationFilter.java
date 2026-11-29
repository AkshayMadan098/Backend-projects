package com.vyson.urlshortener.filter;

import com.vyson.urlshortener.entity.User;
import com.vyson.urlshortener.repository.UserRepository;
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
import java.util.Optional;

/**
 * [Q3 & Q7] Authentication Middleware (Order 4)
 *
 * [Q3] Checks whether the provided API key is valid or not.
 * If invalid or missing on a protected route, short-circuits the pipeline immediately
 * returning HTTP 401 Unauthorized without executing endpoint logic.
 *
 * [Q7] When user details are fetched from the API key the first time, sets this User
 * object into the request attributes ("authenticatedUser") so subsequent middlewares
 * (such as authorization) and controllers can consume it directly without duplicate database queries.
 */
@Component
@Order(FilterOrderConstants.API_KEY_AUTH_ORDER)
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthenticationFilter.class);
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    // Routes that strictly require an API key
    private static final List<String> PROTECTED_ROUTE_PATTERNS = List.of(
            "/shorten/bulk",
            "/shorten/bulk/**",
            "/users/urls",
            "/user/urls"
    );

    private final UserRepository userRepository;

    public ApiKeyAuthenticationFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        long selfStart = System.nanoTime();

        String path = request.getRequestURI();
        String apiKey = extractApiKey(request);
        boolean requiresAuth = isProtectedRoute(path);

        // Case 1: Protected route accessed without API key
        if (requiresAuth && (apiKey == null || apiKey.isBlank())) {
            long duration = System.nanoTime() - selfStart;
            MiddlewareTimer.record(request, "ApiKeyAuthenticationFilter", duration);

            log.warn("[AUTH REQUIRED] Request {} {} missing required API key.", request.getMethod(), path);
            FilterResponseHelper.writeErrorResponse(
                    response,
                    HttpStatus.UNAUTHORIZED,
                    "Unauthorized",
                    "API key is required for this endpoint"
            );
            return;
        }

        // Case 2: API key is supplied (either on protected or optional routes)
        if (apiKey != null && !apiKey.isBlank()) {
            Optional<User> userOpt = userRepository.findByApiKey(apiKey.trim());

            if (userOpt.isEmpty()) {
                long duration = System.nanoTime() - selfStart;
                MiddlewareTimer.record(request, "ApiKeyAuthenticationFilter", duration);

                log.warn("[AUTH FAILED] Invalid API key '{}' for route {} {}.", maskApiKey(apiKey), request.getMethod(), path);
                FilterResponseHelper.writeErrorResponse(
                        response,
                        HttpStatus.UNAUTHORIZED,
                        "Unauthorized",
                        "Invalid API key provided"
                );
                return;
            }

            // [Q7] Context Passing: Store fetched user in request attributes
            User authenticatedUser = userOpt.get();
            request.setAttribute(MiddlewareConstants.AUTHENTICATED_USER_ATTR, authenticatedUser);

            log.debug("[AUTH SUCCESS] User '{}' (tier: {}) authenticated and attached to request context.",
                    authenticatedUser.getEmail(), authenticatedUser.getTier());
        }

        long selfDuration = System.nanoTime() - selfStart;
        MiddlewareTimer.record(request, "ApiKeyAuthenticationFilter", selfDuration);

        // Continue filter chain
        filterChain.doFilter(request, response);
    }

    private boolean isProtectedRoute(String path) {
        for (String pattern : PROTECTED_ROUTE_PATTERNS) {
            if (PATH_MATCHER.match(pattern, path)) {
                return true;
            }
        }
        return false;
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
