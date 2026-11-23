package com.vyson.urlshortener.controller;

import com.vyson.urlshortener.dto.*;
import com.vyson.urlshortener.entity.RequestLog;
import com.vyson.urlshortener.entity.User;
import com.vyson.urlshortener.filter.MiddlewareConstants;
import com.vyson.urlshortener.repository.RequestLogRepository;
import com.vyson.urlshortener.service.UrlShortenerService;
import com.vyson.urlshortener.versioning.ApiVersion;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
public class UrlShortenerController {

    private final UrlShortenerService service;
    private final RequestLogRepository requestLogRepository;

    public UrlShortenerController(UrlShortenerService service, RequestLogRepository requestLogRepository) {
        this.service = service;
        this.requestLogRepository = requestLogRepository;
    }

    /**
     * Root Endpoint: GET /
     * Returns service metadata and API directory with version information.
     */
    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> root(HttpServletRequest request) {
        String baseUrl = extractBaseUrl(request);
        return ResponseEntity.ok(Map.of(
                "service", "URL Shortener API (VYSONM2A4 API Versioning)",
                "status", "UP",
                "timestamp", LocalDateTime.now(),
                "versions", Map.of(
                        "v1", "Legacy (Deprecated, RFC 8594 Sunset: 2027-06-01)",
                        "v2", "Current Active Stable (Mandatory Auth, Enriched Metadata)"
                ),
                "endpoints", Map.of(
                        "POST /shorten", "Shorten URL (resolves version via path, header X-API-Version, or ?version=)",
                        "POST /v1/shorten", "Legacy V1: Anonymous shortening permitted",
                        "POST /v2/shorten", "Modern V2: Strict authentication required",
                        "POST /shorten/bulk", "Bulk shorten URLs (Enterprise tier only)",
                        "GET /redirect?code={code}", "Redirect to original URL",
                        "PUT /shorten/{code}", "Edit short code target URL, expiry date, or password",
                        "DELETE /shorten/{code}", "Soft-delete short code",
                        "GET /users/urls", "List all URLs owned by user",
                        "GET /logs", "View recent HTTP request logs stored by RequestLogging middleware",
                        "GET /health", "System health and database check"
                ),
                "demoUrl", baseUrl + "/shorten"
        ));
    }

