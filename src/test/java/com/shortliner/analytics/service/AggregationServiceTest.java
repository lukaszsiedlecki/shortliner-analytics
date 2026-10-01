package com.shortliner.analytics.service;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AggregationServiceTest {

    @Autowired
    private AggregationService aggregationService;

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    void successfulRunUpdatesLastSuccessGaugeAndTimer() {
        long before = Instant.now().getEpochSecond();

        aggregationService.aggregateDailyStats();

        double lastSuccess = meterRegistry.get("shortliner.analytics.aggregation.last.success").gauge().value();
        assertThat(lastSuccess).isGreaterThanOrEqualTo(before);
        assertThat(meterRegistry.get("aggregation.duration").timer().count()).isPositive();
    }
}
