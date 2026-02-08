package com.shortliner.analytics.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record ClickEventDto(
        @NotBlank String shortCode,
        String userId,
        @NotNull Instant timestamp,
        @NotBlank String ip,
        String userAgent,
        String referrer
) {
}
