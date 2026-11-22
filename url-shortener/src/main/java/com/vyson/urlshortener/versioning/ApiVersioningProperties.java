package com.vyson.urlshortener.versioning;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * [Q14 BONUS] Configuration properties for API Versioning and Unspecified Version Routing.
 */
@Component
@ConfigurationProperties(prefix = "api.versioning")
public class ApiVersioningProperties {

    public enum UnspecifiedStrategy {
        DEFAULT_V1,
        REJECT_WITH_ERROR
    }

    /**
     * Strategy when incoming request does not explicitly declare a version.
     * Options:
     * - DEFAULT_V1: Gracefully routes unversioned calls to V1 behavior with deprecation headers (Backwards-compatible).
     * - REJECT_WITH_ERROR: Immediately returns HTTP 400 Bad Request requiring version specification (Strict mode).
     */
    private UnspecifiedStrategy unspecifiedStrategy = UnspecifiedStrategy.DEFAULT_V1;

    /**
     * Default fallback version key (e.g. "v1")
     */
    private String defaultVersion = "v1";

    /**
     * RFC 8594 Sunset HTTP header value (scheduled retirement date of v1)
     */
    private String sunsetDate = "Wed, 01 Jun 2027 00:00:00 GMT";

    /**
     * RFC 8594 Deprecation header unix timestamp
     */
    private long sunsetTimestamp = 1811817600L;

    /**
     * RFC 7234 Warning header message
     */
    private String deprecationWarning = "API v1 is deprecated and scheduled for sunset on 2027-06-01. Please migrate to API v2 (/v2/...)";

    /**
     * RFC 8288 Link header for successor version
     */
    private String successorLink = "</v2/shorten>; rel=\"successor-version\"";

    public UnspecifiedStrategy getUnspecifiedStrategy() {
        return unspecifiedStrategy;
    }

    public void setUnspecifiedStrategy(UnspecifiedStrategy unspecifiedStrategy) {
        this.unspecifiedStrategy = unspecifiedStrategy;
    }

    public String getDefaultVersion() {
        return defaultVersion;
    }

    public void setDefaultVersion(String defaultVersion) {
        this.defaultVersion = defaultVersion;
    }

    public String getSunsetDate() {
        return sunsetDate;
    }

    public void setSunsetDate(String sunsetDate) {
        this.sunsetDate = sunsetDate;
    }

    public long getSunsetTimestamp() {
        return sunsetTimestamp;
    }

    public void setSunsetTimestamp(long sunsetTimestamp) {
        this.sunsetTimestamp = sunsetTimestamp;
    }

    public String getDeprecationWarning() {
        return deprecationWarning;
    }

    public void setDeprecationWarning(String deprecationWarning) {
        this.deprecationWarning = deprecationWarning;
    }

    public String getSuccessorLink() {
        return successorLink;
    }

    public void setSuccessorLink(String successorLink) {
        this.successorLink = successorLink;
    }
}
