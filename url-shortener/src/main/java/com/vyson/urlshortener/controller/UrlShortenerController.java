package com.vyson.urlshortener.controller;

import com.vyson.urlshortener.dto.ShortenRequest;
import com.vyson.urlshortener.dto.ShortenResponse;
import com.vyson.urlshortener.service.UrlShortenerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class UrlShortenerController {

    private final UrlShortenerService service;

    public UrlShortenerController(UrlShortenerService service) {
        this.service = service;
    }

    /**
     * Endpoint 1: POST /shorten
     * Accepts a long URL and returns a shortened code and redirect URL.
     */
    @PostMapping("/shorten")
    public ResponseEntity<ShortenResponse> shortenUrl(
            @Valid @RequestBody(required = false) ShortenRequest request,
            @RequestParam(name = "url", required = false) String urlParam,
            HttpServletRequest servletRequest) {

        String urlToShorten = null;
        if (request != null && request.getUrl() != null && !request.getUrl().isBlank()) {
            urlToShorten = request.getUrl();
        } else if (urlParam != null && !urlParam.isBlank()) {
            urlToShorten = urlParam;
        } else {
            throw new IllegalArgumentException("URL must be provided either in request body as JSON { \"url\": \"...\" } or as query parameter ?url=...");
        }

        String baseUrl = extractBaseUrl(servletRequest);
        ShortenResponse response = service.shortenUrl(urlToShorten, baseUrl);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Endpoint 2: GET /redirect?code={code}
     * Redirects (HTTP 302 Found) to the original URL.
     */
    @GetMapping("/redirect")
    public ResponseEntity<Void> redirectToOriginalUrl(@RequestParam("code") String code) {
        String originalUrl = service.getOriginalUrl(code);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, originalUrl)
                .build();
    }

    /**
     * Endpoint 3: DELETE /shorten/{code} (RESTful path variable)
     * Endpoint 3b: DELETE /shorten?code={code} (Query param variant)
     * Deletes the short code mapping (Q9).
     */
    @DeleteMapping({"/shorten/{code}", "/shorten"})
    public ResponseEntity<Map<String, String>> deleteShortCode(
            @PathVariable(name = "code", required = false) String codePath,
            @RequestParam(name = "code", required = false) String codeParam) {

        String code = (codePath != null && !codePath.isBlank()) ? codePath : codeParam;
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Short code to delete must be specified in the URL path (/shorten/{code}) or query parameter (/shorten?code={code})");
        }

        service.deleteShortCode(code);
        return ResponseEntity.ok(Map.of(
                "message", "Short code '" + code + "' deleted successfully",
                "code", code
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
