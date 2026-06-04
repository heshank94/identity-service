package com.dreamstartlabs.dreamlink.identity.client;

import com.dreamstartlabs.dreamlink.identity.config.SyncConfig;
import com.dreamstartlabs.dreamlink.identity.model.OneLoginEvent;
import com.dreamstartlabs.dreamlink.identity.model.OneLoginRole;
import com.dreamstartlabs.dreamlink.identity.model.OneLoginUser;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class OneLoginClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(OneLoginClient.class);

    private final SyncConfig.OneLoginProps oneLoginProps;
    private final RestClient restClient;

    private String accessToken;
    private Instant tokenExpiration;

    public OneLoginClient(SyncConfig.OneLoginProps oneLoginProps) {
        this.oneLoginProps = oneLoginProps;
        this.restClient = RestClient.builder()
                .baseUrl(oneLoginProps.getApiUrl())
                .build();
    }

    /**
     * Gets a valid access token. Fetches a new one if not available or expired.
     */
    private synchronized String getAccessToken() {
        if (accessToken == null || tokenExpiration == null || Instant.now().isAfter(tokenExpiration.minusSeconds(60))) {
            fetchToken();
        }
        return accessToken;
    }

    private void fetchToken() {
        LOGGER.info("Requesting new OneLogin access token...");
        String clientId = oneLoginProps.getClientId();
        String clientSecret = oneLoginProps.getClientSecret();

        Map<String, String> body = new HashMap<>();
        body.put("grant_type", "client_credentials");

        try {
            TokenResponse response = restClient.post()
                    .uri("/auth/oauth2/v2/token")
                    .header("Authorization", "client_id:" + clientId + ", client_secret:" + clientSecret)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(TokenResponse.class);

            if (response != null && response.getAccessToken() != null) {
                this.accessToken = response.getAccessToken();
                // If expires_in is present, use it, otherwise default to 36000 seconds (10 hours)
                long expiresIn = response.getExpiresIn() != null ? response.getExpiresIn() : 36000;
                this.tokenExpiration = Instant.now().plusSeconds(expiresIn);
                LOGGER.info("Successfully acquired OneLogin access token. Expires in {} seconds.", expiresIn);
            } else {
                throw new RuntimeException("Invalid token response from OneLogin");
            }
        } catch (Exception e) {
            LOGGER.error("Failed to fetch OneLogin access token: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to authenticate with OneLogin: " + e.getMessage(), e);
        }
    }

    /**
     * Fetches all users from OneLogin, with optional filtering by update time.
     * Uses cursor-based pagination to fetch all pages.
     */
    public List<OneLoginUser> getUsers(String updatedSince) {
        List<OneLoginUser> allUsers = new ArrayList<>();
        String cursor = null;
        boolean hasMore = true;

        LOGGER.info("Fetching users from OneLogin (updatedSince={})...", updatedSince);

        while (hasMore) {
            final String currentCursor = cursor;
            UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/api/2/users")
                    .queryParam("limit", 100);

            if (updatedSince != null && !updatedSince.isEmpty()) {
                builder.queryParam("updated_since", updatedSince);
            }
            if (currentCursor != null) {
                builder.queryParam("cursor", currentCursor);
            }

            try {
                String token = getAccessToken();
                ResponseEntity<List<OneLoginUser>> response = restClient.get()
                        .uri(builder.toUriString())
                        .header("Authorization", "bearer " + token)
                        .retrieve()
                        .toEntity(new ParameterizedTypeReference<List<OneLoginUser>>() {});

                List<OneLoginUser> users = response.getBody();
                if (users != null && !users.isEmpty()) {
                    allUsers.addAll(users);
                    LOGGER.debug("Fetched {} users in current page", users.size());
                }

                // Check for next page cursor in response headers
                List<String> afterCursorHeaders = response.getHeaders().get("After-Cursor");
                if (afterCursorHeaders != null && !afterCursorHeaders.isEmpty() && afterCursorHeaders.get(0) != null && !afterCursorHeaders.get(0).trim().isEmpty()) {
                    cursor = afterCursorHeaders.get(0);
                } else {
                    hasMore = false;
                }
            } catch (Exception e) {
                LOGGER.error("Failed to fetch OneLogin users page: {}", e.getMessage(), e);
                throw new RuntimeException("Error fetching OneLogin users: " + e.getMessage(), e);
            }
        }

        LOGGER.info("Successfully fetched a total of {} users from OneLogin.", allUsers.size());
        return allUsers;
    }

    /**
     * Fetches full user details by ID, including custom_attributes.
     */
    public OneLoginUser getUserById(Long userId) {
        LOGGER.debug("Fetching full OneLogin user details for ID: {}", userId);
        try {
            String token = getAccessToken();
            return restClient.get()
                    .uri("/api/2/users/" + userId)
                    .header("Authorization", "bearer " + token)
                    .retrieve()
                    .body(OneLoginUser.class);
        } catch (Exception e) {
            LOGGER.error("Failed to fetch OneLogin user details for ID {}: {}", userId, e.getMessage());
            return null;
        }
    }

    /**
     * Fetches a OneLogin role by its ID.
     *
     * @param roleId the OneLogin role ID
     * @return the role if found, or null if not found
     */
    public OneLoginRole getRoleById(Long roleId) {
        LOGGER.debug("Fetching OneLogin role details for ID: {}", roleId);
        try {
            String token = getAccessToken();
            return restClient.get()
                    .uri("/api/2/roles/" + roleId)
                    .header("Authorization", "bearer " + token)
                    .retrieve()
                    .body(OneLoginRole.class);
        } catch (Exception e) {
            LOGGER.error("Failed to fetch OneLogin role details for ID {}: {}", roleId, e.getMessage());
            return null;
        }
    }

    /**
     * Fetches deletion events from OneLogin since the specified timestamp.
     */
    public List<OneLoginEvent> getDeletionEvents(String since) {
        List<OneLoginEvent> allEvents = new ArrayList<>();
        String cursor = null;
        boolean hasMore = true;

        LOGGER.info("Fetching user deletion events from OneLogin (since={})...", since);

        while (hasMore) {
            final String currentCursor = cursor;
            UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/api/1/events")
                    .queryParam("event_type_id", 17) // 17 is User Deleted
                    .queryParam("limit", 100);

            if (since != null && !since.isEmpty()) {
                builder.queryParam("since", since);
            }
            if (currentCursor != null) {
                builder.queryParam("cursor", currentCursor);
            }

            try {
                String token = getAccessToken();
                ResponseEntity<EventResponse> response = restClient.get()
                        .uri(builder.toUriString())
                        .header("Authorization", "bearer " + token)
                        .retrieve()
                        .toEntity(EventResponse.class);

                EventResponse body = response.getBody();
                if (body != null && body.getData() != null && !body.getData().isEmpty()) {
                    allEvents.addAll(body.getData());
                    LOGGER.debug("Fetched {} deletion events in current page", body.getData().size());
                }

                List<String> afterCursorHeaders = response.getHeaders().get("After-Cursor");
                if (afterCursorHeaders != null && !afterCursorHeaders.isEmpty() && afterCursorHeaders.get(0) != null && !afterCursorHeaders.get(0).trim().isEmpty()) {
                    cursor = afterCursorHeaders.get(0);
                } else {
                    hasMore = false;
                }
            } catch (Exception e) {
                LOGGER.error("Failed to fetch OneLogin deletion events: {}", e.getMessage(), e);
                throw new RuntimeException("Error fetching OneLogin deletion events: " + e.getMessage(), e);
            }
        }

        LOGGER.info("Successfully fetched a total of {} user deletion events from OneLogin.", allEvents.size());
        return allEvents;
    }

    // Helper class for mapping OAuth token response
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TokenResponse {
        @JsonProperty("access_token")
        private String accessToken;

        @JsonProperty("expires_in")
        private Long expiresIn;

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }

        public Long getExpiresIn() {
            return expiresIn;
        }

        public void setExpiresIn(Long expiresIn) {
            this.expiresIn = expiresIn;
        }
    }

    // Helper class for mapping OneLogin Events API response envelope
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class EventResponse {
        private List<OneLoginEvent> data;

        public List<OneLoginEvent> getData() {
            return data;
        }

        public void setData(List<OneLoginEvent> data) {
            this.data = data;
        }
    }
}
