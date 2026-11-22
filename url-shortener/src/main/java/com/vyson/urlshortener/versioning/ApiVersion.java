package com.vyson.urlshortener.versioning;

/**
 * Supported API Versions for the URL Shortener Platform.
 */
public enum ApiVersion {
    V1("v1", "1.3.0"),
    V2("v2", "2.0.0"),
    UNSPECIFIED("unspecified", "legacy");

    private final String key;
    private final String semVer;

    ApiVersion(String key, String semVer) {
        this.key = key;
        this.semVer = semVer;
    }

    public String getKey() {
        return key;
    }

    public String getSemVer() {
        return semVer;
    }

    public static ApiVersion fromString(String val) {
        if (val == null || val.isBlank()) {
            return UNSPECIFIED;
        }
        String clean = val.trim().toLowerCase();
        if (clean.equals("v1") || clean.equals("1") || clean.equals("1.0") || clean.contains("v1")) {
            return V1;
        }
        if (clean.equals("v2") || clean.equals("2") || clean.equals("2.0") || clean.contains("v2")) {
            return V2;
        }
        return UNSPECIFIED;
    }
}