    /**
     * Endpoint 1: POST /shorten
     * Accepts a long URL and returns a shortened code and redirect URL.
     * M2A4: Adapts response dynamically based on resolved API version (Header, Query, or Default).
     */
    @PostMapping("/shorten")
    public ResponseEntity<?> shortenUrl(
            @Valid @RequestBody(required = false) ShortenRequest request,
            @RequestParam(name = "url", required = false) String urlParam,
            @RequestHeader(name = "X-API-Key", required = false) String apiKey,
            HttpServletRequest servletRequest) {

        ShortenRequest effectiveRequest = request != null ? request : new ShortenRequest();
        if ((effectiveRequest.getUrl() == null || effectiveRequest.getUrl().isBlank()) && urlParam != null && !urlParam.isBlank()) {
            effectiveRequest.setUrl(urlParam);
        }

        if (effectiveRequest.getUrl() == null || effectiveRequest.getUrl().isBlank()) {
            throw new IllegalArgumentException("URL must be provided either in request body as JSON { \"url\": \"...\" } or as query parameter ?url=...");
        }

        String baseUrl = extractBaseUrl(servletRequest);
        User authenticatedUser = (User) servletRequest.getAttribute(MiddlewareConstants.AUTHENTICATED_USER_ATTR);
        ApiVersion version = (ApiVersion) servletRequest.getAttribute(MiddlewareConstants.RESOLVED_API_VERSION_ATTR);

        if (version == ApiVersion.V2) {
            ShortenV2Response v2Response = service.shortenUrlV2(effectiveRequest, authenticatedUser, apiKey, baseUrl);
            return ResponseEntity.status(HttpStatus.CREATED).body(v2Response);
        }

        ShortenResponse response = service.shortenUrl(effectiveRequest, authenticatedUser, apiKey, baseUrl);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * [Q10 & Q12] Endpoint 1b: POST /shorten/bulk
     * Bulk creation of short codes. Restricted to Enterprise tier users.
     * [Q4 & Q7] Authentication and Enterprise tier checks are enforced by middlewares.
     */
    @PostMapping("/shorten/bulk")
    public ResponseEntity<?> bulkShorten(
            @Valid @RequestBody ShortenBulkRequest bulkRequest,
            @RequestHeader(name = "X-API-Key", required = false) String apiKey,
            HttpServletRequest servletRequest) {

        String baseUrl = extractBaseUrl(servletRequest);
        User authenticatedUser = (User) servletRequest.getAttribute(MiddlewareConstants.AUTHENTICATED_USER_ATTR);
        ApiVersion version = (ApiVersion) servletRequest.getAttribute(MiddlewareConstants.RESOLVED_API_VERSION_ATTR);

        if (version == ApiVersion.V2) {
            ShortenBulkV2Response v2Response = service.bulkShortenUrlsV2(bulkRequest, authenticatedUser, apiKey, baseUrl);
            return ResponseEntity.status(HttpStatus.CREATED).body(v2Response);
        }

        ShortenBulkResponse response = service.bulkShortenUrls(bulkRequest, authenticatedUser, apiKey, baseUrl);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * [Q13] Endpoint 1c: PUT /shorten/{code}
     * Edits an existing short code (target URL, expiry date, password). Requires owner API key.
     */
    @PutMapping("/shorten/{code}")
    public ResponseEntity<?> editShortCode(
            @PathVariable("code") String code,
            @RequestBody ShortenEditRequest editRequest,
            @RequestHeader(name = "X-API-Key", required = false) String apiKey,
            HttpServletRequest servletRequest) {

        String baseUrl = extractBaseUrl(servletRequest);
        User authenticatedUser = (User) servletRequest.getAttribute(MiddlewareConstants.AUTHENTICATED_USER_ATTR);
        ApiVersion version = (ApiVersion) servletRequest.getAttribute(MiddlewareConstants.RESOLVED_API_VERSION_ATTR);

        if (version == ApiVersion.V2) {
            ShortenV2Response response = service.editShortCodeV2(code, editRequest, authenticatedUser, apiKey, baseUrl);
            return ResponseEntity.ok(response);
        }

        ShortenResponse response = service.editShortCode(code, editRequest, authenticatedUser, apiKey, baseUrl);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint 2: GET /redirect?code={code}
     * Redirects (HTTP 302 Found) to the original URL.
     * M2A2: Increments click count (Q3), checks expiry (Q8), verifies password (Q14).
     */
    @GetMapping("/redirect")
    public ResponseEntity<Void> redirectToOriginalUrl(
            @RequestParam("code") String code,
            @RequestParam(name = "password", required = false) String passwordParam,
            @RequestHeader(name = "X-Password", required = false) String passwordHeader) {

        String password = passwordParam != null && !passwordParam.isBlank() ? passwordParam : passwordHeader;
        String originalUrl = service.getOriginalUrl(code, password);

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, originalUrl)
                .build();
    }

    /**
     * Endpoint 3: DELETE /shorten/{code} (RESTful path variable)
     * Endpoint 3b: DELETE /shorten?code={code} (Query param variant)
     * [Q5] Soft-deletes the short code mapping with user ownership verification.
     */
    @DeleteMapping({"/shorten/{code}", "/shorten"})
    public ResponseEntity<Map<String, String>> deleteShortCode(
            @PathVariable(name = "code", required = false) String codePath,
            @RequestParam(name = "code", required = false) String codeParam,
            @RequestHeader(name = "X-API-Key", required = false) String apiKey,
            HttpServletRequest servletRequest) {

        String code = (codePath != null && !codePath.isBlank()) ? codePath : codeParam;
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Short code to delete must be specified in the URL path (/shorten/{code}) or query parameter (/shorten?code={code})");
        }

        User authenticatedUser = (User) servletRequest.getAttribute(MiddlewareConstants.AUTHENTICATED_USER_ATTR);
        service.deleteShortCode(code, authenticatedUser, apiKey);
        return ResponseEntity.ok(Map.of(
                "message", "Short code '" + code + "' deleted successfully",
                "code", code
        ));
    }

    /**
     * [Q15] Endpoint 4: GET /users/urls & GET /user/urls
     * Lists all active URLs owned by the authenticated user.
     */
    @GetMapping({"/users/urls", "/user/urls"})
    public ResponseEntity<?> getUserUrls(
            @RequestHeader(name = "X-API-Key", required = false) String apiKey,
            HttpServletRequest servletRequest) {

        String baseUrl = extractBaseUrl(servletRequest);
        User authenticatedUser = (User) servletRequest.getAttribute(MiddlewareConstants.AUTHENTICATED_USER_ATTR);
        ApiVersion version = (ApiVersion) servletRequest.getAttribute(MiddlewareConstants.RESOLVED_API_VERSION_ATTR);

        if (version == ApiVersion.V2) {
            return ResponseEntity.ok(service.getUserUrlsV2(authenticatedUser, apiKey, baseUrl));
        }

        List<ShortenResponse> urls = service.getUserUrls(authenticatedUser, apiKey, baseUrl);
        return ResponseEntity.ok(urls);
    }

    /**
     * [Q1] Endpoint: GET /logs
     * Returns recent request logs saved by the RequestLoggingFilter in the SQLite database.
     */
    @GetMapping("/logs")
    public ResponseEntity<List<RequestLog>> getRequestLogs(
            @RequestParam(name = "limit", defaultValue = "50") int limit) {
        List<RequestLog> logs = requestLogRepository.findAllByOrderByTimestampDesc();
        if (logs.size() > limit) {
            logs = logs.subList(0, limit);
        }
        return ResponseEntity.ok(logs);
    }

    /**
     * [Q16 BONUS] Endpoint 5: GET /health
     * Health check endpoint verifying service state and SQLite database connectivity.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        boolean dbHealthy = service.checkHealth();
        HttpStatus status = dbHealthy ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;

        return ResponseEntity.status(status).body(Map.of(
                "status", dbHealthy ? "UP" : "DOWN",
                "database", dbHealthy ? "UP" : "DOWN",
                "timestamp", LocalDateTime.now()
        ));
    }

    private String extractBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();

        if (("http".equals(scheme) && serverPort == 80) || ("https".equals(scheme) && serverPort == 443)) {
            return scheme + "://" + serverName;
        }
        return scheme + "://" + serverName + ":" + serverPort;
    }
}
