package com.dreamstartlabs.dreamlink.identity.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * OAuth token response.
 *
 * @param accessToken access token returned by the authorization server
 * @param refreshToken refresh token returned by the authorization server
 * @param expiresIn access token expiration time in seconds
 * @author Heshan Karunaratne
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("expires_in") Long expiresIn
) {
}
