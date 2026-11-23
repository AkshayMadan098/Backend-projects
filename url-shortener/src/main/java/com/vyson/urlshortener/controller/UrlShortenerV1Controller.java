package com.vyson.urlshortener.controller;

import com.vyson.urlshortener.dto.*;
import com.vyson.urlshortener.entity.User;
import com.vyson.urlshortener.filter.MiddlewareConstants;
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

/**
 * Legacy API v1 Controller.
 * Base Path: /v1, /api/v1
 *
 * Characteristics:
 * - Anonymous link creation allowed (backward compatible with early releases).
 * - Standard legacy response formats.
 * - Monitored for deprecation with RFC 8594 / RFC 7234 headers injected by ApiVersioningFilter.
 */
@RestController
@RequestMapping({"/v1", "/api/v1"})
public class UrlShortenerV1Controller {

    private final UrlShortenerService service;

    public UrlShortenerV1Controller(UrlShortenerService service) {
        this.service = service;
    }

    @GetMapping({"", "/"})
    public ResponseEntity<Map<String, Object>> v1Root(HttpServletRequest request) {
        return ResponseEntity.ok(Map.of(
                "version", "v1.3.0",
                "status", "DEPRECATED",
                "sunset", "2027-06-01",
                "migrationNotice", "Please migrate your integrations to /v2",
                "endpoints", Map.of(
                        "POST /v1/shorten", "Create short URL (anonymous allowed)",
                        "GET /v1/redirect?code={code}", "Redirect to target URL",
                        "GET /v1/health", "Basic health check"
                )
        ));
    }

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
            throw new IllegalArgumentException("URL must be provided either in request body or as query parameter ?url=...");
        }

        String baseUrl = extractBaseUrl(servletRequest);
        User authenticatedUser = (User) servletRequest.getAttribute(MiddlewareConstants.AUTHENTICATED_USER_ATTR);
        ShortenResponse response = service.shortenUrl(effectiveRequest, authenticatedUser, apiKey, baseUrl);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/shorten/bulk")
    public ResponseEntity<ShortenBulkResponse> bulkShorten(
            @Valid @RequestBody ShortenBulkRequest bulkRequest,
            @RequestHeader(name = "X-API-Key", required = false) String apiKey,
            HttpServletRequest servletRequest) {

        String baseUrl = extractBaseUrl(servletRequest);
        User authenticatedUser = (User) servletRequest.getAttribute(MiddlewareConstants.AUTHENTICATED_USER_ATTR);
        ShortenBulkResponse response = service.bulkShortenUrls(bulkRequest, authenticatedUser, apiKey, baseUrl);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/shorten/{code}")
    public ResponseEntity<ShortenResponse> editShortCode(
            @PathVariable("code") String code,
            @RequestBody ShortenEditRequest editRequest,
            @RequestHeader(name = "X-API-Key", required = false) String apiKey,
            HttpServletRequest servletRequest) {

        String baseUrl = extractBaseUrl(servletRequest);
        User authenticatedUser = (User) servletRequest.getAttribute(MiddlewareConstants.AUTHENTICATED_USER_ATTR);
        ShortenResponse response = service.editShortCode(code, editRequest, authenticatedUser, apiKey, baseUrl);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/shorten/{code}")
    public ResponseEntity<Map<String, String>> deleteShortCode(
            @PathVariable("code") String code,
            @RequestHeader(name = "X-API-Key", required = false) String apiKey,
            HttpServletRequest servletRequest) {

        User authenticatedUser = (User) servletRequest.getAttribute(MiddlewareConstants.AUTHENTICATED_USER_ATTR);
        service.deleteShortCode(code, authenticatedUser, apiKey);
        return ResponseEntity.ok(Map.of("message", "Short code '" + code + "' deleted successfully", "code", code));
    }

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

    @GetMapping("/users/urls")
    public ResponseEntity<List<ShortenResponse>> getUserUrls(
            @RequestHeader(name = "X-API-Key", required = false) String apiKey,
            HttpServletRequest servletRequest) {

        String baseUrl = extractBaseUrl(servletRequest);
        User authenticatedUser = (User) servletRequest.getAttribute(MiddlewareConstants.AUTHENTICATED_USER_ATTR);
        List<ShortenResponse> urls = service.getUserUrls(authenticatedUser, apiKey, baseUrl);
        return ResponseEntity.ok(urls);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        boolean dbHealthy = service.checkHealth();
        return ResponseEntity.ok(Map.of(
                "version", "v1.3.0",
                "status", dbHealthy ? "UP" : "DOWN",
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
