package com.vyson.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;

public class ShortenRequest {

    @NotBlank(message = "url is required and cannot be blank")
    private String url;

    public ShortenRequest() {
    }

    public ShortenRequest(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
