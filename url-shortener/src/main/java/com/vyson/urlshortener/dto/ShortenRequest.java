package com.vyson.urlshortener.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public class ShortenRequest {

    @NotBlank(message = "url is required and cannot be blank")
    private String url;

    @JsonProperty("customCode")
    private String customCode; // Q9

    @JsonProperty("expiresAt")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expiresAt; // Q8

    @JsonProperty("password")
    private String password; // Q14

    public ShortenRequest() {
    }

    public ShortenRequest(String url) {
        this.url = url;
    }

    public ShortenRequest(String url, String customCode, LocalDateTime expiresAt, String password) {
        this.url = url;
        this.customCode = customCode;
        this.expiresAt = expiresAt;
        this.password = password;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getCustomCode() {
        return customCode;
    }

    public void setCustomCode(String customCode) {
        this.customCode = customCode;
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
