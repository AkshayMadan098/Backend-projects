package com.vyson.urlshortener.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShortenBulkV2Response {

    private String apiVersion = "v2.0";
    private int count;
    private List<ShortenV2Response> results;

    public ShortenBulkV2Response() {
    }

    public ShortenBulkV2Response(List<ShortenV2Response> results) {
        this.apiVersion = "v2.0";
        this.results = results;
        this.count = results != null ? results.size() : 0;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public List<ShortenV2Response> getResults() {
        return results;
    }

    public void setResults(List<ShortenV2Response> results) {
        this.results = results;
        this.count = results != null ? results.size() : 0;
    }
}
