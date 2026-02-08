package com.shortliner.analytics.service;

import com.shortliner.analytics.entity.AggregatedStats;
import com.shortliner.analytics.repository.AggregatedStatsRepository;
import com.shortliner.analytics.repository.ClickEventRepository;
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

    public AggregationService(ClickEventRepository clickEventRepository,
                              AggregatedStatsRepository aggregatedStatsRepository) {
        this.clickEventRepository = clickEventRepository;
        this.aggregatedStatsRepository = aggregatedStatsRepository;
    }

    @Scheduled(fixedRateString = "${analytics.aggregation.interval-ms:300000}")
    @Transactional
    public void aggregateDailyStats() {
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

        log.info("Daily stats aggregation completed, processed {} entries", dailyData.size());
    }
}
