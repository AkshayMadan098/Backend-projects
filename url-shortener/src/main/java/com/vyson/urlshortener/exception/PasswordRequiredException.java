package com.vyson.urlshortener.exception;

public class PasswordRequiredException extends RuntimeException {
    public PasswordRequiredException(String message) {
        super(message);
    }
}
