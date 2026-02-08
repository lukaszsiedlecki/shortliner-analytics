package com.shortliner.analytics.controller;

import com.shortliner.analytics.dto.DailyStatsResponse;
import com.shortliner.analytics.dto.SummaryResponse;
import com.shortliner.analytics.dto.UserAnalyticsResponse;
import com.shortliner.analytics.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@Tag(name = "Analytics", description = "URL click analytics endpoints")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/{shortCode}/daily")
    @Operation(summary = "Get daily click statistics for a short URL")
    public ResponseEntity<Page<DailyStatsResponse>> getDailyStats(
            @Parameter(description = "The short URL code") @PathVariable String shortCode,
            @PageableDefault(size = 30) Pageable pageable) {
        return ResponseEntity.ok(analyticsService.getDailyStats(shortCode, pageable));
    }

    @GetMapping("/{shortCode}/summary")
    @Operation(summary = "Get summary statistics for a short URL")
    public ResponseEntity<SummaryResponse> getSummary(
            @Parameter(description = "The short URL code") @PathVariable String shortCode) {
        return ResponseEntity.ok(analyticsService.getSummary(shortCode));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get analytics for all short URLs owned by a user")
    public ResponseEntity<Page<UserAnalyticsResponse>> getUserAnalytics(
            @Parameter(description = "The user ID") @PathVariable String userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(analyticsService.getUserAnalytics(userId, pageable));
    }
}
