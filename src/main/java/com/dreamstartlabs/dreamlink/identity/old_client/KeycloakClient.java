package com.dreamstartlabs.dreamlink.identity.old_client;

import com.dreamstartlabs.dreamlink.identity.config.KeyCloakProps;
import com.dreamstartlabs.dreamlink.identity.old_config.SyncConfig;
import com.dreamstartlabs.dreamlink.identity.old_model.KeycloakRole;
import com.dreamstartlabs.dreamlink.identity.old_model.KeycloakUser;
import com.dreamstartlabs.dreamlink.identity.old_model.OneLoginUser;
import com.dreamstartlabs.dreamlink.identity.old_response.TokenResponse;
import com.dreamstartlabs.dreamlink.identity.old_utils.KeycloakUserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Instant;
import java.util.*;

/**
 * Keycloak Admin REST API client.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Token lifecycle management (fetch + cache with 60 s safety margin)</li>
 *   <li>CRUD operations on Keycloak users</li>
 *   <li>Federated-identity linking</li>
 * </ul>
 *
 * <p>Payload construction is delegated to {@link KeycloakUserMapper}.
 */
@Component
public class KeycloakClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeycloakClient.class);
    private static final int TOKEN_EXPIRY_BUFFER_SECONDS = 60;
    private static final int DEFAULT_TOKEN_TTL_SECONDS   = 300;

    private final KeyCloakProps keycloakProps;
    private final KeycloakUserMapper        userMapper;
    private final RestClient restClient;

    private String  accessToken;
    private Instant tokenExpiration;

    public KeycloakClient(KeyCloakProps keycloakProps, KeycloakUserMapper userMapper) {
        this.keycloakProps = keycloakProps;
        this.userMapper    = userMapper;
        this.restClient    = RestClient.builder()
                .baseUrl(keycloakProps.getApiUrl())
                .build();
    }

    // =========================================================================
    // Token management
    // =========================================================================

    /** Returns a cached, valid admin access token — refreshes automatically. */
    private synchronized String getAccessToken() {
        boolean needsRefresh = accessToken == null
                               || tokenExpiration == null
                               || Instant.now().isAfter(tokenExpiration.minusSeconds(TOKEN_EXPIRY_BUFFER_SECONDS));

        if (needsRefresh) {
            fetchToken();
        }
        return accessToken;
    }

    private void fetchToken() {
        LOGGER.info("Requesting new Keycloak admin access token...");

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type",    "client_credentials");
        body.add("client_id",     keycloakProps.getClientId());
        body.add("client_secret", keycloakProps.getClientSecret());

        try {
            TokenResponse response = restClient.post()
                    .uri("/realms/{realm}/protocol/openid-connect/token", keycloakProps.getRealm())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(TokenResponse.class);

            if (response == null || response.accessToken() == null) {
                throw new IllegalStateException("Token response was empty or missing access_token");
            }

            long expiresIn   = Optional.ofNullable(response.expiresIn()).orElse((long) DEFAULT_TOKEN_TTL_SECONDS);
            this.accessToken      = response.accessToken();
            this.tokenExpiration  = Instant.now().plusSeconds(expiresIn);
            LOGGER.info("Keycloak admin token acquired. Expires in {} s.", expiresIn);

        } catch (Exception e) {
            LOGGER.error("Failed to fetch Keycloak admin access token: {}", e.getMessage(), e);
            throw new RuntimeException("Keycloak authentication failed: " + e.getMessage(), e);
        }
    }

    // =========================================================================
    // User lookups
    // =========================================================================

    /**
     * Searches for a Keycloak user that corresponds to the given OneLogin user.
     *
     * <p>Strategy (first match wins):
     * <ol>
     *   <li>Custom attribute {@code onelogin_id}</li>
     *   <li>Exact username match</li>
     *   <li>Exact e-mail match</li>
     * </ol>
     *
     * @return the fully-hydrated {@link KeycloakUser}, or {@code null} if not found.
     */
    public KeycloakUser findUser(OneLoginUser oneLoginUser) {
        String realm = keycloakProps.getRealm();

        return searchByAttribute(realm, "onelogin_id", String.valueOf(oneLoginUser.getId()))
                .or(() -> searchByParam(realm, "username", oneLoginUser.getUsername()))
                .or(() -> searchByParam(realm, "email",    oneLoginUser.getEmail()))
                .map(u -> getUserById(u.getId()))
                .orElse(null);
    }

    /**
     * Finds a Keycloak user by their stored {@code onelogin_id} attribute.
     *
     * @return the fully-hydrated {@link KeycloakUser}, or {@code null} if not found.
     */
    public KeycloakUser findUserByOneLoginId(Long oneLoginId) {
        String realm = keycloakProps.getRealm();
        return searchByAttribute(realm, "onelogin_id", String.valueOf(oneLoginId))
                .map(u -> getUserById(u.getId()))
                .orElse(null);
    }

    /**
     * Fetches the full user representation by Keycloak ID.
     * User-list endpoints omit attributes, so this is always needed after a search.
     *
     * @return the {@link KeycloakUser}, or {@code null} on failure.
     */
    public KeycloakUser getUserById(String userId) {
        String realm = keycloakProps.getRealm();
        LOGGER.debug("Fetching full Keycloak user for ID: {}", userId);

        return executeGet(
                "/admin/realms/{realm}/users/{id}",
                KeycloakUser.class,
                "fetch user by ID " + userId,
                realm, userId
        ).orElse(null);
    }

    // =========================================================================
    // User mutations
    // =========================================================================

    /**
     * Creates a new user in Keycloak and immediately links their OneLogin federated identity.
     */
    public void createUser(OneLoginUser oneLoginUser) {
        String realm = keycloakProps.getRealm();
        LOGGER.info("Creating Keycloak user: username={}, email={}", oneLoginUser.getUsername(), oneLoginUser.getEmail());

        Map<String, Object> body = userMapper.toKeycloakPayload(oneLoginUser);
        LOGGER.debug("Create user payload: {}", body);

        try {
            ResponseEntity<Void> response = restClient.post()
                    .uri("/admin/realms/{realm}/users", realm)
                    .header("Authorization", bearerToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            if (response.getStatusCode().value() == 201) {
                extractIdFromLocation(response)
                        .ifPresentOrElse(
                                id -> linkFederatedIdentity(id, oneLoginUser),
                                () -> LOGGER.warn("User created but Location header missing; cannot link federated identity.")
                        );
            } else {
                LOGGER.error("Unexpected status {} creating Keycloak user '{}'",
                        response.getStatusCode(), oneLoginUser.getUsername());
            }
        } catch (Exception e) {
            LOGGER.error("Error creating Keycloak user '{}': {}", oneLoginUser.getUsername(), e.getMessage(), e);
        }
    }

    /**
     * Links an existing Keycloak user to their OneLogin Identity Provider identity.
     * Safe to call when the link already exists (409 is silently swallowed).
     */
    public void linkFederatedIdentity(String keycloakUserId, OneLoginUser oneLoginUser) {
        String realm     = keycloakProps.getRealm();
        String idpAlias  = keycloakProps.getIdpAlias();

        Map<String, String> body = userMapper.toFederatedIdentityPayload(oneLoginUser, idpAlias);
        LOGGER.info("Linking user {} to IDP '{}' (OneLogin ID: {})", keycloakUserId, idpAlias, oneLoginUser.getId());
        LOGGER.debug("Federated identity payload: {}", body);

        try {
            restClient.post()
                    .uri("/admin/realms/{realm}/users/{id}/federated-identity/{idp}", realm, keycloakUserId, idpAlias)
                    .header("Authorization", bearerToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            LOGGER.info("Federated identity linked for user {}.", keycloakUserId);
        } catch (HttpClientErrorException.Conflict e) {
            LOGGER.info("User {} is already linked to IDP '{}' — skipping.", keycloakUserId, idpAlias);
        } catch (Exception e) {
            LOGGER.error("Failed to link federated identity for user {}: {}", keycloakUserId, e.getMessage(), e);
        }
    }

    /**
     * Updates an existing Keycloak user's attributes from OneLogin data.
     */
    public void updateUser(String keycloakUserId, OneLoginUser oneLoginUser) {
        String realm = keycloakProps.getRealm();
        LOGGER.info("Updating Keycloak user {}: username={}", keycloakUserId, oneLoginUser.getUsername());

        try {
            // 1. Fetch existing user (to preserve attributes like dreamlink_roles)
            KeycloakUser existingUser = getUserById(keycloakUserId);

            // 2. Build new payload from mapper
            Map<String, Object> body = userMapper.toKeycloakPayload(oneLoginUser);

            // 3. Merge attributes (CRITICAL FIX)
            Map<String, List<String>> mergedAttributes = new HashMap<>();

            if (existingUser != null && existingUser.getAttributes() != null) {
                mergedAttributes.putAll(existingUser.getAttributes());
            }

            Map<String, List<String>> newAttributes =
                    (Map<String, List<String>>) body.get("attributes");

            if (newAttributes != null) {
                mergedAttributes.putAll(newAttributes); // overwrite only mapper fields
            }

            body.put("attributes", mergedAttributes);

            LOGGER.debug("Update user payload (merged): {}", body);

            // 4. Send update request
            restClient.put()
                    .uri("/admin/realms/{realm}/users/{id}", realm, keycloakUserId)
                    .header("Authorization", bearerToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            LOGGER.info("Keycloak user {} updated.", keycloakUserId);

        } catch (Exception e) {
            LOGGER.error("Failed to update Keycloak user {}: {}", keycloakUserId, e.getMessage(), e);
        }
    }

    /**
     * Deletes a user from Keycloak.
     */
    public void deleteUser(String keycloakUserId) {
        String realm = keycloakProps.getRealm();
        LOGGER.info("Deleting Keycloak user {}...", keycloakUserId);

        try {
            restClient.delete()
                    .uri("/admin/realms/{realm}/users/{id}", realm, keycloakUserId)
                    .header("Authorization", bearerToken())
                    .retrieve()
                    .toBodilessEntity();
            LOGGER.info("Keycloak user {} deleted.", keycloakUserId);
        } catch (Exception e) {
            LOGGER.error("Failed to delete Keycloak user {}: {}", keycloakUserId, e.getMessage(), e);
        }
    }

    // =========================================================================
    // Role management
    // =========================================================================

    /**
     * Fetches all realm-level roles from Keycloak.
     *
     * @return list of all realm roles, or empty list if none found
     */
    public List<KeycloakRole> getAllRoles() {
        String realm = keycloakProps.getRealm();
        LOGGER.debug("Fetching all realm roles from Keycloak...");

        try {
            List<KeycloakRole> roles = restClient.get()
                    .uri("/admin/realms/{realm}/roles", realm)
                    .header("Authorization", bearerToken())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<KeycloakRole>>() {});

            LOGGER.debug("Successfully fetched {} realm roles.", roles == null ? 0 : roles.size());
            return roles != null ? roles : new ArrayList<>();
        } catch (Exception e) {
            LOGGER.error("Failed to fetch realm roles: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Fetches a specific realm-level role by name.
     *
     * @param roleName the name of the role to fetch
     * @return the role if found, or null if not found
     */
    public KeycloakRole getRoleByName(String roleName) {
        String realm = keycloakProps.getRealm();
        LOGGER.debug("Fetching role by name: {}", roleName);

        try {
            KeycloakRole role = restClient.get()
                    .uri("/admin/realms/{realm}/roles/{name}", realm, roleName)
                    .header("Authorization", bearerToken())
                    .retrieve()
                    .body(KeycloakRole.class);

            if (role != null) {
                LOGGER.debug("Found role: {} (ID: {})", roleName, role.getId());
            } else {
                LOGGER.debug("Role not found: {}", roleName);
            }
            return role;
        } catch (Exception e) {
            LOGGER.debug("Failed to fetch role by name '{}': {}", roleName, e.getMessage());
            return null;
        }
    }

    /**
     * Creates a new realm-level role in Keycloak.
     *
     * @param roleName the name of the role to create
     * @return the created role, or null if creation failed
     */
    public KeycloakRole createRole(String roleName) {
        String realm = keycloakProps.getRealm();
        LOGGER.info("Creating new Keycloak role: {}", roleName);

        Map<String, Object> rolePayload = new java.util.HashMap<>();
        rolePayload.put("name", roleName);
        rolePayload.put("description", "Synced from OneLogin");

        try {
            ResponseEntity<Void> response = restClient.post()
                    .uri("/admin/realms/{realm}/roles", realm)
                    .header("Authorization", bearerToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(rolePayload)
                    .retrieve()
                    .toBodilessEntity();

            if (response.getStatusCode().value() == 201) {
                LOGGER.info("Successfully created role: {}", roleName);
                // Fetch the role to get its ID
                return getRoleByName(roleName);
            } else {
                LOGGER.error("Unexpected status {} creating role '{}'", response.getStatusCode(), roleName);
                return null;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to create role '{}': {}", roleName, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Assigns a realm-level role to a user.
     * This grants the role to the user at the realm level.
     *
     * @param keycloakUserId the ID of the user
     * @param roleName the name of the role to assign
     * @return true if assignment succeeded, false otherwise
     */
    public boolean assignRoleToUser(String keycloakUserId, String roleName) {
        String realm = keycloakProps.getRealm();
        LOGGER.info("Assigning role '{}' to user {}", roleName, keycloakUserId);

        try {
            // First, get the role by name to obtain its ID
            KeycloakRole role = getRoleByName(roleName);
            if (role == null) {
                LOGGER.warn("Role '{}' not found. Cannot assign to user {}", roleName, keycloakUserId);
                return false;
            }

            // Prepare the role representation for assignment
            List<KeycloakRole> rolesList = new ArrayList<>();
            rolesList.add(role);

            restClient.post()
                    .uri("/admin/realms/{realm}/users/{userId}/role-mappings/realm", realm, keycloakUserId)
                    .header("Authorization", bearerToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(rolesList)
                    .retrieve()
                    .toBodilessEntity();

            LOGGER.info("Successfully assigned role '{}' to user {}", roleName, keycloakUserId);
            return true;
        } catch (HttpClientErrorException.Conflict e) {
            LOGGER.info("User {} already has role '{}' — skipping.", keycloakUserId, roleName);
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to assign role '{}' to user {}: {}", roleName, keycloakUserId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Removes a realm-level role from a user.
     *
     * @param keycloakUserId the ID of the user
     * @param roleName the name of the role to remove
     * @return true if removal succeeded, false otherwise
     */
    public boolean removeRoleFromUser(String keycloakUserId, String roleName) {
        String realm = keycloakProps.getRealm();
        LOGGER.info("Removing role '{}' from user {}", roleName, keycloakUserId);

        try {
            // First, get the role by name to obtain its ID
            KeycloakRole role = getRoleByName(roleName);
            if (role == null) {
                LOGGER.warn("Role '{}' not found. Cannot remove from user {}", roleName, keycloakUserId);
                return false;
            }

            // Prepare the role representation for removal
            List<KeycloakRole> rolesList = new ArrayList<>();
            rolesList.add(role);

            restClient.method(org.springframework.http.HttpMethod.DELETE)
                    .uri("/admin/realms/{realm}/users/{userId}/role-mappings/realm", realm, keycloakUserId)
                    .header("Authorization", bearerToken())
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .body(rolesList)
                    .retrieve()
                    .toBodilessEntity();

            LOGGER.info("Successfully removed role '{}' from user {}", roleName, keycloakUserId);
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to remove role '{}' from user {}: {}", roleName, keycloakUserId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Fetches the realm-level roles assigned to a specific user.
     *
     * @param keycloakUserId the ID of the user
     * @return list of roles assigned to the user, or empty list if none
     */
    public List<KeycloakRole> getUserRoles(String keycloakUserId) {
        String realm = keycloakProps.getRealm();
        LOGGER.debug("Fetching roles for user {}", keycloakUserId);

        try {
            List<KeycloakRole> roles = restClient.get()
                    .uri("/admin/realms/{realm}/users/{userId}/role-mappings/realm", realm, keycloakUserId)
                    .header("Authorization", bearerToken())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<KeycloakRole>>() {});

            LOGGER.debug("User {} has {} realm role(s).", keycloakUserId, roles == null ? 0 : roles.size());
            return roles != null ? roles : new ArrayList<>();
        } catch (Exception e) {
            LOGGER.error("Failed to fetch user roles for {}: {}", keycloakUserId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /** Produces the Authorization header value from the current token. */
    private String bearerToken() {
        return "Bearer " + getAccessToken();
    }

    /**
     * Generic GET helper — handles logging and exception wrapping in one place.
     */
    private <T> Optional<T> executeGet(String uriTemplate, Class<T> responseType, String operationLabel, Object... uriVars) {
        try {
            T result = restClient.get()
                    .uri(uriTemplate, uriVars)
                    .header("Authorization", bearerToken())
                    .retrieve()
                    .body(responseType);
            return Optional.ofNullable(result);
        } catch (Exception e) {
            LOGGER.error("Failed to {}: {}", operationLabel, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Searches for users via Keycloak's {@code ?q=attribute:value} filter.
     * Returns an {@link Optional} holding the first result's partial representation
     * (attributes are absent — always follow up with {@link #getUserById}).
     */
    private Optional<KeycloakUser> searchByAttribute(String realm, String attrName, String attrValue) {
        LOGGER.debug("Searching Keycloak users by attribute {}={}", attrName, attrValue);
        try {
            List<KeycloakUser> users = restClient.get()
                    .uri("/admin/realms/{realm}/users?q={attr}:{val}", realm, attrName, attrValue)
                    .header("Authorization", bearerToken())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            LOGGER.debug("Attribute search ({}={}) returned {} result(s).", attrName, attrValue,
                    users == null ? 0 : users.size());
            return firstOf(users);
        } catch (Exception e) {
            LOGGER.warn("Attribute search ({}={}) failed: {} — will try next strategy.", attrName, attrValue, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Searches for users by a simple query parameter (e.g. {@code username}, {@code email})
     * using Keycloak's {@code &exact=true} flag.
     */
    private Optional<KeycloakUser> searchByParam(String realm, String paramName, String paramValue) {
        LOGGER.debug("Searching Keycloak users by {}={}", paramName, paramValue);
        try {
            List<KeycloakUser> users = restClient.get()
                    .uri("/admin/realms/{realm}/users?{param}={val}&exact=true", realm, paramName, paramValue)
                    .header("Authorization", bearerToken())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            LOGGER.debug("Param search ({}={}) returned {} result(s).", paramName, paramValue,
                    users == null ? 0 : users.size());
            return firstOf(users);
        } catch (Exception e) {
            LOGGER.error("Param search ({}={}) failed: {}", paramName, paramValue, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /** Returns an Optional of the first element, or empty if the list is null/empty. */
    private static <T> Optional<T> firstOf(List<T> list) {
        return (list != null && !list.isEmpty()) ? Optional.of(list.get(0)) : Optional.empty();
    }

    /** Extracts the newly-created Keycloak user ID from a 201 Location header. */
    private static Optional<String> extractIdFromLocation(ResponseEntity<?> response) {
        URI location = response.getHeaders().getLocation();
        if (location == null) return Optional.empty();
        String path = location.getPath();
        return Optional.of(path.substring(path.lastIndexOf('/') + 1));
    }

    public boolean setUserRolesAttribute(String keycloakUserId, List<String> roleNames) {
        String realm = keycloakProps.getRealm();
        LOGGER.info("Setting dreamlink_roles for user {} -> {}", keycloakUserId, roleNames);

        try {
            if (roleNames == null || roleNames.isEmpty()) {
                LOGGER.warn("No roles provided for user {}. Skipping attribute update.", keycloakUserId);
                return false;
            }

            // 1. Fetch existing user (IMPORTANT: we need current attributes)
            KeycloakUser user = getUserById(keycloakUserId);
            if (user == null) {
                LOGGER.warn("User {} not found in Keycloak.", keycloakUserId);
                return false;
            }

            // 2. Get or initialize attributes
            Map<String, List<String>> attributes =
                    user.getAttributes() != null ? user.getAttributes() : new HashMap<>();

            // 3. Set dreamlink_roles
            attributes.put("dreamlink_roles", roleNames);

            // 4. Build payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("attributes", attributes);

            LOGGER.debug("Updating dreamlink_roles payload: {}", payload);

            // 5. Send update request
            restClient.put()
                    .uri("/admin/realms/{realm}/users/{id}", realm, keycloakUserId)
                    .header("Authorization", bearerToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            LOGGER.info("Successfully updated dreamlink_roles for user {}", keycloakUserId);
            return true;

        } catch (Exception e) {
            LOGGER.error("Failed to set dreamlink_roles for user {}: {}", keycloakUserId, e.getMessage(), e);
            return false;
        }
    }
}