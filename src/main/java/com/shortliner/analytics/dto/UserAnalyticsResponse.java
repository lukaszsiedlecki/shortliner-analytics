package com.shortliner.analytics.dto;

import java.time.Instant;

public record UserAnalyticsResponse(
        String shortCode,
        long clickCount,
        Instant lastClick
) {
}
