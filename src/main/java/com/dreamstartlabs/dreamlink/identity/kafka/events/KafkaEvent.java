package com.dreamstartlabs.dreamlink.identity.kafka.events;


/**
 * @param meta the metadata container with event tracing and audit information
 * @param data the generic business payload of the event
 * @param <T>  the type of the event payload data
 * @author Heshan Karunaratne
 */
public record KafkaEvent<T>(
        Meta meta,
        T data
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
}
