package com.dreamstartlabs.dreamlink.identity.exception;

/**
 * @author Heshan Karunaratne
 */
public class OneLoginException extends RuntimeException {

    public OneLoginException(String message) {
        super(message);
    }

    public OneLoginException(String message, Throwable cause) {
        super(message, cause);
    }
}
