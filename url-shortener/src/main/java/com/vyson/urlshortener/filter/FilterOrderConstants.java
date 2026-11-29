package com.vyson.urlshortener.filter;

import org.springframework.core.Ordered;

/**
 * [Q8] Central order definitions for middleware chaining in Spring Boot.
 *
 * Ordered pipeline:
 * 1. RESPONSE_TIME_ORDER (HIGHEST_PRECEDENCE)
 *    - Outermost wrapper. Must start timer before anything runs and compute total latency after everything finishes.
 * 2. REQUEST_LOGGING_ORDER (HIGHEST_PRECEDENCE + 10)
 *    - Records all incoming HTTP traffic (method, URI, IP, user-agent) for auditability, even if rejected later.
 * 3. BLACKLIST_ORDER (HIGHEST_PRECEDENCE + 20)
 *    - Blocks notorious / abusive API keys immediately using the runtime config file before database lookups.
 * 4. API_KEY_AUTH_ORDER (HIGHEST_PRECEDENCE + 30)
 *    - Validates API key against database, authenticates identity, and attaches User to request attributes (Q7).
 * 5. TIER_AUTHORIZATION_ORDER (HIGHEST_PRECEDENCE + 40)
 *    - Inspects pre-attached User to enforce plan restrictions (e.g., Enterprise tier for bulk operations).
 * 6. CONTROLLER / ENDPOINT HANDLER
 *    - Executes business logic.
 */
public final class FilterOrderConstants {

    private FilterOrderConstants() {
    }

    public static final int RESPONSE_TIME_ORDER = Ordered.HIGHEST_PRECEDENCE;
    public static final int REQUEST_LOGGING_ORDER = Ordered.HIGHEST_PRECEDENCE + 10;
    public static final int BLACKLIST_ORDER = Ordered.HIGHEST_PRECEDENCE + 20;
    public static final int API_KEY_AUTH_ORDER = Ordered.HIGHEST_PRECEDENCE + 30;
    public static final int TIER_AUTHORIZATION_ORDER = Ordered.HIGHEST_PRECEDENCE + 40;
}
