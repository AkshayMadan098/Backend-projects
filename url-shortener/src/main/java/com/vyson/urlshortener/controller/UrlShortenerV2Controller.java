package com.vyson.urlshortener.controller;

import com.vyson.urlshortener.dto.*;
import com.vyson.urlshortener.entity.User;
import com.vyson.urlshortener.exception.InvalidApiKeyException;
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
 * Modern API v2 Controller.
 * Base Path: /v2, /api/v2
 *
 * Characteristics:
 * - Mandatory Authentication (API Key required for shortening, editing, deleting, dashboards).
 * - Enriched response schema (ShortenV2Response) with apiVersion, owner details, lifecycle dates, click analytics.
 * - Enterprise tier bulk operations.
 */
@RestController
@RequestMapping({"/v2", "/api/v2"})
public class UrlShortenerV2Controller {

    private final UrlShortenerService service;

    public UrlShortenerV2Controller(UrlShortenerService service) {
        this.service = service;
    }

    @GetMapping({"", "/"})
    public ResponseEntity<Map<String, Object>> v2Root(HttpServletRequest request) {
        return ResponseEntity.ok(Map.of(
                "apiVersion", "v2.0",
                "status", "ACTIVE",
                "features", List.of(
                        "mandatory-api-key-authentication",
                        "enriched-metadata-responses",
                        "custom-short-codes",
                        "expiration-lifecycle",
                        "password-protection",
                        "enterprise-bulk-creation"
                ),
                "endpoints", Map.of(
                        "POST /v2/shorten", "Create short URL (Requires X-API-Key)",
                        "POST /v2/shorten/bulk", "Enterprise bulk creation",
                        "GET /v2/redirect?code={code}", "Redirect to target URL",
                        "PUT /v2/shorten/{code}", "Edit existing short code (Owner only)",
                        "DELETE /v2/shorten/{code}", "Soft-delete short code (Owner only)",
                        "GET /v2/users/urls", "User links dashboard",
                        "GET /v2/health", "System & component diagnostics"
                )
        ));
    }

    @PostMapping("/shorten")
    public ResponseEntity<ShortenV2Response> shortenUrl(
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

        User authenticatedUser = (User) servletRequest.getAttribute(MiddlewareConstants.AUTHENTICATED_USER_ATTR);
        if (authenticatedUser == null && (apiKey == null || apiKey.isBlank())) {
            throw new InvalidApiKeyException("API key is required for API v2");
        }

        String baseUrl = extractBaseUrl(servletRequest);
        ShortenV2Response response = service.shortenUrlV2(effectiveRequest, authenticatedUser, apiKey, baseUrl);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/shorten/bulk")
    public ResponseEntity<ShortenBulkV2Response> bulkShorten(
            @Valid @RequestBody ShortenBulkRequest bulkRequest,
            @RequestHeader(name = "X-API-Key", required = false) String apiKey,
            HttpServletRequest servletRequest) {

        String baseUrl = extractBaseUrl(servletRequest);
        User authenticatedUser = (User) servletRequest.getAttribute(MiddlewareConstants.AUTHENTICATED_USER_ATTR);
        ShortenBulkV2Response response = service.bulkShortenUrlsV2(bulkRequest, authenticatedUser, apiKey, baseUrl);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/shorten/{code}")
    public ResponseEntity<ShortenV2Response> editShortCode(
            @PathVariable("code") String code,
            @RequestBody ShortenEditRequest editRequest,
            @RequestHeader(name = "X-API-Key", required = false) String apiKey,
            HttpServletRequest servletRequest) {

        String baseUrl = extractBaseUrl(servletRequest);
        User authenticatedUser = (User) servletRequest.getAttribute(MiddlewareConstants.AUTHENTICATED_USER_ATTR);
        ShortenV2Response response = service.editShortCodeV2(code, editRequest, authenticatedUser, apiKey, baseUrl);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/shorten/{code}")
    public ResponseEntity<Map<String, String>> deleteShortCode(
            @PathVariable("code") String code,
            @RequestHeader(name = "X-API-Key", required = false) String apiKey,
            HttpServletRequest servletRequest) {

        User authenticatedUser = (User) servletRequest.getAttribute(MiddlewareConstants.AUTHENTICATED_USER_ATTR);
        service.deleteShortCode(code, authenticatedUser, apiKey);
        return ResponseEntity.ok(Map.of(
                "apiVersion", "v2.0",
                "message", "Short code '" + code + "' deleted successfully",
                "code", code
        ));
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
    public ResponseEntity<List<ShortenV2Response>> getUserUrls(
            @RequestHeader(name = "X-API-Key", required = false) String apiKey,
            HttpServletRequest servletRequest) {

        String baseUrl = extractBaseUrl(servletRequest);
        User authenticatedUser = (User) servletRequest.getAttribute(MiddlewareConstants.AUTHENTICATED_USER_ATTR);
        List<ShortenV2Response> urls = service.getUserUrlsV2(authenticatedUser, apiKey, baseUrl);
        return ResponseEntity.ok(urls);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        boolean dbHealthy = service.checkHealth();
        return ResponseEntity.ok(Map.of(
                "apiVersion", "v2.0",
                "status", dbHealthy ? "UP" : "DOWN",
                "database", dbHealthy ? "UP" : "DOWN",
                "timestamp", LocalDateTime.now(),
                "components", Map.of(
                        "sqlite", Map.of("status", dbHealthy ? "UP" : "DOWN", "walMode", true),
                        "versioning", Map.of("activeVersion", "v2.0", "supportedVersions", List.of("v1", "v2"))
                )
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
