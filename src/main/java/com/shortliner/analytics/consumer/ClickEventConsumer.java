package com.shortliner.analytics.consumer;

import com.shortliner.analytics.dto.ClickEventDto;
import com.shortliner.analytics.service.AnalyticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class ClickEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ClickEventConsumer.class);

    private final AnalyticsService analyticsService;

    public ClickEventConsumer(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @KafkaListener(topics = "shortliner.clicks", containerFactory = "kafkaListenerContainerFactory")
    public void consume(@Payload ClickEventDto event, Acknowledgment ack) {
        log.info("Received click event: shortCode={}, ip={}", event.shortCode(), event.ip());
        try {
            analyticsService.processClickEvent(event);
            ack.acknowledge();
            log.debug("Successfully processed and acknowledged click event for shortCode={}", event.shortCode());
        } catch (Exception e) {
            log.error("Error processing click event for shortCode={}: {}",
                    event.shortCode(), e.getMessage(), e);
            throw e;
        }
    }
}
