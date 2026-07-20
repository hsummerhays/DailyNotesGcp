package com.hsummerhays.cloudnotes.user.application;

// Deliberately does not extend IllegalArgumentException so it isn't caught by the generic 404 handler.
public class InvalidRefreshTokenException extends RuntimeException {
    public InvalidRefreshTokenException(String message) {
        super(message);
    }
}
