package com.shortliner.analytics.service;

import com.shortliner.analytics.dto.ClickEventDto;
import com.shortliner.analytics.dto.DailyStatsResponse;
import com.shortliner.analytics.dto.SummaryResponse;
import com.shortliner.analytics.dto.UserAnalyticsResponse;
import com.shortliner.analytics.entity.ClickEvent;
import com.shortliner.analytics.exception.ShortCodeNotFoundException;
import com.shortliner.analytics.mapper.AnalyticsMapper;
import com.shortliner.analytics.repository.AggregatedStatsRepository;
import com.shortliner.analytics.repository.ClickEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

@Service
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);

    private final ClickEventRepository clickEventRepository;
    private final AggregatedStatsRepository aggregatedStatsRepository;
    private final AnalyticsMapper mapper;
    private final Counter clickEventsPersistedCounter;
    private final Counter clickEventsDuplicateCounter;

    public AnalyticsService(ClickEventRepository clickEventRepository,
                            AggregatedStatsRepository aggregatedStatsRepository,
                            AnalyticsMapper mapper,
                            MeterRegistry meterRegistry) {
        this.clickEventRepository = clickEventRepository;
        this.aggregatedStatsRepository = aggregatedStatsRepository;
        this.mapper = mapper;
        this.clickEventsPersistedCounter = Counter.builder("click.events.persisted")
                .description("Click events deduplicated and persisted to click_events")
                .register(meterRegistry);
        this.clickEventsDuplicateCounter = Counter.builder("click.events.duplicate")
                .description("Click events skipped because their event hash already exists")
                .register(meterRegistry);
    }

    @Transactional
    public void processClickEvent(ClickEventDto dto) {
        String eventHash = computeEventHash(dto);

        if (clickEventRepository.existsByEventHash(eventHash)) {
            log.debug("Duplicate click event detected, skipping: {}", eventHash);
            clickEventsDuplicateCounter.increment();
            return;
        }

        ClickEvent event = mapper.toEntity(dto, eventHash);
        clickEventRepository.save(event);
        clickEventsPersistedCounter.increment();
        log.debug("Persisted click event for shortCode={}", dto.shortCode());
    }

    @Transactional(readOnly = true)
    public Page<DailyStatsResponse> getDailyStats(String shortCode, Pageable pageable) {
        if (!clickEventRepository.existsByShortCode(shortCode)) {
            throw new ShortCodeNotFoundException(shortCode);
        }
        return aggregatedStatsRepository
                .findByShortCodeOrderByDateDesc(shortCode, pageable)
                .map(mapper::toDailyResponse);
    }

    @Transactional(readOnly = true)
    public SummaryResponse getSummary(String shortCode) {
        if (!clickEventRepository.existsByShortCode(shortCode)) {
            throw new ShortCodeNotFoundException(shortCode);
        }

        long totalClicks = clickEventRepository.countByShortCode(shortCode);
        long uniqueVisitors = clickEventRepository.countDistinctIpByShortCode(shortCode);
        Instant firstClick = clickEventRepository.findFirstClickByShortCode(shortCode);
        Instant lastClick = clickEventRepository.findLastClickByShortCode(shortCode);

        return new SummaryResponse(shortCode, totalClicks, uniqueVisitors, firstClick, lastClick);
    }

    @Transactional(readOnly = true)
    public Page<UserAnalyticsResponse> getUserAnalytics(String userId, Pageable pageable) {
        return clickEventRepository.findUserAnalytics(userId, pageable);
    }

    private String computeEventHash(ClickEventDto dto) {
        String raw = dto.shortCode() + "|" + dto.timestamp() + "|" + dto.ip() + "|" + dto.userAgent();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
