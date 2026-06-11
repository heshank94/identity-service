package com.dreamstartlabs.dreamlink.identity.kafka.producer;

import com.dreamstartlabs.dreamlink.identity.kafka.events.KafkaEvent;
import com.dreamstartlabs.dreamlink.identity.kafka.events.UserEventAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * @author Heshan Karunaratne
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SyncEventProducer {

    private final KafkaTemplate<String, KafkaEvent<?>> kafkaTemplate;

    public <T extends UserEventAction> void publish(String topic, KafkaEvent<T> event) {
        String userId = event.payload().values()
                .stream()
                .findFirst()
                .map(UserEventAction::id)
                .orElse("unknown");

        log.info("Publishing event to topic={} | userID={}", topic, userId);

        try {
            kafkaTemplate.send(topic, userId, event)
                    .whenComplete((result, ex) -> {
                        if (ex != null)
                            log.error("KAFKA_FAIL | topic={} | userID={} | reason={}", topic, userId, ex.getMessage());
                        else
                            log.debug("KAFKA_SUCCESS | topic={} | userID={} | offset={}",
                                    topic, userId, result.getRecordMetadata().offset());
                    });
        } catch (Exception e) {
            log.error("Failed to publish Kafka event for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Kafka publish failed", e);
        }
    }
}
