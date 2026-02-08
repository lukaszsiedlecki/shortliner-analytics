package com.shortliner.analytics.dto;

import java.time.Instant;

public record SummaryResponse(
        String shortCode,
        long totalClicks,
        long uniqueVisitors,
        Instant firstClick,
        Instant lastClick
) {
}
