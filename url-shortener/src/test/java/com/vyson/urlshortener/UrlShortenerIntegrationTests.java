package com.vyson.urlshortener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vyson.urlshortener.config.DataInitializer;
import com.vyson.urlshortener.dto.ShortenBulkRequest;
import com.vyson.urlshortener.dto.ShortenEditRequest;
import com.vyson.urlshortener.dto.ShortenRequest;
import com.vyson.urlshortener.entity.UrlMapping;
import com.vyson.urlshortener.entity.User;
import com.vyson.urlshortener.repository.UrlMappingRepository;
import com.vyson.urlshortener.repository.UserRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class UrlShortenerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UrlMappingRepository urlMappingRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        urlMappingRepository.deleteAll();

        // Ensure sample users exist for tests
        if (userRepository.count() == 0) {
            User alice = new User("alice@example.com", "Alice Smith", DataInitializer.ALICE_API_KEY, "hobby");
            User bob = new User("bob@example.com", "Bob Jones", DataInitializer.BOB_API_KEY, "hobby");
            User enterprise = new User("enterprise@example.com", "Acme Corp Enterprise", DataInitializer.ENTERPRISE_API_KEY, "enterprise");
            userRepository.saveAll(List.of(alice, bob, enterprise));
        }
    }

    /**
     * Basic Integration Flow:
     * 1. Call POST /shorten with input URL
     * 2. Call GET /redirect?code={shortCode} and assert 302 Found
     */
    @Test
    @DisplayName("Should shorten URL and redirect with HTTP 302")
    void testShortenAndRedirectIntegrationFlow() throws Exception {
        String originalUrl = "https://example.com";
        ShortenRequest request = new ShortenRequest(originalUrl);

        MvcResult shortenResult = mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode").isString())
                .andExpect(jsonPath("$.originalUrl").value(originalUrl))
                .andReturn();

        String shortCode = objectMapper.readTree(shortenResult.getResponse().getContentAsString())
                .get("shortCode").asText();
        assertThat(shortCode).isNotBlank();

        mockMvc.perform(get("/redirect").param("code", shortCode))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, originalUrl));
    }

    /**
     * [Q2] When an empty URL is sent in the request, return an error message along with 400 Bad Request.
     */
    @Test
    @DisplayName("Q2: Should reject empty or blank URL with HTTP 400 Bad Request")
    void testEmptyUrlReturns400BadRequest() throws Exception {
        mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));

        mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    /**
     * [Q3] Click count increment & last_accessed_at update on redirect.
     */
    @Test
    @DisplayName("Q3: Should increment click counter and record last accessed timestamp on redirection")
    void testClickCountAndLastAccessedAtTracking() throws Exception {
        String originalUrl = "https://spring.io";
        MvcResult createResult = mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ShortenRequest(originalUrl))))
                .andExpect(status().isCreated())
                .andReturn();

        String shortCode = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("shortCode").asText();

        // Before any redirects, clickCount = 0
        UrlMapping initial = urlMappingRepository.findByShortCode(shortCode).orElseThrow();
        assertThat(initial.getClickCount()).isEqualTo(0L);
        assertThat(initial.getLastAccessedAt()).isNull();

        // 1st redirect
        mockMvc.perform(get("/redirect").param("code", shortCode))
                .andExpect(status().isFound());

        UrlMapping afterFirst = urlMappingRepository.findByShortCode(shortCode).orElseThrow();
        assertThat(afterFirst.getClickCount()).isEqualTo(1L);
        assertThat(afterFirst.getLastAccessedAt()).isNotNull();

        // 2nd redirect
        mockMvc.perform(get("/redirect").param("code", shortCode))
                .andExpect(status().isFound());

        UrlMapping afterSecond = urlMappingRepository.findByShortCode(shortCode).orElseThrow();
        assertThat(afterSecond.getClickCount()).isEqualTo(2L);
    }

    /**
     * [Q4] Allow multiple short codes for the same URL.
     */
    @Test
    @DisplayName("Q4: Should generate distinct short codes when shortening identical URLs multiple times")
    void testMultipleShortCodesAllowedForSameUrl() throws Exception {
        String targetUrl = "https://example.com/common-url-" + UUID.randomUUID();
        ShortenRequest request = new ShortenRequest(targetUrl);

        MvcResult firstResult = mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        String code1 = objectMapper.readTree(firstResult.getResponse().getContentAsString())
                .get("shortCode").asText();

        MvcResult secondResult = mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        String code2 = objectMapper.readTree(secondResult.getResponse().getContentAsString())
                .get("shortCode").asText();

        // Assert different short codes generated
        assertThat(code1).isNotEqualTo(code2);
        assertThat(urlMappingRepository.count()).isEqualTo(2);
    }

    /**
     * [Q5] API Key ownership, unauthorized deletion check, and soft delete.
     */
    @Test
    @DisplayName("Q5: Should link creator via API key, block deletion by non-owners, and soft-delete")
    void testApiKeyOwnershipAndSoftDelete() throws Exception {
        // Alice creates a short code
        MvcResult createResult = mockMvc.perform(post("/shorten")
                        .header("X-API-Key", DataInitializer.ALICE_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \"https://alice-portfolio.com\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        String code = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("shortCode").asText();

        // Bob tries to delete Alice's short code -> 403 Forbidden
        mockMvc.perform(delete("/shorten/" + code)
                        .header("X-API-Key", DataInitializer.BOB_API_KEY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));

        // Anonymous user tries to delete -> 401 Unauthorized
        mockMvc.perform(delete("/shorten/" + code))
                .andExpect(status().isUnauthorized());

        // Alice deletes her own short code -> 200 OK
        mockMvc.perform(delete("/shorten/" + code)
                        .header("X-API-Key", DataInitializer.ALICE_API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Short code '" + code + "' deleted successfully"));

        // Verification of Soft Delete: Record still in DB, but marked is_deleted = true
        UrlMapping inDb = urlMappingRepository.findByShortCode(code).orElseThrow();
        assertThat(inDb.getDeleted()).isTrue();
        assertThat(inDb.getDeletedAt()).isNotNull();

        // Subsequent redirect returns 404 Not Found
        mockMvc.perform(get("/redirect").param("code", code))
                .andExpect(status().isNotFound());
    }

    /**
     * [Q8] Expiry Date: Return 404 if the short code is expired.
     */
    @Test
    @DisplayName("Q8: Should return 404 Not Found when accessing an expired short code")
    void testExpiredShortCodeReturns404() throws Exception {
        LocalDateTime pastExpiry = LocalDateTime.now().minusDays(1);
        ShortenRequest request = new ShortenRequest("https://limited-offer.com", null, pastExpiry, null);

        MvcResult createResult = mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String code = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("shortCode").asText();

        // Redirect returns 404 because it is expired
        mockMvc.perform(get("/redirect").param("code", code))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Short code '" + code + "' has expired"));
    }

    /**
     * [Q9] Custom short codes and collision prevention.
     */
    @Test
    @DisplayName("Q9: Should allow custom short code and reject collisions with 409 Conflict")
    void testCustomShortCodeAndCollision() throws Exception {
        String custom = "awesome-article";
        ShortenRequest request1 = new ShortenRequest("https://my-blog.com/post-1", custom, null, null);

        // 1st creation succeeds
        mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode").value(custom));

        // Redirect works
        mockMvc.perform(get("/redirect").param("code", custom))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, "https://my-blog.com/post-1"));

        // 2nd creation with duplicate custom code fails with 409 Conflict
        ShortenRequest request2 = new ShortenRequest("https://another-blog.com", custom, null, null);
        mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    /**
     * [Q10 & Q12] Bulk creation endpoint:
     * - Enterprise user succeeds (201 Created)
     * - Hobby user is blocked (403 Forbidden)
     * - Unauthenticated user is blocked (401 Unauthorized)
     */
    @Test
    @DisplayName("Q10 & Q12: Bulk creation should only be permitted for Enterprise tier users")
    void testBulkCreationTierRestriction() throws Exception {
        ShortenBulkRequest bulkRequest = new ShortenBulkRequest(List.of(
                "https://example.com/page-1",
                "https://example.com/page-2",
                "https://example.com/page-3"
        ));

        // Unauthenticated -> 401
        mockMvc.perform(post("/shorten/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bulkRequest)))
                .andExpect(status().isUnauthorized());

        // Hobby tier user (Alice) -> 403 Forbidden
        mockMvc.perform(post("/shorten/bulk")
                        .header("X-API-Key", DataInitializer.ALICE_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bulkRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Bulk creation is only available for enterprise tier users. Please upgrade your plan."));

        // Enterprise tier user -> 201 Created
        mockMvc.perform(post("/shorten/bulk")
                        .header("X-API-Key", DataInitializer.ENTERPRISE_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bulkRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.successful").value(3))
                .andExpect(jsonPath("$.results").isArray());
    }

    /**
     * [Q13] Allow editing short code (URL, expiration date, password) by owner.
     */
    @Test
    @DisplayName("Q13: Owner should be able to edit short code target URL and expiration date")
    void testEditShortCodeFunctionality() throws Exception {
        // Bob creates a short code
        MvcResult createResult = mockMvc.perform(post("/shorten")
                        .header("X-API-Key", DataInitializer.BOB_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \"https://initial-site.com\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        String code = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("shortCode").asText();

        // Bob edits target URL to point to a new destination
        ShortenEditRequest editRequest = new ShortenEditRequest("https://updated-destination.com", null, null);
        mockMvc.perform(put("/shorten/" + code)
                        .header("X-API-Key", DataInitializer.BOB_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(editRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalUrl").value("https://updated-destination.com"));

        // Verify redirection now goes to updated URL
        mockMvc.perform(get("/redirect").param("code", code))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, "https://updated-destination.com"));

        // Bob deactivates short code by setting expiry date to the past
        ShortenEditRequest expireRequest = new ShortenEditRequest(null, LocalDateTime.now().minusHours(2), null);
        mockMvc.perform(put("/shorten/" + code)
                        .header("X-API-Key", DataInitializer.BOB_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(expireRequest)))
                .andExpect(status().isOk());

        // Now redirection returns 404
        mockMvc.perform(get("/redirect").param("code", code))
                .andExpect(status().isNotFound());
    }

    /**
     * [Q14] Password protection on short codes.
     */
    @Test
    @DisplayName("Q14: Should require correct password to redirect on protected short code")
    void testPasswordProtectedShortCode() throws Exception {
        ShortenRequest request = new ShortenRequest("https://vip-content.com", null, null, "superSecretPass");

        MvcResult createResult = mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String code = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("shortCode").asText();

        // 1. Calling /redirect without password -> 401 Unauthorized
        mockMvc.perform(get("/redirect").param("code", code))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));

        // 2. Calling with wrong password -> 401 Unauthorized
        mockMvc.perform(get("/redirect").param("code", code).param("password", "wrongPass"))
                .andExpect(status().isUnauthorized());

        // 3. Calling with correct password -> 302 Found
        mockMvc.perform(get("/redirect").param("code", code).param("password", "superSecretPass"))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, "https://vip-content.com"));
    }

    /**
     * [Q15] List all URLs of a user.
     */
    @Test
    @DisplayName("Q15: Should list all URLs created by an authenticated user")
    void testGetUserUrlsList() throws Exception {
        // Alice creates 2 URLs
        mockMvc.perform(post("/shorten")
                        .header("X-API-Key", DataInitializer.ALICE_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \"https://alice-site-1.com\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/shorten")
                        .header("X-API-Key", DataInitializer.ALICE_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \"https://alice-site-2.com\"}"))
                .andExpect(status().isCreated());

        // Alice fetches her URLs
        mockMvc.perform(get("/users/urls")
                        .header("X-API-Key", DataInitializer.ALICE_API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        // Bob has 0 URLs
        mockMvc.perform(get("/users/urls")
                        .header("X-API-Key", DataInitializer.BOB_API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    /**
     * [Q16 BONUS] Health check endpoint.
     */
    @Test
    @DisplayName("Q16 BONUS: /health should verify server and database connectivity")
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.database").value("UP"));
    }
}
