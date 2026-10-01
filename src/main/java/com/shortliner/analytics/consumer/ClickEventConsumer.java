package com.shortliner.analytics.consumer;

import com.shortliner.analytics.dto.ClickEventDto;
import com.shortliner.analytics.service.AnalyticsService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
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
    private final Counter clickEventsReceivedCounter;
    private final Counter clickEventsProcessingErrorCounter;

    public ClickEventConsumer(AnalyticsService analyticsService, MeterRegistry meterRegistry) {
        this.analyticsService = analyticsService;
        this.clickEventsReceivedCounter = Counter.builder("click.events.received")
                .description("Click events received from the shortliner.clicks Kafka topic")
                .register(meterRegistry);
        this.clickEventsProcessingErrorCounter = Counter.builder("click.events.processing.errors")
                .description("Click events that failed processing and were not acknowledged")
                .register(meterRegistry);
    }

    @KafkaListener(topics = "shortliner.clicks", containerFactory = "kafkaListenerContainerFactory")
    public void consume(@Payload ClickEventDto event, Acknowledgment ack) {
        log.atInfo().addKeyValue("shortCode", event.shortCode()).log("Received click event");
        clickEventsReceivedCounter.increment();
        try {
            analyticsService.processClickEvent(event);
            ack.acknowledge();
            log.atDebug().addKeyValue("shortCode", event.shortCode())
                    .log("Successfully processed and acknowledged click event");
        } catch (Exception e) {
            clickEventsProcessingErrorCounter.increment();
            log.atError()
                    .addKeyValue("shortCode", event.shortCode())
                    .addKeyValue("eventTimestamp", event.timestamp())
                    .setCause(e)
                    .log("Error processing click event: {}", e.getMessage());
            throw e;
        }
    }
}
