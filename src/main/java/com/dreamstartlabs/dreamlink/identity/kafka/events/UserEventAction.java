package com.dreamstartlabs.dreamlink.identity.kafka.events;

/**
 * @author Heshan Karunaratne
 */
public sealed interface UserEventAction
        permits UserCreatedEvent, UserDisabledEvent, UserUpdatedEvent {
    String id();
}