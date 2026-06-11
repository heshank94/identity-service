package com.dreamstartlabs.dreamlink.identity.kafka.events;

import lombok.Builder;

import java.time.Instant;

/**
 * Event published when a user is disabled or their status is updated.
 *
 * @param id unique identifier of the user
 * @param oneLoginId OneLogin user identifier
 * @param tenant tenant associated with the user
 * @param region region associated with the user
 * @param enabled whether the user is enabled or disabled
 * @param occurredAt timestamp when the event occurred
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
) implements UserEventAction {

}
