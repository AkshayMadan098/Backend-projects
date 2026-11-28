package com.vyson.urlshortener.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * [Q1] Entity to store incoming HTTP request logs in the SQLite database table "request_logs".
 * Captures timestamp, HTTP method, URL, User-Agent, and client IP.
 */
@Entity
@Table(
    name = "request_logs",
    indexes = {
        @Index(name = "idx_request_log_timestamp", columnList = "timestamp"),
        @Index(name = "idx_request_log_client_ip", columnList = "client_ip"),
        @Index(name = "idx_request_log_http_method", columnList = "http_method")
    }
)
public class RequestLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "http_method", nullable = false, length = 16)
    private String httpMethod;

    @Column(name = "request_url", nullable = false, length = 2048)
    private String requestUrl;

    @Column(name = "user_agent", length = 1024)
    private String userAgent;

    @Column(name = "client_ip", nullable = false, length = 64)
    private String clientIp;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "duration_ms")
    private Long durationMs;

    public RequestLog() {
    }

    public RequestLog(LocalDateTime timestamp, String httpMethod, String requestUrl, String userAgent, String clientIp, Integer statusCode, Long durationMs) {
        this.timestamp = timestamp;
        this.httpMethod = httpMethod;
        this.requestUrl = requestUrl;
        this.userAgent = userAgent;
        this.clientIp = clientIp;
        this.statusCode = statusCode;
        this.durationMs = durationMs;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getRequestUrl() {
        return requestUrl;
    }

    public void setRequestUrl(String requestUrl) {
        this.requestUrl = requestUrl;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }
}
