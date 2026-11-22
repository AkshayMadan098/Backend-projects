package com.vyson.urlshortener.versioning;

import com.vyson.urlshortener.filter.FilterOrderConstants;
import com.vyson.urlshortener.filter.FilterResponseHelper;
import com.vyson.urlshortener.filter.MiddlewareConstants;
import com.vyson.urlshortener.filter.MiddlewareTimer;
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
 * [Q14 BONUS & Core Versioning]
 * API Versioning Middleware Filter.
 *
 * Responsibilities:
 * 1. Resolves request API version (Path, Header, Query Parameter, Content Negotiation).
 * 2. Handles Unspecified version requests according to configured strategy:
 *    - DEFAULT_V1: Transparent backward compatibility fallback to v1.
 *    - REJECT_WITH_ERROR: Rejection with HTTP 400 Bad Request.
 * 3. Injects RFC 8594 (Deprecation, Sunset) and RFC 7234 (Warning) headers for v1 clients.
 * 4. Injects X-API-Version header on all responses.
 * 5. Passes resolved version in request attributes for downstream filters and controllers.
 */
@Component
@Order(FilterOrderConstants.API_VERSIONING_ORDER)
public class ApiVersioningFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiVersioningFilter.class);

    private final ApiVersionResolver versionResolver;
    private final ApiVersioningProperties properties;

    public ApiVersioningFilter(ApiVersionResolver versionResolver, ApiVersioningProperties properties) {
        this.versionResolver = versionResolver;
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        long selfStart = System.nanoTime();
        String uri = request.getRequestURI();

        ApiVersion resolved = versionResolver.resolveVersion(request);

        // Check if version was unspecified on versioned API endpoints
        if (resolved == ApiVersion.UNSPECIFIED) {
            boolean isVersionableRoute = isVersionableRoute(uri);

            if (isVersionableRoute && properties.getUnspecifiedStrategy() == ApiVersioningProperties.UnspecifiedStrategy.REJECT_WITH_ERROR) {
                long duration = System.nanoTime() - selfStart;
                MiddlewareTimer.record(request, "ApiVersioningFilter", duration);

                log.warn("[VERSION REJECTED] Request {} {} missing required API version specification in strict mode.",
                        request.getMethod(), uri);

                FilterResponseHelper.writeErrorResponse(
                        response,
                        HttpStatus.BAD_REQUEST,
                        "Bad Request",
                        "API version is required. Please specify version via URL path (/v1/... or /v2/...), 'X-API-Version' header, or '?version=' query parameter."
                );
                return;
            }

            // Fallback strategy: default to V1
            resolved = ApiVersion.V1;
        }

        // Attach resolved version to request attributes
        request.setAttribute(MiddlewareConstants.RESOLVED_API_VERSION_ATTR, resolved);

        // Inject response version headers
        if (resolved == ApiVersion.V1) {
            response.setHeader(MiddlewareConstants.HEADER_API_VERSION, "v1");
            response.setHeader(MiddlewareConstants.HEADER_DEPRECATION, "@" + properties.getSunsetTimestamp());
            response.setHeader(MiddlewareConstants.HEADER_SUNSET, properties.getSunsetDate());
            response.setHeader(MiddlewareConstants.HEADER_WARNING, "299 - \"" + properties.getDeprecationWarning() + "\"");
            response.setHeader(MiddlewareConstants.HEADER_LINK, properties.getSuccessorLink());
        } else if (resolved == ApiVersion.V2) {
            response.setHeader(MiddlewareConstants.HEADER_API_VERSION, "v2");
        }

        long duration = System.nanoTime() - selfStart;
        MiddlewareTimer.record(request, "ApiVersioningFilter", duration);

        filterChain.doFilter(request, response);
    }

    private boolean isVersionableRoute(String uri) {
        // Health and static resources are not strictly version-gated in reject mode
        if (uri.equals("/health") || uri.startsWith("/health/") || uri.equals("/") || uri.startsWith("/logs")) {
            return false;
        }
        return uri.startsWith("/shorten") || uri.startsWith("/users/urls") || uri.startsWith("/user/urls");
    }
}
