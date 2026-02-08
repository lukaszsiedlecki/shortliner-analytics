package com.shortliner.analytics.exception;

public class ShortCodeNotFoundException extends RuntimeException {

    public ShortCodeNotFoundException(String shortCode) {
        super("No analytics data found for short code: " + shortCode);
    }
}
