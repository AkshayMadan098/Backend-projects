package com.vyson.urlshortener.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "url_mappings",
    indexes = {
        @Index(name = "idx_short_code", columnList = "short_code", unique = true),
        @Index(name = "idx_original_url", columnList = "original_url")
    }
)
public class UrlMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "short_code", nullable = false, unique = true, length = 32)
    private String shortCode;

    @Column(name = "original_url", nullable = false, length = 2048)
    private String originalUrl;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public UrlMapping() {
    }

    public UrlMapping(String shortCode, String originalUrl, LocalDateTime createdAt) {
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
