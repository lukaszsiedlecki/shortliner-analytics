package com.shortliner.analytics.mapper;

import com.shortliner.analytics.dto.ClickEventDto;
import com.shortliner.analytics.dto.DailyStatsResponse;
import com.shortliner.analytics.entity.AggregatedStats;
import com.shortliner.analytics.entity.ClickEvent;
import org.springframework.stereotype.Component;

@Component
public class AnalyticsMapper {

    public ClickEvent toEntity(ClickEventDto dto, String eventHash) {
        ClickEvent entity = new ClickEvent();
        entity.setShortCode(dto.shortCode());
        entity.setUserId(dto.userId());
        entity.setTimestamp(dto.timestamp());
        entity.setIp(dto.ip());
        entity.setUserAgent(dto.userAgent());
        entity.setReferrer(dto.referrer());
        entity.setEventHash(eventHash);
        return entity;
    }

    public DailyStatsResponse toDailyResponse(AggregatedStats stats) {
        return new DailyStatsResponse(
                stats.getShortCode(),
                stats.getDate(),
                stats.getClickCount(),
                stats.getUniqueVisitors()
        );
    }
}
