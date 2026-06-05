package com.dreamstartlabs.dreamlink.identity.core;

import com.dreamstartlabs.dreamlink.identity.old_response.TokenResponse;

import java.time.Instant;

import static com.dreamstartlabs.dreamlink.identity.utils.Constants.REFRESH_BUFFER_SECONDS;

/**
 * @author Heshan Karunaratne
 */
public abstract class AbstractTokenManager {

    private String accessToken;
    private Instant expiry;

    protected abstract TokenResponse fetchToken();

    protected synchronized String getValidToken() {
        if (accessToken == null || expiry == null || Instant.now().isAfter(expiry.minusSeconds(REFRESH_BUFFER_SECONDS))) {
            TokenResponse token = fetchToken();

            if (token == null || token.accessToken() == null) {
                throw new IllegalStateException("Failed to obtain access token");
            }

            this.accessToken = token.accessToken();
            long ttl = token.expiresIn() != null ? token.expiresIn() : 36000;
            this.expiry = Instant.now().plusSeconds(ttl);
        }
        return accessToken;
    }
}