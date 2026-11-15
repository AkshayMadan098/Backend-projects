package com.vyson.urlshortener.dto;

import java.util.List;

public class ShortenBulkResponse {

    private int total;
    private int successful;
    private List<ShortenResponse> results;

    public ShortenBulkResponse() {
    }

    public ShortenBulkResponse(List<ShortenResponse> results) {
        this.results = results;
        this.total = results != null ? results.size() : 0;
        this.successful = this.total;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getSuccessful() {
        return successful;
    }

    public void setSuccessful(int successful) {
        this.successful = successful;
    }

    public List<ShortenResponse> getResults() {
        return results;
    }

    public void setResults(List<ShortenResponse> results) {
        this.results = results;
    }
}
