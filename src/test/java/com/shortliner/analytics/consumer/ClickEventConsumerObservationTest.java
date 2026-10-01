package com.shortliner.analytics.consumer;

import com.shortliner.analytics.dto.ClickEventDto;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

// Re-enable Kafka auto-config (excluded in the test profile) so @KafkaListener containers start
@SpringBootTest(properties = "spring.autoconfigure.exclude=")
@ActiveProfiles("test")
@EmbeddedKafka(topics = "shortliner.clicks", bootstrapServersProperty = "spring.kafka.bootstrap-servers")
class ClickEventConsumerObservationTest {

    @Autowired
    private EmbeddedKafkaBroker broker;

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    void consumedRecordIsRecordedByListenerObservation() {
        Map<String, Object> props = KafkaTestUtils.producerProps(broker);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        ClickEventDto event = new ClickEventDto("abc123", "user-1", Instant.now(), "10.0.0.1", "junit", null);

        try (KafkaProducer<String, ClickEventDto> producer = new KafkaProducer<>(props)) {
            producer.send(new ProducerRecord<>("shortliner.clicks", event.shortCode(), event));
        }

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(meterRegistry.find("spring.kafka.listener").timers())
                        .anySatisfy(timer -> assertThat(timer.count()).isPositive()));
        assertThat(meterRegistry.find("kafka.consumer.fetch.manager.records.consumed.total").meters()).isNotEmpty();
    }
}
