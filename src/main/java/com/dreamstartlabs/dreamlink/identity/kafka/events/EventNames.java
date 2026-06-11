package com.dreamstartlabs.dreamlink.identity.kafka.events;

/**
 * @author Heshan Karunaratne
 */
public final class EventNames {
    private EventNames() {

    }

    public static final String USER_CREATED  = "userCreated";
    public static final String USER_UPDATED  = "userUpdated";
    public static final String USER_DISABLED = "userDisabled";
}
