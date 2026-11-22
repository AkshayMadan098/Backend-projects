package com.vyson.urlshortener.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

/**
 * Modern V2 API Response DTO.
 * Includes enriched metadata: apiVersion, owner details, expiration, click metrics.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShortenV2Response {

    private String apiVersion = "v2.0";
    private String shortCode;
    private String originalUrl;
    private String shortUrl;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private Long clickCount;
    private Boolean passwordProtected;
    private UserSummary owner;

    public ShortenV2Response() {
    }

    public ShortenV2Response(String apiVersion, String shortCode, String originalUrl, String shortUrl,
                             LocalDateTime createdAt, LocalDateTime expiresAt, Long clickCount,
                             Boolean passwordProtected, UserSummary owner) {
        this.apiVersion = apiVersion != null ? apiVersion : "v2.0";
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
        this.shortUrl = shortUrl;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.clickCount = clickCount;
        this.passwordProtected = passwordProtected;
        this.owner = owner;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public String getShortUrl() {
        return shortUrl;
    }

    public void setShortUrl(String shortUrl) {
        this.shortUrl = shortUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Long getClickCount() {
        return clickCount;
    }

    public void setClickCount(Long clickCount) {
        this.clickCount = clickCount;
    }

    public Boolean getPasswordProtected() {
        return passwordProtected;
    }

    public void setPasswordProtected(Boolean passwordProtected) {
        this.passwordProtected = passwordProtected;
    }

    public UserSummary getOwner() {
        return owner;
    }

    public void setOwner(UserSummary owner) {
        this.owner = owner;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UserSummary {
        private String email;
        private String name;
        private String tier;

        public UserSummary() {
        }

        public UserSummary(String email, String name, String tier) {
            this.email = email;
            this.name = name;
            this.tier = tier;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getTier() {
            return tier;
        }

        public void setTier(String tier) {
            this.tier = tier;
        }
    }
}
