package com.dreamstartlabs.dreamlink.identity.kafka.events;

import lombok.Builder;

import java.time.Instant;
import java.util.List;

/**
 * @author Heshan Karunaratne
 */
@Builder
public record UserCreatedEvent(
        String id,
        Long oneLoginId,
        String username,
        String email,
        String firstName,
        String lastName,
        String tenant,
        String region,
        List<String> roles,
        boolean enabled,
        Instant occurredAt
) implements UserEventAction {}