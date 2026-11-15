package com.vyson.urlshortener.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class ShortenBulkRequest {

    @NotEmpty(message = "original_urls must not be empty")
    @JsonProperty("original_urls")
    @JsonAlias({"originalUrls", "urls"})
    private List<String> originalUrls;

    public ShortenBulkRequest() {
    }

    public ShortenBulkRequest(List<String> originalUrls) {
        this.originalUrls = originalUrls;
    }

    public List<String> getOriginalUrls() {
        return originalUrls;
    }

    public void setOriginalUrls(List<String> originalUrls) {
        this.originalUrls = originalUrls;
    }
}
