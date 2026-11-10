package com.vyson.urlshortener.service;

import com.vyson.urlshortener.dto.ShortenResponse;
import com.vyson.urlshortener.entity.UrlMapping;
import com.vyson.urlshortener.exception.InvalidUrlException;
import com.vyson.urlshortener.exception.UrlNotFoundException;
import com.vyson.urlshortener.repository.UrlMappingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UrlShortenerService {

    private static final String BASE62_ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int CODE_LENGTH = 7;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UrlMappingRepository repository;

    public UrlShortenerService(UrlMappingRepository repository) {
        this.repository = repository;
    }

    /**
     * Shortens a given URL. If the URL has already been shortened,
     * the existing short code is returned (idempotent / duplicate URL handling - Q7).
     */
    @Transactional
    public ShortenResponse shortenUrl(String originalUrl, String baseUrl) {
        validateUrl(originalUrl);
        String normalizedUrl = originalUrl.trim();

        // Check if the URL is already present in the database (Q7)
        Optional<UrlMapping> existing = repository.findByOriginalUrl(normalizedUrl);
        if (existing.isPresent()) {
            UrlMapping mapping = existing.get();
            return buildResponse(mapping, baseUrl);
        }

        // Generate a unique short code
        String shortCode = generateUniqueShortCode();

        UrlMapping mapping = new UrlMapping(shortCode, normalizedUrl, LocalDateTime.now());
        UrlMapping saved = repository.save(mapping);

        return buildResponse(saved, baseUrl);
    }

    /**
     * Retrieves the original URL corresponding to a short code.
     * Throws UrlNotFoundException if the code does not exist (Q8).
     */
    @Transactional(readOnly = true)
    public String getOriginalUrl(String shortCode) {
        if (shortCode == null || shortCode.isBlank()) {
            throw new UrlNotFoundException("Short code cannot be empty");
        }

        UrlMapping mapping = repository.findByShortCode(shortCode.trim())
                .orElseThrow(() -> new UrlNotFoundException("Short code '" + shortCode + "' not found"));

        return mapping.getOriginalUrl();
    }

    /**
     * Deletes a URL mapping by its short code (Q9).
     * Throws UrlNotFoundException if the code does not exist.
     */
    @Transactional
    public void deleteShortCode(String shortCode) {
        if (shortCode == null || shortCode.isBlank()) {
            throw new UrlNotFoundException("Short code cannot be empty");
        }

        UrlMapping mapping = repository.findByShortCode(shortCode.trim())
                .orElseThrow(() -> new UrlNotFoundException("Short code '" + shortCode + "' not found"));

        repository.delete(mapping);
    }

    /**
     * Generates a random Base62 alphanumeric code with collision resolution.
     */
    private String generateUniqueShortCode() {
        int maxAttempts = 10;
        for (int i = 0; i < maxAttempts; i++) {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int j = 0; j < CODE_LENGTH; j++) {
                int index = RANDOM.nextInt(BASE62_ALPHABET.length());
                sb.append(BASE62_ALPHABET.charAt(index));
            }
            String code = sb.toString();
            if (!repository.existsByShortCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Failed to generate a unique short code after " + maxAttempts + " attempts.");
    }

    /**
     * Validates that the input is a non-empty, syntactically valid HTTP/HTTPS URL.
     */
    private void validateUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new InvalidUrlException("URL must not be null or blank");
        }

        String trimmed = url.trim();
        try {
            URI uri = new URI(trimmed);
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                throw new InvalidUrlException("URL must start with http:// or https://");
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new InvalidUrlException("URL must contain a valid host");
            }
        } catch (URISyntaxException e) {
            throw new InvalidUrlException("Invalid URL syntax: " + e.getMessage());
        }
    }

    private ShortenResponse buildResponse(UrlMapping mapping, String baseUrl) {
        String shortUrl = baseUrl + "/redirect?code=" + mapping.getShortCode();
        return new ShortenResponse(mapping.getShortCode(), mapping.getOriginalUrl(), shortUrl, mapping.getCreatedAt());
    }
}
