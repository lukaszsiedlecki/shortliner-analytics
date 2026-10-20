package com.shortliner.analytics.service;

import com.shortliner.analytics.entity.AggregatedStats;
import com.shortliner.analytics.entity.ClickEvent;
import com.shortliner.analytics.repository.AggregatedStatsRepository;
import com.shortliner.analytics.repository.ClickEventRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AggregationServiceTest {

    @Autowired
    private AggregationService aggregationService;

    @Autowired
    private ClickEventRepository clickEventRepository;

    @Autowired
    private AggregatedStatsRepository aggregatedStatsRepository;

    @Autowired
    private MeterRegistry meterRegistry;

    @AfterEach
    void cleanUp() {
        aggregatedStatsRepository.deleteAll();
        clickEventRepository.deleteAll();
    }

    @Test
    void successfulRunUpdatesLastSuccessGaugeAndTimer() {
        long before = Instant.now().getEpochSecond();

        aggregationService.aggregateDailyStats();

        double lastSuccess = meterRegistry.get("shortliner.analytics.aggregation.last.success").gauge().value();
        assertThat(lastSuccess).isGreaterThanOrEqualTo(before);
        assertThat(meterRegistry.get("aggregation.duration").timer().count()).isPositive();
    }

    @Test
    void aggregatesClicksPerShortCodeAndDay() {
        saveClick("agg1", "10.0.0.1");
        saveClick("agg1", "10.0.0.1");
        saveClick("agg1", "10.0.0.2");

        aggregationService.aggregateDailyStats();

        assertThat(aggregatedStatsRepository.findAll())
                .filteredOn(stats -> stats.getShortCode().equals("agg1"))
                .singleElement()
                .satisfies(stats -> {
                    assertThat(stats.getClickCount()).isEqualTo(3);
                    assertThat(stats.getUniqueVisitors()).isEqualTo(2);
                    assertThat(stats.getDate()).isNotNull();
                });
    }

    private void saveClick(String shortCode, String ip) {
        ClickEvent event = new ClickEvent();
        event.setShortCode(shortCode);
        event.setTimestamp(Instant.parse("2026-10-20T12:00:00Z"));
        event.setIp(ip);
        event.setEventHash(UUID.randomUUID().toString().replace("-", ""));
        clickEventRepository.save(event);
    }
}
