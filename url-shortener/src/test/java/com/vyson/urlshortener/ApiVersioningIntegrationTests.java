package com.vyson.urlshortener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vyson.urlshortener.config.DataInitializer;
import com.vyson.urlshortener.dto.ShortenBulkRequest;
import com.vyson.urlshortener.dto.ShortenEditRequest;
import com.vyson.urlshortener.dto.ShortenRequest;
import com.vyson.urlshortener.entity.User;
import com.vyson.urlshortener.filter.MiddlewareConstants;
import com.vyson.urlshortener.repository.UrlMappingRepository;
import com.vyson.urlshortener.repository.UserRepository;
import com.vyson.urlshortener.versioning.ApiVersioningProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive Integration Tests for VYSONM2A4: API Versioning
 * Validates:
 * - URL Path Versioning (/v1 vs /v2)
 * - Header Versioning (X-API-Version: v1 | v2)
 * - Query Parameter Versioning (?version=v1 | ?version=v2)
 * - Content Negotiation (Accept: application/vnd.vyson.v2+json)
 * - RFC 8594 (Deprecation, Sunset) & RFC 7234 (Warning) headers
 * - Breaking Change enforcement in v2 (mandatory authentication)
 * - Q14 BONUS: Default V1 strategy vs Strict Mode error rejection
 */
