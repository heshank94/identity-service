package com.dreamstartlabs.dreamlink.identity.kafka;

import com.dreamstartlabs.dreamlink.identity.kafka.events.*;
import com.dreamstartlabs.dreamlink.identity.models.dto.OneLoginUser;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.dreamstartlabs.dreamlink.identity.utils.CustomAttributeUtil.extract;

/**
 * @author Heshan Karunaratne
 */
@Component
public class SyncEventBuilder {

    public KafkaEvent<UserEventAction> buildCreatedEvent(
            OneLoginUser user,
            List<String> roles,
            String keycloakUserId
    ) {

        UserCreatedEvent data = UserCreatedEvent.builder()
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
                .build();

        return new KafkaEvent<>(meta(), data);
    }

    public KafkaEvent<UserEventAction> buildUpdatedEvent(
            OneLoginUser user,
            List<String> roles,
            String keycloakUserId
    ) {

        UserUpdatedEvent data = UserUpdatedEvent.builder()
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
                .build();

        return new KafkaEvent<>(meta(), data);
    }

    public KafkaEvent<UserEventAction> buildDisabledEvent(
            OneLoginUser user,
            String keycloakUserId
    ) {

        UserDisabledEvent data = UserDisabledEvent.builder()
                .id(keycloakUserId)
                .oneLoginId(user.getId())
                .enabled(false)
                .occurredAt(Instant.now())
                .build();

        return new KafkaEvent<>(meta(), data);
    }

    private KafkaEvent.Meta meta() {
        return new KafkaEvent.Meta(
                UUID.randomUUID().toString(),
                "system"

        );
    }
}