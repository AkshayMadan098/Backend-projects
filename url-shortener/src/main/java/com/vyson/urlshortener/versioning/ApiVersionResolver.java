package com.vyson.urlshortener.versioning;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * Multi-Strategy API Version Resolver.
 * Resolves API Version using:
 * 1. URL Path: /v1/..., /api/v1/..., /v2/..., /api/v2/...
 * 2. Header: X-API-Version: v1 | v2
 * 3. Query Parameter: ?version=v1 | ?version=v2 | ?v=1 | ?v=2
 * 4. Content Negotiation / Accept Header: application/vnd.vyson.v1+json | application/vnd.vyson.v2+json
 */
@Component
public class ApiVersionResolver {

    public static final String HEADER_API_VERSION = "X-API-Version";
    public static final String PARAM_VERSION = "version";
    public static final String PARAM_V = "v";
    public static final String HEADER_ACCEPT = "Accept";

    public ApiVersion resolveVersion(HttpServletRequest request) {
        String uri = request.getRequestURI();

        // 1. URL Path Versioning (Highest Precedence)
        if (uri.startsWith("/v2/") || uri.equals("/v2") || uri.startsWith("/api/v2/") || uri.equals("/api/v2")) {
            return ApiVersion.V2;
        }
        if (uri.startsWith("/v1/") || uri.equals("/v1") || uri.startsWith("/api/v1/") || uri.equals("/api/v1")) {
            return ApiVersion.V1;
        }

        // 2. Custom Request Header (X-API-Version)
        String headerVersion = request.getHeader(HEADER_API_VERSION);
        if (headerVersion != null && !headerVersion.isBlank()) {
            ApiVersion parsed = ApiVersion.fromString(headerVersion);
            if (parsed != ApiVersion.UNSPECIFIED) {
                return parsed;
            }
        }

        // 3. Query Parameter (?version=v2 or ?v=2)
        String paramVersion = request.getParameter(PARAM_VERSION);
        if (paramVersion == null || paramVersion.isBlank()) {
            paramVersion = request.getParameter(PARAM_V);
        }
        if (paramVersion != null && !paramVersion.isBlank()) {
            ApiVersion parsed = ApiVersion.fromString(paramVersion);
            if (parsed != ApiVersion.UNSPECIFIED) {
                return parsed;
            }
        }

        // 4. Accept Header content negotiation
        String acceptHeader = request.getHeader(HEADER_ACCEPT);
        if (acceptHeader != null && !acceptHeader.isBlank()) {
            if (acceptHeader.contains("application/vnd.vyson.v2+json") || acceptHeader.contains("version=2")) {
                return ApiVersion.V2;
            }
            if (acceptHeader.contains("application/vnd.vyson.v1+json") || acceptHeader.contains("version=1")) {
                return ApiVersion.V1;
            }
        }

        // 5. Unspecified
        return ApiVersion.UNSPECIFIED;
    }
}
