package com.dreamstartlabs.dreamlink.identity.exception;

/**
 * @author Heshan Karunaratne
 */
public class KeyCloakException extends RuntimeException {

    public KeyCloakException(String message) {
        super(message);
    }

    public KeyCloakException(String message, Throwable cause) {
        super(message, cause);
    }
}
