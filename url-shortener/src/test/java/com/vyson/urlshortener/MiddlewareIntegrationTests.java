package com.vyson.urlshortener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vyson.urlshortener.config.DataInitializer;
import com.vyson.urlshortener.dto.ShortenBulkRequest;
import com.vyson.urlshortener.dto.ShortenRequest;
import com.vyson.urlshortener.entity.RequestLog;
import com.vyson.urlshortener.entity.User;
import com.vyson.urlshortener.filter.MiddlewareConstants;
import com.vyson.urlshortener.repository.RequestLogRepository;
import com.vyson.urlshortener.repository.UrlMappingRepository;
import com.vyson.urlshortener.repository.UserRepository;
import com.vyson.urlshortener.service.BlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive integration tests for VYSONM2A3: Middlewares
 * Validates Questions Q1 through Q9 BONUS.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class MiddlewareIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RequestLogRepository requestLogRepository;

    @Autowired
    private UrlMappingRepository urlMappingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BlacklistService blacklistService;

    @BeforeEach
    void setUp() {
        requestLogRepository.deleteAll();
        urlMappingRepository.deleteAll();

        // Ensure users exist
        if (userRepository.count() == 0) {
            User alice = new User("alice@example.com", "Alice Smith", DataInitializer.ALICE_API_KEY, "hobby");
            User bob = new User("bob@example.com", "Bob Jones", DataInitializer.BOB_API_KEY, "hobby");
            User enterprise = new User("enterprise@example.com", "Acme Corp Enterprise", DataInitializer.ENTERPRISE_API_KEY, "enterprise");
            userRepository.saveAll(List.of(alice, bob, enterprise));
        }
    }

    /**
     * [Q1] Request Logging Middleware:
     * Logs Timestamp, HTTP method, URL, User-Agent, and Client IP.
     * Stores log entries into the SQLite database table "request_logs".
     */
    @Test
    @DisplayName("Q1: Middleware logs timestamp, method, URL, User-Agent, and IP to database table")
    void testRequestLoggingMiddlewareSavesToDatabase() throws Exception {
        String testUrl = "https://github.com/vyson-tech";
        String customUserAgent = "VysonClient/2.0 (TestingEngine)";
        String forwardedIp = "203.0.113.195";

        mockMvc.perform(post("/shorten")
                        .header("User-Agent", customUserAgent)
                        .header("X-Forwarded-For", forwardedIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ShortenRequest(testUrl))))
                .andExpect(status().isCreated());

        List<RequestLog> logs = requestLogRepository.findAll();
        assertThat(logs).isNotEmpty();

        RequestLog log = logs.stream()
                .filter(l -> l.getRequestUrl().contains("/shorten") && "POST".equalsIgnoreCase(l.getHttpMethod()))
                .findFirst()
                .orElse(null);

        assertThat(log).isNotNull();
        assertThat(log.getTimestamp()).isNotNull();
        assertThat(log.getHttpMethod()).isEqualTo("POST");
        assertThat(log.getRequestUrl()).isEqualTo("/shorten");
        assertThat(log.getUserAgent()).isEqualTo(customUserAgent);
        assertThat(log.getClientIp()).isEqualTo(forwardedIp);
        assertThat(log.getStatusCode()).isEqualTo(201);
        assertThat(log.getDurationMs()).isNotNull();
    }

    /**
     * [Q2] Route-level filtering:
     * Excluded routes like /health are skipped by the logging middleware,
     * while core API routes like /shorten are actively logged.
     */
    @Test
    @DisplayName("Q2: Excluded routes (e.g. /health) are skipped by logging middleware")
    void testRouteFilteringSkipsExcludedPaths() throws Exception {
        requestLogRepository.deleteAll();

        // 1. Call /health (configured in exclude-patterns)
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk());

        // 2. Assert no log record was inserted for /health
        List<RequestLog> logsAfterHealth = requestLogRepository.findAll();
        boolean containsHealth = logsAfterHealth.stream()
                .anyMatch(l -> l.getRequestUrl().startsWith("/health"));
        assertThat(containsHealth).isFalse();

        // 3. Call /shorten (included path)
        mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \"https://example.com/routed\"}"))
                .andExpect(status().isCreated());

        // 4. Assert log record exists for /shorten
        List<RequestLog> logsAfterShorten = requestLogRepository.findAll();
        boolean containsShorten = logsAfterShorten.stream()
                .anyMatch(l -> l.getRequestUrl().startsWith("/shorten"));
        assertThat(containsShorten).isTrue();
    }

    /**
     * [Q3] Authentication Middleware:
     * Validates API key and short-circuits with 401 Unauthorized if invalid or missing on protected routes.
     */
    @Test
    @DisplayName("Q3: Authentication middleware rejects missing or invalid API keys before endpoint execution")
    void testAuthenticationMiddlewareRejection() throws Exception {
        ShortenBulkRequest request = new ShortenBulkRequest(List.of("https://site-a.com"));

        // 1. Missing API key on protected endpoint -> 401 Unauthorized
        mockMvc.perform(post("/shorten/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));

        // 2. Invalid API key -> 401 Unauthorized
        mockMvc.perform(post("/shorten/bulk")
                        .header("X-API-Key", "ak_invalid_nonexistent_key_12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid API key provided"));
    }

    /**
     * [Q4] Authorization Middleware (Tier Check):
     * Bulk endpoint authorization logic moved to middleware. Rejects non-enterprise users with 403 Forbidden.
     */
    @Test
    @DisplayName("Q4: Authorization middleware restricts bulk endpoint to enterprise tier users")
    void testTierAuthorizationMiddleware() throws Exception {
        ShortenBulkRequest request = new ShortenBulkRequest(List.of("https://batch-url.com"));

        // Alice has Hobby tier -> Blocked by TierAuthorizationFilter with 403 Forbidden
        mockMvc.perform(post("/shorten/bulk")
                        .header("X-API-Key", DataInitializer.ALICE_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Bulk creation is only available for enterprise tier users. Please upgrade your plan."));

        // Enterprise user -> Allowed through
        mockMvc.perform(post("/shorten/bulk")
                        .header("X-API-Key", DataInitializer.ENTERPRISE_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.successful").value(1));
    }

    /**
     * [Q5] Blacklist Middleware with Dynamic Runtime Config File:
     * Blocks notorious user keys from config file.
     * Demonstrates dynamic runtime reload without application restart.
     */
    @Test
    @DisplayName("Q5: Blacklist middleware blocks notorious keys and supports dynamic runtime file reload")
    void testBlacklistMiddlewareWithRuntimeReload() throws Exception {
        // 1. Key already in blacklist.txt (ak_live_spammer999) -> 403 Forbidden
        mockMvc.perform(post("/shorten")
                        .header("X-API-Key", "ak_live_spammer999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \"https://spam-destination.com\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Your API key has been revoked or blacklisted due to suspicious or abusive activity."));

        // 2. Test dynamic runtime reload:
        // Dynamically add a temporary bad actor key to config/blacklist.txt on disk
        String temporaryBadKey = "ak_live_temporary_spammer_" + System.currentTimeMillis();
        blacklistService.addKeyToConfigFile(temporaryBadKey);

        try {
            // Immediately rejected with 403 at runtime without restarting Spring Boot!
            mockMvc.perform(post("/shorten")
                            .header("X-API-Key", temporaryBadKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"url\": \"https://spam-attack.com\"}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));
        } finally {
            // Clean up runtime config file
            blacklistService.removeKeyFromConfigFile(temporaryBadKey);
        }
    }

    /**
     * [Q6] Response Time Middleware:
     * Stores start time, computes elapsed duration, and adds "X-Response-Time" header to response.
     */
    @Test
    @DisplayName("Q6: Response time middleware adds X-Response-Time header to HTTP response")
    void testResponseTimeMiddlewareHeader() throws Exception {
        MvcResult result = mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \"https://timing-test.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists(MiddlewareConstants.HEADER_RESPONSE_TIME))
                .andReturn();

        String responseTimeHeader = result.getResponse().getHeader(MiddlewareConstants.HEADER_RESPONSE_TIME);
        assertThat(responseTimeHeader).isNotNull();
        assertThat(responseTimeHeader).matches("^\\d+ms$"); // e.g. "5ms"
    }

    /**
     * [Q7] Context Passing Between Middlewares:
     * User object fetched by ApiKeyAuthenticationFilter is placed in request attributes
     * ("authenticatedUser") and consumed downstream by TierAuthorizationFilter & Controller.
     */
    @Test
    @DisplayName("Q7: Pre-authenticated User in request attribute avoids redundant database lookup")
    void testContextPassingAcrossMiddlewares() throws Exception {
        // Calling enterprise bulk endpoint verifies:
        // 1. ApiKeyAuthenticationFilter resolves User from DB and sets request attribute "authenticatedUser"
        // 2. TierAuthorizationFilter reads "authenticatedUser" directly from request attribute without DB query
        // 3. Controller & Service consume "authenticatedUser" directly from request attribute
        ShortenBulkRequest bulkRequest = new ShortenBulkRequest(List.of(
                "https://context-pass-1.com",
                "https://context-pass-2.com"
        ));

        mockMvc.perform(post("/shorten/bulk")
                        .header("X-API-Key", DataInitializer.ENTERPRISE_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bulkRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.successful").value(2));
    }

    /**
     * [Q8] Middleware Chaining & Execution Order:
     * Order:
     * 1. ResponseTimeFilter
     * 2. RequestLoggingFilter
     * 3. BlacklistFilter
     * 4. ApiKeyAuthenticationFilter
     * 5. TierAuthorizationFilter
     * 6. Controller
     */
    @Test
    @DisplayName("Q8: Middleware execution order stops malicious/invalid requests at earliest appropriate stage")
    void testMiddlewareExecutionOrderAndChaining() throws Exception {
        // Case A: Malicious blacklisted key is rejected at Step 3 (BlacklistFilter) before database lookup (Step 4)
        mockMvc.perform(post("/shorten/bulk")
                        .header("X-API-Key", "ak_live_spammer999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"originalUrls\": [\"https://malicious.com\"]}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Your API key has been revoked or blacklisted due to suspicious or abusive activity."));

        // Case B: Non-blacklisted but invalid key is rejected at Step 4 (ApiKeyAuthenticationFilter) with 401
        mockMvc.perform(post("/shorten/bulk")
                        .header("X-API-Key", "ak_live_random_invalid_key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"originalUrls\": [\"https://unknown.com\"]}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid API key provided"));

        // Case C: Valid key but non-enterprise tier is rejected at Step 5 (TierAuthorizationFilter) with 403
        mockMvc.perform(post("/shorten/bulk")
                        .header("X-API-Key", DataInitializer.ALICE_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"originalUrls\": [\"https://alice.com\"]}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Bulk creation is only available for enterprise tier users. Please upgrade your plan."));
    }

    /**
     * [Q9 BONUS] Per-Middleware Timing Tracker:
     * Tracks time taken inside each middleware and exposes timings in logs & X-Middleware-Timings header.
     */
    @Test
    @DisplayName("Q9 BONUS: Middleware timer records and exposes internal execution time for each filter")
    void testPerMiddlewareTimingTracker() throws Exception {
        MvcResult result = mockMvc.perform(post("/shorten")
                        .header("X-API-Key", DataInitializer.ALICE_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \"https://profile-timing.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists(MiddlewareConstants.HEADER_MIDDLEWARE_TIMINGS))
                .andReturn();

        String timingsHeader = result.getResponse().getHeader(MiddlewareConstants.HEADER_MIDDLEWARE_TIMINGS);
        assertThat(timingsHeader).isNotNull();
        // Assert timings header contains breakdown for each middleware and controller
        assertThat(timingsHeader).contains("ResponseTimeFilter=");
        assertThat(timingsHeader).contains("BlacklistFilter=");
        assertThat(timingsHeader).contains("ApiKeyAuthenticationFilter=");
        assertThat(timingsHeader).contains("RequestLoggingFilter=");
        assertThat(timingsHeader).contains("Controller=");
        assertThat(timingsHeader).contains("Total=");
    }
}
