package com.dreamstartlabs.dreamlink.identity.kafka;

import com.dreamstartlabs.dreamlink.identity.kafka.events.KafkaEvent;
import com.dreamstartlabs.dreamlink.identity.kafka.events.UserCreatedEvent;
import com.dreamstartlabs.dreamlink.identity.kafka.events.UserDisabledEvent;
import com.dreamstartlabs.dreamlink.identity.kafka.events.UserUpdatedEvent;
import com.dreamstartlabs.dreamlink.identity.models.dto.OneLoginUser;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.dreamstartlabs.dreamlink.identity.kafka.events.EventNames.*;
import static com.dreamstartlabs.dreamlink.identity.utils.CustomAttributeUtil.extract;

/**
 * @author Heshan Karunaratne
 */
@Component
public class SyncEventBuilder {

    public KafkaEvent<UserCreatedEvent> buildCreatedEvent(
            OneLoginUser user,
            List<String> roles,
            String keycloakUserId
    ) {

        return KafkaEvent.of(USER_CREATED, UserCreatedEvent.builder()
                .id(keycloakUserId)
                .oneLoginId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .tenant(extract(user, "tenant"))
                .region(extract(user, "region"))
                .roles(roles)
                .enabled(true)
                .occurredAt(Instant.now())
                .build());
    }

    public KafkaEvent<UserUpdatedEvent> buildUpdatedEvent(
            OneLoginUser user,
            List<String> roles,
            String keycloakUserId
    ) {

        return KafkaEvent.of(USER_UPDATED, UserUpdatedEvent.builder()
                .id(keycloakUserId)
                .oneLoginId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .tenant(extract(user, "tenant"))
                .region(extract(user, "region"))
                .roles(roles)
                .enabled(true)
                .occurredAt(Instant.now())
                .build());
    }

    public KafkaEvent<UserDisabledEvent> buildDisabledEvent(
            OneLoginUser user,
            String keycloakUserId
    ) {

        return KafkaEvent.of(USER_DISABLED, UserDisabledEvent.builder()
                .id(keycloakUserId)
                .oneLoginId(user.getId())
                .tenant(extract(user, "tenant"))
                .region(extract(user, "region"))
                .enabled(false)
                .occurredAt(Instant.now())
                .build());
    }

    private KafkaEvent.Meta meta() {
        return new KafkaEvent.Meta(
                UUID.randomUUID().toString(),
                "system"

        );
    }
}