package com.dreamstartlabs.dreamlink.identity.kafka.events;

import lombok.Builder;

import java.time.Instant;

/**
 * @author Heshan Karunaratne
 */
@Builder
public record UserDisabledEvent(
        String id,
        Long oneLoginId,
        String tenant,
        String region,
        boolean enabled,
        Instant occurredAt
) implements UserEventAction {}