package com.dreamstartlabs.dreamlink.identity.kafka.events;

import com.fasterxml.jackson.annotation.JsonAnyGetter;

import java.util.Map;
import java.util.UUID;

/**
 * Generic wrapper for Kafka events with metadata and a dynamic payload.
 *
 * @param meta metadata information such as correlation and actor details
 * @param payload dynamic event payload mapped by event name
 * @param <T> type of event payload extending UserEventAction
 */
public record KafkaEvent<T extends UserEventAction>(
        Meta meta,
        @JsonAnyGetter Map<String, T> payload
) {
    public record Meta(
            String correlationId,
            String actorId
            //TODO: Add later once protobufs are finalized
//            AuditAction action,
//            ResourceType resourceType,
//            Source source
    ) {
    }

    public static <T extends UserEventAction> KafkaEvent<T> of(String eventName, T data) {
        return new KafkaEvent<>(
                new Meta(
                        UUID.randomUUID().toString(),
                        "system"
                ),
                Map.of(eventName, data)
        );
    }
}
