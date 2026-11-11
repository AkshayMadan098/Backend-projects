package com.vyson.urlshortener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vyson.urlshortener.dto.ShortenRequest;
import com.vyson.urlshortener.repository.UrlMappingRepository;
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
    private UrlMappingRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    /**
     * [Q2] Integration Test:
     * 1. Call /shorten API with input URL as "https://example.com", store short code received in a variable.
     * 2. Call /redirect API with the short code and see if the redirection happens correctly.
     */
    @Test
    @DisplayName("Q2: Should shorten URL and redirect to original URL with HTTP 302")
    void testShortenAndRedirectIntegrationFlow() throws Exception {
        String originalUrl = "https://example.com";
        ShortenRequest request = new ShortenRequest(originalUrl);

        // Step 1: Call POST /shorten
        MvcResult shortenResult = mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode").isString())
                .andExpect(jsonPath("$.originalUrl").value(originalUrl))
                .andReturn();

        // Extract short code from response
        String responseJson = shortenResult.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(responseJson);
        String shortCode = jsonNode.get("shortCode").asText();
        assertThat(shortCode).isNotBlank();

        // Step 2: Call GET /redirect?code={shortCode} and verify redirection
        mockMvc.perform(get("/redirect").param("code", shortCode))
                .andExpect(status().isFound()) // HTTP 302 Found
                .andExpect(header().string(HttpHeaders.LOCATION, originalUrl));
    }

    /**
     * [Q7] Duplicate URLs Test:
     * What happens if the same URL is sent again? Handle this case of duplicate URLs.
     * Write a test case that generates a random URL and sends it twice to the API.
     * The same short code should be returned.
     */
    @Test
    @DisplayName("Q7: Should return the same short code when the same URL is submitted twice")
    void testDuplicateUrlHandling() throws Exception {
        // Generate a unique random URL
        String randomUrl = "https://example.com/page/" + UUID.randomUUID();
        ShortenRequest request = new ShortenRequest(randomUrl);

        // First call
        MvcResult firstResult = mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String firstShortCode = objectMapper.readTree(firstResult.getResponse().getContentAsString())
                .get("shortCode").asText();

        // Second call with the exact same URL
        MvcResult secondResult = mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String secondShortCode = objectMapper.readTree(secondResult.getResponse().getContentAsString())
                .get("shortCode").asText();

        // Assert both calls return the identical short code
        assertThat(secondShortCode)
                .as("Submitting the same URL multiple times must return the same short code")
                .isEqualTo(firstShortCode);

        // Verify only one database entry exists
        assertThat(repository.count()).isEqualTo(1);
    }

    /**
     * [Q8] Non-existent short code Test:
     * What happens when you try to fetch a short code that doesn’t exist?
     * The best HTTP status code is 404 Not Found.
     */
    @Test
    @DisplayName("Q8: Should return HTTP 404 Not Found when accessing a non-existent short code")
    void testNonExistentShortCodeReturns404() throws Exception {
        String nonExistentCode = "nonExistent999";

        mockMvc.perform(get("/redirect").param("code", nonExistentCode))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Short code 'nonExistent999' not found"));
    }

    /**
     * [Q9] DELETE short code Test:
     * Verify deletion of an existing short code, followed by verifying that
     * /redirect returns 404 Not Found, and deleting an already deleted code returns 404.
     */
    @Test
    @DisplayName("Q9: Should delete short code and verify subsequent redirection returns 404")
    void testDeleteShortCodeFunctionality() throws Exception {
        // Step 1: Create a short URL
        String originalUrl = "https://spring.io/projects/spring-boot";
        MvcResult createResult = mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ShortenRequest(originalUrl))))
                .andExpect(status().isCreated())
                .andReturn();

        String shortCode = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("shortCode").asText();

        // Step 2: Confirm it can be accessed
        mockMvc.perform(get("/redirect").param("code", shortCode))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, originalUrl));

        // Step 3: Delete using DELETE /shorten/{code}
        mockMvc.perform(delete("/shorten/" + shortCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Short code '" + shortCode + "' deleted successfully"));

        // Step 4: Verify subsequent redirect returns HTTP 404
        mockMvc.perform(get("/redirect").param("code", shortCode))
                .andExpect(status().isNotFound());

        // Step 5: Deleting again should return 404
        mockMvc.perform(delete("/shorten/" + shortCode))
                .andExpect(status().isNotFound());
    }

    /**
     * [Q10 BONUS] Edge Cases & Validation Tests:
     * - Malformed URL
     * - Blank URL
     * - Non-HTTP/HTTPS protocols (e.g. ftp://, javascript:)
     * - URLs with query strings and fragment anchors
     */
    @Test
    @DisplayName("Q10 BONUS: Should reject invalid or blank URLs with HTTP 400 Bad Request")
    void testInvalidUrlsReturn400BadRequest() throws Exception {
        // Blank URL
        mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \"\"}"))
                .andExpect(status().isBadRequest());

        // Malformed non-URL string
        mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \"not_a_valid_url\"}"))
                .andExpect(status().isBadRequest());

        // Disallowed protocol (FTP)
        mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \"ftp://ftp.example.com/file.zip\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Q10 BONUS: Complex URL with query params and anchors should be preserved exactly")
    void testComplexUrlPreservedExactly() throws Exception {
        String complexUrl = "https://example.com/search?q=spring+boot&filter=active#results-section";
        MvcResult result = mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ShortenRequest(complexUrl))))
                .andExpect(status().isCreated())
                .andReturn();

        String code = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("shortCode").asText();

        mockMvc.perform(get("/redirect").param("code", code))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, complexUrl));
    }
}
