package com.vyson.urlshortener.filter;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * [Q9 BONUS] Helper to record and format the execution time spent inside
 * each individual middleware in the processing chain.
 */
public final class MiddlewareTimer {

    private MiddlewareTimer() {
    }

    @SuppressWarnings("unchecked")
    public static void record(HttpServletRequest request, String middlewareName, long durationNanos) {
        if (request == null) return;
        Map<String, Long> timings = (Map<String, Long>) request.getAttribute(MiddlewareConstants.MIDDLEWARE_TIMINGS_ATTR);
        if (timings == null) {
            timings = new LinkedHashMap<>();
            request.setAttribute(MiddlewareConstants.MIDDLEWARE_TIMINGS_ATTR, timings);
        }
        timings.put(middlewareName, durationNanos);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Long> getTimings(HttpServletRequest request) {
        if (request == null) return Collections.emptyMap();
        Map<String, Long> timings = (Map<String, Long>) request.getAttribute(MiddlewareConstants.MIDDLEWARE_TIMINGS_ATTR);
        return timings != null ? timings : Collections.emptyMap();
    }

    /**
     * Formats the recorded middleware timings into a human-readable string for logs and headers.
     * Example: "ResponseTime=0.12ms, Logging=1.05ms, Blacklist=0.23ms, Auth=2.10ms, TierAuth=0.05ms, Controller=8.45ms, Total=12.00ms"
     */
    public static String formatTimings(HttpServletRequest request, long totalElapsedNanos) {
        Map<String, Long> timings = getTimings(request);
        StringBuilder sb = new StringBuilder();

        long sumMiddlewaresNanos = 0;
        for (Map.Entry<String, Long> entry : timings.entrySet()) {
            if (sb.length() > 0) sb.append(", ");
            double ms = entry.getValue() / 1_000_000.0;
            sb.append(entry.getKey()).append("=").append(String.format("%.2fms", ms));
            sumMiddlewaresNanos += entry.getValue();
        }

        // Calculate remainder spent in controller / endpoint logic
        long controllerNanos = Math.max(0, totalElapsedNanos - sumMiddlewaresNanos);
        if (sb.length() > 0) sb.append(", ");
        sb.append("Controller=").append(String.format("%.2fms", controllerNanos / 1_000_000.0));

        sb.append(", Total=").append(String.format("%.2fms", totalElapsedNanos / 1_000_000.0));
        return sb.toString();
    }
}
