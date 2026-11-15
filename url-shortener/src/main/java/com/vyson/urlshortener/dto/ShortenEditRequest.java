package com.vyson.urlshortener.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class ShortenEditRequest {

    @JsonProperty("originalUrl")
    private String originalUrl;

    @JsonProperty("expiresAt")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expiresAt;

    @JsonProperty("password")
    private String password;

    public ShortenEditRequest() {
    }

    public ShortenEditRequest(String originalUrl, LocalDateTime expiresAt, String password) {
        this.originalUrl = originalUrl;
        this.expiresAt = expiresAt;
        this.password = password;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
