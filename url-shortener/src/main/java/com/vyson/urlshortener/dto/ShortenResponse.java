package com.vyson.urlshortener.dto;

import java.time.LocalDateTime;

public class ShortenResponse {

    private String shortCode;
    private String originalUrl;
    private String shortUrl;
    private LocalDateTime createdAt;

    public ShortenResponse() {
    }

    public ShortenResponse(String shortCode, String originalUrl, String shortUrl, LocalDateTime createdAt) {
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
        this.shortUrl = shortUrl;
        this.createdAt = createdAt;
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
}
