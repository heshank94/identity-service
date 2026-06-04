package com.dreamstartlabs.dreamlink.identity.client;

import com.dreamstartlabs.dreamlink.identity.config.SyncConfig;
import com.dreamstartlabs.dreamlink.identity.model.OneLoginUser;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class KeycloakClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeycloakClient.class);

    private final SyncConfig.KeycloakProps keycloakProps;
    private final RestClient restClient;

    private String accessToken;
    private Instant tokenExpiration;

    public KeycloakClient(SyncConfig.KeycloakProps keycloakProps) {
        this.keycloakProps = keycloakProps;
        this.restClient = RestClient.builder()
                .baseUrl(keycloakProps.getApiUrl())
                .build();
    }

    /**
     * Gets a valid Keycloak Admin access token.
     */
    private synchronized String getAccessToken() {
        if (accessToken == null || tokenExpiration == null || Instant.now().isAfter(tokenExpiration.minusSeconds(60))) {
            fetchToken();
        }
        return accessToken;
    }

    private void fetchToken() {
        LOGGER.info("Requesting new Keycloak admin access token...");
        String realm = keycloakProps.getRealm();
        String clientId = keycloakProps.getClientId();
        String clientSecret = keycloakProps.getClientSecret();

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);

        try {
            TokenResponse response = restClient.post()
                    .uri("/realms/" + realm + "/protocol/openid-connect/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(TokenResponse.class);

            if (response != null && response.getAccessToken() != null) {
                this.accessToken = response.getAccessToken();
                long expiresIn = response.getExpiresIn() != null ? response.getExpiresIn() : 300;
                this.tokenExpiration = Instant.now().plusSeconds(expiresIn);
                LOGGER.info("Successfully acquired Keycloak admin access token. Expires in {} seconds.", expiresIn);
            } else {
                throw new RuntimeException("Invalid token response from Keycloak");
            }
        } catch (Exception e) {
            LOGGER.error("Failed to fetch Keycloak admin access token: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to authenticate with Keycloak: " + e.getMessage(), e);
        }
    }

    /**
     * Searches for a user in Keycloak.
     * First attempts to query by the custom 'onelogin_id' attribute using the 'q'
     * parameter.
     * If not found, falls back to querying by username.
     */
    public KeycloakUser findUser(OneLoginUser oneLoginUser) {
        String realm = keycloakProps.getRealm();
        String token = getAccessToken();

        // 1. Search by custom attribute
        try {
            LOGGER.debug("Searching Keycloak user by attribute 'onelogin_id': {}", oneLoginUser.getId());
            List<KeycloakUser> users = restClient.get()
                    .uri("/admin/realms/" + realm + "/users?q=onelogin_id:" + oneLoginUser.getId())
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<KeycloakUser>>() {
                    });

            LOGGER.debug("Keycloak search by attribute result: {}", users);
            if (users != null && !users.isEmpty()) {
                LOGGER.debug("Found Keycloak user by onelogin_id: {}", oneLoginUser.getId());
                return getUserById(users.get(0).getId());
            }
        } catch (Exception e) {
            LOGGER.warn(
                    "Failed to search user by attribute 'onelogin_id' via q parameter: {}. Falling back to username search.",
                    e.getMessage());
        }

        // 2. Fallback: Search by username
        try {
            LOGGER.debug("Searching Keycloak user by username: {}", oneLoginUser.getUsername());
            List<KeycloakUser> users = restClient.get()
                    .uri("/admin/realms/" + realm + "/users?username=" + oneLoginUser.getUsername() + "&exact=true")
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<KeycloakUser>>() {
                    });

            LOGGER.debug("Keycloak search by username result: {}", users);
            if (users != null && !users.isEmpty()) {
                LOGGER.debug("Found Keycloak user by username: {}", oneLoginUser.getUsername());
                return getUserById(users.get(0).getId());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to search user by username: {}", e.getMessage(), e);
        }

        // 3. Fallback: Search by email
        try {
            LOGGER.debug("Searching Keycloak user by email: {}", oneLoginUser.getEmail());
            List<KeycloakUser> users = restClient.get()
                    .uri("/admin/realms/" + realm + "/users?email=" + oneLoginUser.getEmail() + "&exact=true")
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<KeycloakUser>>() {
                    });

            LOGGER.debug("Keycloak search by email result: {}", users);
            if (users != null && !users.isEmpty()) {
                LOGGER.debug("Found Keycloak user by email: {}", oneLoginUser.getEmail());
                return getUserById(users.get(0).getId());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to search user by email: {}", e.getMessage(), e);
        }

        return null;
    }

    /**
     * Finds a user directly by their OneLogin ID attribute.
     */
    public KeycloakUser findUserByOneLoginId(Long oneLoginId) {
        String realm = keycloakProps.getRealm();
        String token = getAccessToken();

        try {
            LOGGER.debug("Searching Keycloak user by direct onelogin_id: {}", oneLoginId);
            List<KeycloakUser> users = restClient.get()
                    .uri("/admin/realms/" + realm + "/users?q=onelogin_id:" + oneLoginId)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<KeycloakUser>>() {
                    });

            LOGGER.debug("Keycloak direct onelogin_id search result: {}", users);
            if (users != null && !users.isEmpty()) {
                return getUserById(users.get(0).getId());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to find Keycloak user by onelogin_id {}: {}", oneLoginId, e.getMessage());
        }
        return null;
    }

    /**
     * Fetches a full user representation by their Keycloak ID.
     * This is necessary because user lists from search endpoints do not return user
     * attributes.
     */
    public KeycloakUser getUserById(String userId) {
        String realm = keycloakProps.getRealm();
        String token = getAccessToken();
        try {
            LOGGER.debug("Fetching full Keycloak user details for ID: {}", userId);
            return restClient.get()
                    .uri("/admin/realms/" + realm + "/users/" + userId)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(KeycloakUser.class);
        } catch (Exception e) {
            LOGGER.error("Failed to fetch full Keycloak user by ID {}: {}", userId, e.getMessage());
            return null;
        }
    }

    /**
     * Creates a user in Keycloak and links their federated identity.
     */
    public void createUser(OneLoginUser oneLoginUser) {
        String realm = keycloakProps.getRealm();
        String token = getAccessToken();

        Map<String, Object> body = new HashMap<>();
        body.put("username", oneLoginUser.getUsername());
        body.put("email", oneLoginUser.getEmail());
        body.put("firstName", oneLoginUser.getFirstName());
        body.put("lastName", oneLoginUser.getLastName());
        body.put("enabled", isUserEnabled(oneLoginUser.getStatus()));
        body.put("emailVerified", true);

        // Store user attributes (onelogin_id, tenant, role_ids)
        body.put("attributes", buildUserAttributes(oneLoginUser));

        try {
            LOGGER.info("Creating user in Keycloak: username={}, email={}", oneLoginUser.getUsername(),
                    oneLoginUser.getEmail());
            LOGGER.debug("Create Keycloak user request payload: {}", body);
            ResponseEntity<Void> response = restClient.post()
                    .uri("/admin/realms/" + realm + "/users")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            if (response.getStatusCode().value() == 201) {
                URI location = response.getHeaders().getLocation();
                if (location != null) {
                    String path = location.getPath();
                    String keycloakUserId = path.substring(path.lastIndexOf('/') + 1);
                    LOGGER.info("User created successfully in Keycloak with ID: {}", keycloakUserId);

                    // Create federated identity link
                    linkFederatedIdentity(keycloakUserId, oneLoginUser);
                } else {
                    LOGGER.warn(
                            "User created in Keycloak, but Location header was missing. Cannot link federated identity immediately.");
                }
            } else {
                LOGGER.error("Failed to create Keycloak user: HTTP {}", response.getStatusCode());
            }
        } catch (Exception e) {
            LOGGER.error("Error creating Keycloak user: {}", e.getMessage(), e);
        }
    }

    /**
     * Links the Keycloak user to their OneLogin Identity Provider identity.
     */
    public void linkFederatedIdentity(String keycloakUserId, OneLoginUser oneLoginUser) {
        String realm = keycloakProps.getRealm();
        String idpAlias = keycloakProps.getIdpAlias();
        String token = getAccessToken();

        Map<String, String> body = new HashMap<>();
        body.put("identityProvider", idpAlias);
        body.put("userId", String.valueOf(oneLoginUser.getId()));
        body.put("userName", oneLoginUser.getUsername()); // Or email depending on IDP configuration

        try {
            LOGGER.info("Linking user {} to Identity Provider '{}' (IDP UserId: {})", keycloakUserId, idpAlias,
                    oneLoginUser.getId());
            LOGGER.debug("Link federated identity request payload: {}", body);
            restClient.post()
                    .uri("/admin/realms/" + realm + "/users/" + keycloakUserId + "/federated-identity/" + idpAlias)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            LOGGER.info("Successfully linked federated identity for user: {}", keycloakUserId);
        } catch (org.springframework.web.client.HttpClientErrorException.Conflict e) {
            LOGGER.info("User {} is already linked to Identity Provider '{}'.", keycloakUserId, idpAlias);
        } catch (Exception e) {
            LOGGER.error("Failed to link federated identity for user {}: {}", keycloakUserId, e.getMessage(), e);
        }
    }

    /**
     * Updates an existing Keycloak user with values from OneLogin.
     */
    public void updateUser(String keycloakUserId, OneLoginUser oneLoginUser) {
        String realm = keycloakProps.getRealm();
        String token = getAccessToken();

        Map<String, Object> body = new HashMap<>();
        body.put("username", oneLoginUser.getUsername());
        body.put("email", oneLoginUser.getEmail());
        body.put("firstName", oneLoginUser.getFirstName());
        body.put("lastName", oneLoginUser.getLastName());
        body.put("enabled", isUserEnabled(oneLoginUser.getStatus()));

        // Keep attributes updated (onelogin_id, tenant, role_ids)
        body.put("attributes", buildUserAttributes(oneLoginUser));

        try {
            LOGGER.info("Updating user {} in Keycloak: username={}", keycloakUserId, oneLoginUser.getUsername());
            LOGGER.debug("Update Keycloak user request payload: {}", body);
            restClient.put()
                    .uri("/admin/realms/" + realm + "/users/" + keycloakUserId)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            LOGGER.info("User {} updated successfully in Keycloak", keycloakUserId);
        } catch (Exception e) {
            LOGGER.error("Failed to update user {}: {}", keycloakUserId, e.getMessage(), e);
        }
    }

    /**
     * Deletes a user from Keycloak.
     */
    public void deleteUser(String keycloakUserId) {
        String realm = keycloakProps.getRealm();
        String token = getAccessToken();

        try {
            LOGGER.info("Deleting user {} from Keycloak...", keycloakUserId);
            restClient.delete()
                    .uri("/admin/realms/" + realm + "/users/" + keycloakUserId)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .toBodilessEntity();
            LOGGER.info("User {} deleted successfully from Keycloak", keycloakUserId);
        } catch (Exception e) {
            LOGGER.error("Failed to delete user {}: {}", keycloakUserId, e.getMessage(), e);
        }
    }

    /**
     * Helper to construct Keycloak user attributes mapping.
     */
    private Map<String, List<String>> buildUserAttributes(OneLoginUser oneLoginUser) {
        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put("onelogin_id", Collections.singletonList(String.valueOf(oneLoginUser.getId())));

        // Add tenant from custom_attributes if present
        if (oneLoginUser.getCustomAttributes() != null && oneLoginUser.getCustomAttributes().containsKey("tenant")) {
            Object tenantVal = oneLoginUser.getCustomAttributes().get("tenant");
            if (tenantVal != null) {
                attributes.put("tenant", Collections.singletonList(String.valueOf(tenantVal)));
            }
        }

        // Add role_ids if present
        if (oneLoginUser.getRoleIds() != null && !oneLoginUser.getRoleIds().isEmpty()) {
            List<String> roleIds = oneLoginUser.getRoleIds().stream()
                    .map(String::valueOf)
                    .toList();
            attributes.put("role_ids", roleIds);
        }

        return attributes;
    }

    /**
     * Maps OneLogin user status to active/inactive (enabled) state in Keycloak.
     * Status: 1 = Active (all other operational values except 2, 3 represent active
     * for Keycloak login)
     * Status: 2 = Suspended, 3 = Locked should disable the user.
     */
    private boolean isUserEnabled(Integer status) {
        if (status == null) {
            return true;
        }
        return status != 2 && status != 3;
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

    // Representation of a Keycloak user searched from endpoints
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KeycloakUser {
        private String id;
        private String username;
        private String email;
        private boolean enabled;
        private Map<String, List<String>> attributes;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Map<String, List<String>> getAttributes() {
            return attributes;
        }

        public void setAttributes(Map<String, List<String>> attributes) {
            this.attributes = attributes;
        }

        public String getOneLoginId() {
            if (attributes != null && attributes.containsKey("onelogin_id")) {
                List<String> values = attributes.get("onelogin_id");
                if (values != null && !values.isEmpty()) {
                    return values.get(0);
                }
            }
            return null;
        }
    }
}
