package com.vyson.urlshortener.service;

import com.vyson.urlshortener.dto.*;
import com.vyson.urlshortener.entity.UrlMapping;
import com.vyson.urlshortener.entity.User;
import com.vyson.urlshortener.exception.*;
import com.vyson.urlshortener.repository.UrlMappingRepository;
import com.vyson.urlshortener.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UrlShortenerService {

    private static final String BASE62_ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int CODE_LENGTH = 7;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UrlMappingRepository repository;
    private final UserRepository userRepository;

    public UrlShortenerService(UrlMappingRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    /**
     * Shortens a given URL.
     * M2A2 Updates:
     * - [Q4] Multiple short codes for duplicate URLs are now allowed (no deduplication return).
     * - [Q5] Links user if API key is provided.
     * - [Q8] Supports optional expiration date.
     * - [Q9] Supports optional custom short code with collision handling.
     * - [Q14] Supports optional password protection.
     */
    @Transactional
    public ShortenResponse shortenUrl(ShortenRequest request, User authenticatedUser, String apiKey, String baseUrl) {
        if (request == null || request.getUrl() == null || request.getUrl().isBlank()) {
            throw new InvalidUrlException("URL must not be null or blank");
        }

        validateUrl(request.getUrl());
        String normalizedUrl = request.getUrl().trim();

        // [Q7] Resolve authenticated user: reuse user set by middleware if present, else resolve from API key
        User owner = authenticatedUser != null ? authenticatedUser : resolveUser(apiKey);

        String shortCode;
        if (request.getCustomCode() != null && !request.getCustomCode().isBlank()) {
            // [Q9] Custom short code
            String custom = request.getCustomCode().trim();
            validateCustomCode(custom);
            if (repository.existsByShortCodeAndIsDeletedFalse(custom)) {
                throw new CustomCodeAlreadyExistsException("Custom code '" + custom + "' already in use, please choose another");
            }
            shortCode = custom;
        } else {
            // Random Base62 code
            shortCode = generateUniqueShortCode();
        }

        UrlMapping mapping = new UrlMapping(shortCode, normalizedUrl, LocalDateTime.now());
        mapping.setUser(owner);
        mapping.setExpiresAt(request.getExpiresAt());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            mapping.setPassword(request.getPassword().trim());
        }

        UrlMapping saved = repository.save(mapping);
        return buildResponse(saved, baseUrl);
    }

    @Transactional
    public ShortenResponse shortenUrl(ShortenRequest request, String apiKey, String baseUrl) {
        return shortenUrl(request, null, apiKey, baseUrl);
    }

    /**
     * Backward-compatible helper for basic shorten calls
     */
    @Transactional
    public ShortenResponse shortenUrl(String originalUrl, String baseUrl) {
        return shortenUrl(new ShortenRequest(originalUrl), null, null, baseUrl);
    }

    /**
     * [Q10 & Q12] Bulk creation of short codes for enterprise users.
     * [Q7] Consumes pre-authenticated user from middleware attribute to avoid duplicate DB query.
     */
    @Transactional
    public ShortenBulkResponse bulkShortenUrls(ShortenBulkRequest bulkRequest, User authenticatedUser, String apiKey, String baseUrl) {
        User user = authenticatedUser != null ? authenticatedUser : resolveUser(apiKey);
        if (user == null) {
            throw new InvalidApiKeyException("API key is required for bulk creation");
        }

        // [Q4 & Q12] Authorization tier check (defense-in-depth with TierAuthorizationFilter)
        if (!"enterprise".equalsIgnoreCase(user.getTier())) {
            throw new ForbiddenAccessException("Bulk creation is only available for enterprise tier users. Please upgrade your plan.");
        }

        List<ShortenResponse> results = new ArrayList<>();
        for (String url : bulkRequest.getOriginalUrls()) {
            if (url == null || url.isBlank()) {
                throw new InvalidUrlException("One of the URLs in bulk request is null or blank");
            }
            validateUrl(url);
            String code = generateUniqueShortCode();
            UrlMapping mapping = new UrlMapping(code, url.trim(), LocalDateTime.now());
            mapping.setUser(user);
            UrlMapping saved = repository.save(mapping);
            results.add(buildResponse(saved, baseUrl));
        }

        return new ShortenBulkResponse(results);
    }

    @Transactional
    public ShortenBulkResponse bulkShortenUrls(ShortenBulkRequest bulkRequest, String apiKey, String baseUrl) {
        return bulkShortenUrls(bulkRequest, null, apiKey, baseUrl);
    }

    /**
     * [Q3, Q8, Q14] Retrieves the original URL for redirection:
     * - Increments click counter (Q3).
     * - Updates last_accessed_at timestamp (Q3).
     * - Checks expiry date (Q8) -> returns 404 if expired.
     * - Verifies password if protected (Q14) -> returns 401 if unauthorized.
     */
    @Transactional
    public String getOriginalUrl(String shortCode, String password) {
        if (shortCode == null || shortCode.isBlank()) {
            throw new UrlNotFoundException("Short code cannot be empty");
        }

        UrlMapping mapping = repository.findByShortCodeAndIsDeletedFalse(shortCode.trim())
                .orElseThrow(() -> new UrlNotFoundException("Short code '" + shortCode + "' not found"));

        // [Q8] Expiry check
        if (mapping.isExpired()) {
            throw new UrlExpiredException("Short code '" + shortCode + "' has expired");
        }

        // [Q14] Password protection check
        if (mapping.hasPassword()) {
            if (password == null || !password.equals(mapping.getPassword())) {
                throw new PasswordRequiredException("Password required or incorrect for protected short code '" + shortCode + "'");
            }
        }

        // [Q3] Increment click counter & update last accessed time
        mapping.setClickCount(mapping.getClickCount() + 1);
        mapping.setLastAccessedAt(LocalDateTime.now());
        repository.save(mapping);

        return mapping.getOriginalUrl();
    }

    public String getOriginalUrl(String shortCode) {
        return getOriginalUrl(shortCode, null);
    }

    /**
     * [Q5] Soft-deletes a URL mapping with user ownership check.
     * [Q7] Accepts pre-authenticated user from middleware attribute.
     */
    @Transactional
    public void deleteShortCode(String shortCode, User authenticatedUser, String apiKey) {
        if (shortCode == null || shortCode.isBlank()) {
            throw new UrlNotFoundException("Short code cannot be empty");
        }

        UrlMapping mapping = repository.findByShortCodeAndIsDeletedFalse(shortCode.trim())
                .orElseThrow(() -> new UrlNotFoundException("Short code '" + shortCode + "' not found"));

        // Ownership verification (Q5)
        if (mapping.getUser() != null) {
            User requestingUser = authenticatedUser != null ? authenticatedUser : resolveUser(apiKey);
            if (requestingUser == null) {
                throw new InvalidApiKeyException("API key required to delete this short code");
            }

            if (!mapping.getUser().getId().equals(requestingUser.getId())) {
                throw new ForbiddenAccessException("You do not own this short code and cannot delete it");
            }
        }

        // Soft delete (Q5)
        mapping.setDeleted(true);
        mapping.setDeletedAt(LocalDateTime.now());
        repository.save(mapping);
    }

    @Transactional
    public void deleteShortCode(String shortCode, String apiKey) {
        deleteShortCode(shortCode, null, apiKey);
    }

    /**
     * [Q13] Allows editing a short code (URL, expiration date, password) with ownership check.
     * [Q7] Accepts pre-authenticated user from middleware attribute.
     */
    @Transactional
    public ShortenResponse editShortCode(String shortCode, ShortenEditRequest editRequest, User authenticatedUser, String apiKey, String baseUrl) {
        if (shortCode == null || shortCode.isBlank()) {
            throw new UrlNotFoundException("Short code cannot be empty");
        }

        UrlMapping mapping = repository.findByShortCodeAndIsDeletedFalse(shortCode.trim())
                .orElseThrow(() -> new UrlNotFoundException("Short code '" + shortCode + "' not found"));

        // Ownership check
        if (mapping.getUser() != null) {
            User requestingUser = authenticatedUser != null ? authenticatedUser : resolveUser(apiKey);
            if (requestingUser == null) {
                throw new InvalidApiKeyException("API key required to edit this short code");
            }

            if (!mapping.getUser().getId().equals(requestingUser.getId())) {
                throw new ForbiddenAccessException("You do not own this short code and cannot edit it");
            }
        } else if (authenticatedUser != null || (apiKey != null && !apiKey.isBlank())) {
            // Claim ownership if previously unowned
            User requestingUser = authenticatedUser != null ? authenticatedUser : resolveUser(apiKey);
            mapping.setUser(requestingUser);
        }

        // Update target URL if provided
        if (editRequest.getOriginalUrl() != null && !editRequest.getOriginalUrl().isBlank()) {
            validateUrl(editRequest.getOriginalUrl());
            mapping.setOriginalUrl(editRequest.getOriginalUrl().trim());
        }

        // Update expiry date (can be in past to deactivate)
        if (editRequest.getExpiresAt() != null) {
            mapping.setExpiresAt(editRequest.getExpiresAt());
        }

        // Update password (Q14)
        if (editRequest.getPassword() != null) {
            mapping.setPassword(editRequest.getPassword().isBlank() ? null : editRequest.getPassword().trim());
        }

        UrlMapping saved = repository.save(mapping);
        return buildResponse(saved, baseUrl);
    }

    @Transactional
    public ShortenResponse editShortCode(String shortCode, ShortenEditRequest editRequest, String apiKey, String baseUrl) {
        return editShortCode(shortCode, editRequest, null, apiKey, baseUrl);
    }

    /**
     * [Q15] Retrieves all URLs owned by an authenticated user.
     * [Q7] Accepts pre-authenticated user from middleware attribute.
     */
    @Transactional(readOnly = true)
    public List<ShortenResponse> getUserUrls(User authenticatedUser, String apiKey, String baseUrl) {
        User user = authenticatedUser != null ? authenticatedUser : resolveUser(apiKey);
        if (user == null) {
            throw new InvalidApiKeyException("API key is required to view your URLs");
        }

        List<UrlMapping> mappings = repository.findByUserAndIsDeletedFalseOrderByCreatedAtDesc(user);
        List<ShortenResponse> responses = new ArrayList<>();
        for (UrlMapping mapping : mappings) {
            responses.add(buildResponse(mapping, baseUrl));
        }
        return responses;
    }

    @Transactional(readOnly = true)
    public List<ShortenResponse> getUserUrls(String apiKey, String baseUrl) {
        return getUserUrls(null, apiKey, baseUrl);
    }

    /**
     * [Q16 BONUS] Health check verifying database connectivity.
     */
    @Transactional(readOnly = true)
    public boolean checkHealth() {
        try {
            repository.count();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private User resolveUser(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        return userRepository.findByApiKey(apiKey.trim())
                .orElseThrow(() -> new InvalidApiKeyException("Invalid API key provided"));
    }

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

    private void validateCustomCode(String code) {
        if (!code.matches("^[a-zA-Z0-9_-]{3,32}$")) {
            throw new IllegalArgumentException("Custom code must be between 3 and 32 alphanumeric characters, hyphens, or underscores.");
        }
    }

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

    public ShortenResponse buildResponse(UrlMapping mapping, String baseUrl) {
        String shortUrl = baseUrl + "/redirect?code=" + mapping.getShortCode();
        return new ShortenResponse(
                mapping.getShortCode(),
                mapping.getOriginalUrl(),
                shortUrl,
                mapping.getCreatedAt(),
                mapping.getClickCount(),
                mapping.getLastAccessedAt(),
                mapping.getExpiresAt(),
                mapping.hasPassword(),
                mapping.getDeleted()
        );
    }

    public ShortenV2Response buildV2Response(UrlMapping mapping, String baseUrl) {
        String shortUrl = baseUrl + "/redirect?code=" + mapping.getShortCode();
        ShortenV2Response.UserSummary ownerSummary = null;
        if (mapping.getUser() != null) {
            ownerSummary = new ShortenV2Response.UserSummary(
                    mapping.getUser().getEmail(),
                    mapping.getUser().getName(),
                    mapping.getUser().getTier()
            );
        }
        return new ShortenV2Response(
                "v2.0",
                mapping.getShortCode(),
                mapping.getOriginalUrl(),
                shortUrl,
                mapping.getCreatedAt(),
                mapping.getExpiresAt(),
                mapping.getClickCount(),
                mapping.hasPassword(),
                ownerSummary
        );
    }

    @Transactional
    public ShortenV2Response shortenUrlV2(ShortenRequest request, User authenticatedUser, String apiKey, String baseUrl) {
        if (request == null || request.getUrl() == null || request.getUrl().isBlank()) {
            throw new InvalidUrlException("URL must not be null or blank");
        }
        validateUrl(request.getUrl());
        String normalizedUrl = request.getUrl().trim();

        User owner = authenticatedUser != null ? authenticatedUser : resolveUser(apiKey);
        if (owner == null) {
            throw new InvalidApiKeyException("API key is required for API v2");
        }

        String shortCode;
        if (request.getCustomCode() != null && !request.getCustomCode().isBlank()) {
            String custom = request.getCustomCode().trim();
            validateCustomCode(custom);
            if (repository.existsByShortCodeAndIsDeletedFalse(custom)) {
                throw new CustomCodeAlreadyExistsException("Custom code '" + custom + "' already in use, please choose another");
            }
            shortCode = custom;
        } else {
            shortCode = generateUniqueShortCode();
        }

        UrlMapping mapping = new UrlMapping(shortCode, normalizedUrl, LocalDateTime.now());
        mapping.setUser(owner);
        mapping.setExpiresAt(request.getExpiresAt());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            mapping.setPassword(request.getPassword().trim());
        }

        UrlMapping saved = repository.save(mapping);
        return buildV2Response(saved, baseUrl);
    }

    @Transactional
    public ShortenBulkV2Response bulkShortenUrlsV2(ShortenBulkRequest bulkRequest, User authenticatedUser, String apiKey, String baseUrl) {
        User user = authenticatedUser != null ? authenticatedUser : resolveUser(apiKey);
        if (user == null) {
            throw new InvalidApiKeyException("API key is required for bulk creation");
        }
        if (!"enterprise".equalsIgnoreCase(user.getTier())) {
            throw new ForbiddenAccessException("Bulk creation is only available for enterprise tier users. Please upgrade your plan.");
        }

        List<ShortenV2Response> results = new ArrayList<>();
        for (String url : bulkRequest.getOriginalUrls()) {
            if (url == null || url.isBlank()) {
                throw new InvalidUrlException("One of the URLs in bulk request is null or blank");
            }
            validateUrl(url);
            String code = generateUniqueShortCode();
            UrlMapping mapping = new UrlMapping(code, url.trim(), LocalDateTime.now());
            mapping.setUser(user);
            UrlMapping saved = repository.save(mapping);
            results.add(buildV2Response(saved, baseUrl));
        }

        return new ShortenBulkV2Response(results);
    }

    @Transactional(readOnly = true)
    public List<ShortenV2Response> getUserUrlsV2(User authenticatedUser, String apiKey, String baseUrl) {
        User user = authenticatedUser != null ? authenticatedUser : resolveUser(apiKey);
        if (user == null) {
            throw new InvalidApiKeyException("API key is required to view your URLs");
        }

        List<UrlMapping> mappings = repository.findByUserAndIsDeletedFalseOrderByCreatedAtDesc(user);
        List<ShortenV2Response> responses = new ArrayList<>();
        for (UrlMapping mapping : mappings) {
            responses.add(buildV2Response(mapping, baseUrl));
        }
        return responses;
    }

    @Transactional
    public ShortenV2Response editShortCodeV2(String shortCode, ShortenEditRequest editRequest, User authenticatedUser, String apiKey, String baseUrl) {
        editShortCode(shortCode, editRequest, authenticatedUser, apiKey, baseUrl);
        UrlMapping mapping = repository.findByShortCodeAndIsDeletedFalse(shortCode.trim())
                .orElseThrow(() -> new UrlNotFoundException("Short code '" + shortCode + "' not found"));
        return buildV2Response(mapping, baseUrl);
    }
}
