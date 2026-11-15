package com.vyson.urlshortener.controller;

import com.vyson.urlshortener.dto.*;
import com.vyson.urlshortener.service.UrlShortenerService;
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

    public UrlShortenerController(UrlShortenerService service) {
        this.service = service;
    }

    /**
     * Root Endpoint: GET /
     * Returns service metadata and API directory.
     */
    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> root(HttpServletRequest request) {
        String baseUrl = extractBaseUrl(request);
        return ResponseEntity.ok(Map.of(
                "service", "URL Shortener API (VYSONM2A2)",
                "status", "UP",
                "timestamp", LocalDateTime.now(),
                "endpoints", Map.of(
                        "POST /shorten", "Shorten URL (supports customCode, expiresAt, password, X-API-Key header)",
                        "POST /shorten/bulk", "Bulk shorten URLs (Enterprise tier only, requires X-API-Key)",
                        "GET /redirect?code={code}", "Redirect to original URL (supports password query/header)",
                        "PUT /shorten/{code}", "Edit short code target URL, expiry date, or password (requires owner X-API-Key)",
                        "DELETE /shorten/{code}", "Soft-delete short code (requires owner X-API-Key)",
                        "GET /users/urls", "List all URLs owned by user (requires X-API-Key)",
                        "GET /health", "System health and database check"
                ),
                "demoUrl", baseUrl + "/shorten"
        ));
    }

    /**
     * Endpoint 1: POST /shorten
     * Accepts a long URL and returns a shortened code and redirect URL.
     * M2A2: Supports customCode (Q9), expiresAt (Q8), password (Q14), and owner API key (Q5).
     */
    @PostMapping("/shorten")
    public ResponseEntity<ShortenResponse> shortenUrl(
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
        ShortenResponse response = service.shortenUrl(effectiveRequest, apiKey, baseUrl);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * [Q10 & Q12] Endpoint 1b: POST /shorten/bulk
     * Bulk creation of short codes. Restricted to Enterprise tier users.
     */
    @PostMapping("/shorten/bulk")
    public ResponseEntity<ShortenBulkResponse> bulkShorten(
            @Valid @RequestBody ShortenBulkRequest bulkRequest,
            @RequestHeader(name = "X-API-Key", required = false) String apiKey,
            HttpServletRequest servletRequest) {

        String baseUrl = extractBaseUrl(servletRequest);
        ShortenBulkResponse response = service.bulkShortenUrls(bulkRequest, apiKey, baseUrl);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * [Q13] Endpoint 1c: PUT /shorten/{code}
     * Edits an existing short code (target URL, expiry date, password). Requires owner API key.
     */
    @PutMapping("/shorten/{code}")
    public ResponseEntity<ShortenResponse> editShortCode(
            @PathVariable("code") String code,
            @RequestBody ShortenEditRequest editRequest,
            @RequestHeader(name = "X-API-Key", required = false) String apiKey,
            HttpServletRequest servletRequest) {

        String baseUrl = extractBaseUrl(servletRequest);
        ShortenResponse response = service.editShortCode(code, editRequest, apiKey, baseUrl);
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
            @RequestHeader(name = "X-API-Key", required = false) String apiKey) {

        String code = (codePath != null && !codePath.isBlank()) ? codePath : codeParam;
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Short code to delete must be specified in the URL path (/shorten/{code}) or query parameter (/shorten?code={code})");
        }

        service.deleteShortCode(code, apiKey);
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
    public ResponseEntity<List<ShortenResponse>> getUserUrls(
            @RequestHeader(name = "X-API-Key", required = false) String apiKey,
            HttpServletRequest servletRequest) {

        String baseUrl = extractBaseUrl(servletRequest);
        List<ShortenResponse> urls = service.getUserUrls(apiKey, baseUrl);
        return ResponseEntity.ok(urls);
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
