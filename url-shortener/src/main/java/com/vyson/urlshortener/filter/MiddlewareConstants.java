package com.vyson.urlshortener.filter;

/**
 * Common constants for HTTP middleware attributes, headers, and context keys.
 */
public final class MiddlewareConstants {

    private MiddlewareConstants() {
    }

    // [Q7] Request attribute key for storing the authenticated User object across middlewares
    public static final String AUTHENTICATED_USER_ATTR = "authenticatedUser";

    // [Q6] Header for roundtrip request duration
    public static final String HEADER_RESPONSE_TIME = "X-Response-Time";

    // [Q9 BONUS] Header for per-middleware execution breakdown
    public static final String HEADER_MIDDLEWARE_TIMINGS = "X-Middleware-Timings";

    // Standard API Key Header
    public static final String HEADER_API_KEY = "X-API-Key";

    // Request attribute key for tracking per-middleware nanosecond execution times
    public static final String MIDDLEWARE_TIMINGS_ATTR = "middlewareTimings";

    // Request attribute key for request start time
    public static final String REQUEST_START_TIME_ATTR = "requestStartTimeNanos";

    // [M2A4 API Versioning] Request attribute key for resolved API version enum
    public static final String RESOLVED_API_VERSION_ATTR = "resolvedApiVersion";

    // [M2A4 API Versioning] Response & Request Header for API Versioning
    public static final String HEADER_API_VERSION = "X-API-Version";

    // [M2A4 API Versioning] RFC 8594 / RFC 7234 Standard Deprecation & Sunset Headers
    public static final String HEADER_DEPRECATION = "Deprecation";
    public static final String HEADER_SUNSET = "Sunset";
    public static final String HEADER_WARNING = "Warning";
    public static final String HEADER_LINK = "Link";
}
