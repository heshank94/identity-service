package com.dreamstartlabs.dreamlink.identity.kafka.events;

import lombok.Builder;

import java.time.Instant;
import java.util.List;

/**
 * Event published when a user is updated.
 *
 * @param id unique user identifier
 * @param oneLoginId OneLogin user identifier
 * @param username username of the user
 * @param email email address of the user
 * @param firstName user's first name
 * @param lastName user's last name
 * @param tenant tenant associated with the user
 * @param region region associated with the user
 * @param roles roles assigned to the user
 * @param enabled indicates whether the user is enabled
 * @param occurredAt timestamp when the update event occurred
 * @author Heshan Karunaratne
 */
@Builder
public record UserUpdatedEvent(
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
) implements UserEventAction {

}
