package com.vyson.urlshortener.exception;

public class CustomCodeAlreadyExistsException extends RuntimeException {
    public CustomCodeAlreadyExistsException(String message) {
        super(message);
    }
}
