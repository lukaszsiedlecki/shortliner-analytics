package com.shortliner.analytics.dto;

import java.time.LocalDate;

public record DailyStatsResponse(
        String shortCode,
        LocalDate date,
        long clickCount,
        long uniqueVisitors
) {
}