@SpringBootTest
@AutoConfigureMockMvc
public class ApiVersioningIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UrlMappingRepository urlMappingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApiVersioningProperties versioningProperties;

    @BeforeEach
    void setUp() {
        urlMappingRepository.deleteAll();

        // Ensure sample users exist
        if (userRepository.count() == 0) {
            User alice = new User("alice@example.com", "Alice Smith", DataInitializer.ALICE_API_KEY, "hobby");
            User bob = new User("bob@example.com", "Bob Jones", DataInitializer.BOB_API_KEY, "hobby");
            User enterprise = new User("enterprise@example.com", "Acme Corp Enterprise", DataInitializer.ENTERPRISE_API_KEY, "enterprise");
            userRepository.saveAll(List.of(alice, bob, enterprise));
        }

        // Reset default strategy
        versioningProperties.setUnspecifiedStrategy(ApiVersioningProperties.UnspecifiedStrategy.DEFAULT_V1);
    }

    /**
     * [Q11 & V1 Baseline] V1 allows anonymous link shortening (backward compatibility).
     */
    @Test
    @DisplayName("V1: Allows anonymous link shortening without API key")
    void testV1AnonymousShortenSucceeds() throws Exception {
        String originalUrl = "https://example.com/docs/v1";
        ShortenRequest request = new ShortenRequest(originalUrl);

        mockMvc.perform(post("/v1/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode").isString())
                .andExpect(jsonPath("$.originalUrl").value(originalUrl))
                .andExpect(header().string(MiddlewareConstants.HEADER_API_VERSION, "v1"));
    }

    /**
     * [Deprecation Headers] V1 responses include RFC 8594 and RFC 7234 deprecation headers.
     */
    @Test
    @DisplayName("V1: Injects RFC 8594 (Deprecation, Sunset) and RFC 7234 (Warning) headers")
    void testV1ResponseIncludesDeprecationAndSunsetHeaders() throws Exception {
        mockMvc.perform(get("/v1/health"))
                .andExpect(status().isOk())
                .andExpect(header().string(MiddlewareConstants.HEADER_API_VERSION, "v1"))
                .andExpect(header().exists(MiddlewareConstants.HEADER_DEPRECATION))
                .andExpect(header().string(MiddlewareConstants.HEADER_SUNSET, versioningProperties.getSunsetDate()))
                .andExpect(header().string(MiddlewareConstants.HEADER_WARNING, org.hamcrest.Matchers.containsString("deprecated")))
                .andExpect(header().string(MiddlewareConstants.HEADER_LINK, versioningProperties.getSuccessorLink()));
    }

    /**
     * [Breaking Change in V2] Calling /v2/shorten without API key returns 401 Unauthorized.
     */
    @Test
    @DisplayName("V2: Strictly requires API key authentication on URL creation (breaking change)")
    void testV2RequiresApiKeyAuthentication() throws Exception {
        String originalUrl = "https://example.com/secure";
        ShortenRequest request = new ShortenRequest(originalUrl);

        mockMvc.perform(post("/v2/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(header().string(MiddlewareConstants.HEADER_API_VERSION, "v2"));
    }

    /**
     * [V2 Enriched Schema] Calling /v2/shorten with valid API key returns ShortenV2Response.
     */
    @Test
    @DisplayName("V2: Authenticated call succeeds and returns enriched V2 response schema")
    void testV2WithValidApiKeySucceedsAndReturnsEnrichedSchema() throws Exception {
        String originalUrl = "https://example.com/v2-schema-test";
        ShortenRequest request = new ShortenRequest(originalUrl);

        MvcResult result = mockMvc.perform(post("/v2/shorten")
                        .header("X-API-Key", DataInitializer.ALICE_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string(MiddlewareConstants.HEADER_API_VERSION, "v2"))
                .andExpect(jsonPath("$.apiVersion").value("v2.0"))
                .andExpect(jsonPath("$.shortCode").isString())
                .andExpect(jsonPath("$.originalUrl").value(originalUrl))
                .andExpect(jsonPath("$.owner.email").value("alice@example.com"))
                .andExpect(jsonPath("$.owner.name").value("Alice Smith"))
                .andExpect(jsonPath("$.owner.tier").value("hobby"))
                .andReturn();

        // Deprecation headers should NOT be present on v2 responses
        assertThat(result.getResponse().getHeader(MiddlewareConstants.HEADER_SUNSET)).isNull();
    }

    /**
     * [Header Versioning] Passing X-API-Version: v2 to root /shorten enforces V2 behavior.
     */
    @Test
    @DisplayName("Header Versioning: X-API-Version: v2 routes unversioned URI to V2 engine")
    void testHeaderBasedVersioningV2() throws Exception {
        String originalUrl = "https://example.com/header-test";
        ShortenRequest request = new ShortenRequest(originalUrl);

        // 1. Without API key -> 401 Unauthorized
        mockMvc.perform(post("/shorten")
                        .header("X-API-Version", "v2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(MiddlewareConstants.HEADER_API_VERSION, "v2"));

        // 2. With API key -> 201 Created with V2 payload
        mockMvc.perform(post("/shorten")
                        .header("X-API-Version", "v2")
                        .header("X-API-Key", DataInitializer.BOB_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string(MiddlewareConstants.HEADER_API_VERSION, "v2"))
                .andExpect(jsonPath("$.apiVersion").value("v2.0"))
                .andExpect(jsonPath("$.owner.email").value("bob@example.com"));
    }

    /**
     * [Header Versioning] Passing X-API-Version: v1 to root /shorten enforces V1 behavior.
     */
    @Test
    @DisplayName("Header Versioning: X-API-Version: v1 serves legacy contract with sunset headers")
    void testHeaderBasedVersioningV1() throws Exception {
        String originalUrl = "https://example.com/header-v1";
        ShortenRequest request = new ShortenRequest(originalUrl);

        mockMvc.perform(post("/shorten")
                        .header("X-API-Version", "v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string(MiddlewareConstants.HEADER_API_VERSION, "v1"))
                .andExpect(header().exists(MiddlewareConstants.HEADER_SUNSET))
                .andExpect(jsonPath("$.shortCode").isString());
    }

    /**
     * [Query Param Versioning] Query parameter ?version=v2 or ?version=v1 routes properly.
     */
    @Test
    @DisplayName("Query Parameter Versioning: ?version=v2 activates V2 engine")
    void testQueryParamVersioning() throws Exception {
        String originalUrl = "https://example.com/query-test";
        ShortenRequest request = new ShortenRequest(originalUrl);

        // V2 via query param requires API key
        mockMvc.perform(post("/shorten?version=v2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        // V2 via query param with API key succeeds
        mockMvc.perform(post("/shorten?version=v2")
                        .header("X-API-Key", DataInitializer.ALICE_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.apiVersion").value("v2.0"));
    }

    /**
     * [Content Negotiation] Accept header application/vnd.vyson.v2+json routes to V2.
     */
    @Test
    @DisplayName("Content Negotiation: Accept header activates V2 engine")
    void testAcceptHeaderVersioning() throws Exception {
        String originalUrl = "https://example.com/accept-test";
        ShortenRequest request = new ShortenRequest(originalUrl);

        mockMvc.perform(post("/shorten")
                        .header(HttpHeaders.ACCEPT, "application/vnd.vyson.v2+json")
                        .header("X-API-Key", DataInitializer.ALICE_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.apiVersion").value("v2.0"));
    }

    /**
     * [Q14 BONUS: Option 2] Default V1 strategy for unversioned requests.
     * Prevents breaking changes for existing consumers while notifying them via Sunset headers.
     */
    @Test
    @DisplayName("Q14 BONUS (Option 2): Unspecified version smoothly defaults to V1 with deprecation warnings")
    void testDefaultV1StrategyForUnversionedCalls() throws Exception {
        versioningProperties.setUnspecifiedStrategy(ApiVersioningProperties.UnspecifiedStrategy.DEFAULT_V1);

        String originalUrl = "https://example.com/legacy-client";
        ShortenRequest request = new ShortenRequest(originalUrl);

        mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string(MiddlewareConstants.HEADER_API_VERSION, "v1"))
                .andExpect(header().exists(MiddlewareConstants.HEADER_SUNSET))
                .andExpect(header().exists(MiddlewareConstants.HEADER_WARNING));
    }

    /**
     * [Q14 BONUS: Option 1] Strict Mode rejection for unversioned requests.
     * Returns HTTP 400 Bad Request if client fails to declare API version.
     */
    @Test
    @DisplayName("Q14 BONUS (Option 1): Strict mode rejects unversioned request with HTTP 400 Bad Request")
    void testStrictModeRejectionForUnversionedCalls() throws Exception {
        versioningProperties.setUnspecifiedStrategy(ApiVersioningProperties.UnspecifiedStrategy.REJECT_WITH_ERROR);

        String originalUrl = "https://example.com/strict-test";
        ShortenRequest request = new ShortenRequest(originalUrl);

        mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("API version is required")));
    }

    /**
     * [V2 Health Diagnostics] /v2/health returns enriched subsystem diagnostics.
     */
    @Test
    @DisplayName("V2 Health: Returns comprehensive subsystem diagnostics")
    void testV2HealthCheckReturnsEnrichedDiagnostics() throws Exception {
        mockMvc.perform(get("/v2/health"))
                .andExpect(status().isOk())
                .andExpect(header().string(MiddlewareConstants.HEADER_API_VERSION, "v2"))
                .andExpect(jsonPath("$.apiVersion").value("v2.0"))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.sqlite.status").value("UP"))
                .andExpect(jsonPath("$.components.versioning.activeVersion").value("v2.0"));
    }

    /**
     * [V2 Enterprise Bulk] Bulk creation on /v2/shorten/bulk requires Enterprise tier.
     */
    @Test
    @DisplayName("V2 Bulk: Enterprise key succeeds; hobby tier rejected with 403 Forbidden")
    void testV2BulkShortenEnterpriseOnly() throws Exception {
        ShortenBulkRequest bulkRequest = new ShortenBulkRequest(List.of(
                "https://example.com/bulk1",
                "https://example.com/bulk2"
        ));

        // 1. Hobby key -> 403 Forbidden
        mockMvc.perform(post("/v2/shorten/bulk")
                        .header("X-API-Key", DataInitializer.ALICE_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bulkRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));

        // 2. Enterprise key -> 201 Created with ShortenBulkV2Response
        mockMvc.perform(post("/v2/shorten/bulk")
                        .header("X-API-Key", DataInitializer.ENTERPRISE_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bulkRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().string(MiddlewareConstants.HEADER_API_VERSION, "v2"))
                .andExpect(jsonPath("$.apiVersion").value("v2.0"))
                .andExpect(jsonPath("$.count").value(2))
                .andExpect(jsonPath("$.results[0].owner.tier").value("enterprise"));
    }

    /**
     * [V2 Edit Short Code] PUT /v2/shorten/{code} edits target URL and returns V2 schema.
     */
    @Test
    @DisplayName("V2 Edit: Updates target URL with owner key and returns ShortenV2Response")
    void testV2EditShortCode() throws Exception {
        // First create via v2
        ShortenRequest createReq = new ShortenRequest("https://example.com/initial");
        MvcResult createRes = mockMvc.perform(post("/v2/shorten")
                        .header("X-API-Key", DataInitializer.ALICE_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String code = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("shortCode").asText();

        // Edit via v2
        ShortenEditRequest editReq = new ShortenEditRequest();
        editReq.setOriginalUrl("https://example.com/updated");

        mockMvc.perform(put("/v2/shorten/" + code)
                        .header("X-API-Key", DataInitializer.ALICE_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(editReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiVersion").value("v2.0"))
                .andExpect(jsonPath("$.originalUrl").value("https://example.com/updated"))
                .andExpect(jsonPath("$.owner.email").value("alice@example.com"));
    }
}
