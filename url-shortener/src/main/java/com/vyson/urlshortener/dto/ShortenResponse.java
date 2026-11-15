package com.vyson.urlshortener.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShortenResponse {

    private String shortCode;
    private String originalUrl;
    private String shortUrl;
    private LocalDateTime createdAt;
    private Long clickCount;
    private LocalDateTime lastAccessedAt;
    private LocalDateTime expiresAt;
    private Boolean isProtected;
    private Boolean isDeleted;

    public ShortenResponse() {
    }

    public ShortenResponse(String shortCode, String originalUrl, String shortUrl, LocalDateTime createdAt) {
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
        this.shortUrl = shortUrl;
        this.createdAt = createdAt;
    }

    public ShortenResponse(String shortCode, String originalUrl, String shortUrl, LocalDateTime createdAt,
                           Long clickCount, LocalDateTime lastAccessedAt, LocalDateTime expiresAt,
                           Boolean isProtected, Boolean isDeleted) {
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
        this.shortUrl = shortUrl;
        this.createdAt = createdAt;
        this.clickCount = clickCount;
        this.lastAccessedAt = lastAccessedAt;
        this.expiresAt = expiresAt;
        this.isProtected = isProtected;
        this.isDeleted = isDeleted;
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

    public Long getClickCount() {
        return clickCount;
    }

    public void setClickCount(Long clickCount) {
        this.clickCount = clickCount;
    }

    public LocalDateTime getLastAccessedAt() {
        return lastAccessedAt;
    }

    public void setLastAccessedAt(LocalDateTime lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Boolean getIsProtected() {
        return isProtected;
    }

    public void setIsProtected(Boolean isProtected) {
        this.isProtected = isProtected;
    }

    public Boolean getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Boolean isDeleted) {
        this.isDeleted = isDeleted;
    }
}
