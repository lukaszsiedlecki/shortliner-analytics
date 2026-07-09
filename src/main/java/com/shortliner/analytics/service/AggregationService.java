package com.shortliner.analytics.service;

import com.shortliner.analytics.entity.AggregatedStats;
import com.shortliner.analytics.repository.AggregatedStatsRepository;
import com.shortliner.analytics.repository.ClickEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
public class AggregationService {

    private static final Logger log = LoggerFactory.getLogger(AggregationService.class);

    private final ClickEventRepository clickEventRepository;
    private final AggregatedStatsRepository aggregatedStatsRepository;
    private final Timer aggregationDurationTimer;
    private final Counter aggregationEntriesProcessedCounter;

    public AggregationService(ClickEventRepository clickEventRepository,
                              AggregatedStatsRepository aggregatedStatsRepository,
                              MeterRegistry meterRegistry) {
        this.clickEventRepository = clickEventRepository;
        this.aggregatedStatsRepository = aggregatedStatsRepository;
        this.aggregationDurationTimer = Timer.builder("aggregation.duration")
                .description("Time taken to run the daily stats aggregation job")
                .register(meterRegistry);
        this.aggregationEntriesProcessedCounter = Counter.builder("aggregation.entries.processed")
                .description("Number of shortCode/date rows upserted into aggregated_stats per run")
                .register(meterRegistry);
    }

    @Scheduled(fixedRateString = "${analytics.aggregation.interval-ms:300000}")
    @Transactional
    public void aggregateDailyStats() {
        Timer.Sample sample = Timer.start();
        try {
            log.info("Starting daily stats aggregation");

            List<Object[]> dailyData = clickEventRepository.findDailyAggregates();

            for (Object[] row : dailyData) {
                String shortCode = (String) row[0];
                LocalDate date = ((java.sql.Date) row[1]).toLocalDate();
                long clickCount = ((Number) row[2]).longValue();
                long uniqueIps = ((Number) row[3]).longValue();

                AggregatedStats stats = aggregatedStatsRepository
                        .findByShortCodeAndDate(shortCode, date)
                        .orElseGet(() -> {
                            AggregatedStats newStats = new AggregatedStats();
                            newStats.setShortCode(shortCode);
                            newStats.setDate(date);
                            return newStats;
                        });

                stats.setClickCount(clickCount);
                stats.setUniqueVisitors(uniqueIps);
                stats.setLastUpdated(Instant.now());
                aggregatedStatsRepository.save(stats);
            }

            aggregationEntriesProcessedCounter.increment(dailyData.size());
            log.info("Daily stats aggregation completed, processed {} entries", dailyData.size());
        } finally {
            sample.stop(aggregationDurationTimer);
        }
    }
}
