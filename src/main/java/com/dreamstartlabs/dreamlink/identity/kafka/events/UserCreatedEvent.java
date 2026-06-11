package com.dreamstartlabs.dreamlink.identity.kafka.events;

import lombok.Builder;

import java.time.Instant;
import java.util.List;

/**
 * Event published when a new user is created in the system.
 *
 * @param id unique identifier of the user event
 * @param oneLoginId OneLogin user identifier
 * @param username username of the user
 * @param email email address of the user
 * @param firstName first name of the user
 * @param lastName last name of the user
 * @param tenant tenant associated with the user
 * @param region region associated with the user
 * @param roles roles assigned to the user
 * @param enabled whether the user is enabled
 * @param occurredAt timestamp when the event occurred
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
) implements UserEventAction {

}
