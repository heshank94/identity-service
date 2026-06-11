package com.dreamstartlabs.dreamlink.identity.service.keycloak;

import com.dreamstartlabs.dreamlink.identity.config.KeyCloakProps;
import com.dreamstartlabs.dreamlink.identity.core.AbstractTokenManager;
import com.dreamstartlabs.dreamlink.identity.core.client.keycloak.KeyCloakClient;
import com.dreamstartlabs.dreamlink.identity.exception.KeyCloakException;
import com.dreamstartlabs.dreamlink.identity.models.dto.KeyCloakUser;
import com.dreamstartlabs.dreamlink.identity.models.dto.OneLoginUser;
import com.dreamstartlabs.dreamlink.identity.models.response.TokenResponse;
import com.dreamstartlabs.dreamlink.identity.utils.KeyCloakUserMapperUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import static com.dreamstartlabs.dreamlink.identity.utils.Constants.AUTHORIZATION;

/**
 * @author Heshan Karunaratne
 */
@Service
@Slf4j
public class KeyCloakClientService extends AbstractTokenManager implements KeyCloakClient {
    private final WebClient webClient;
    private final KeyCloakProps keyCloakProps;
    private final KeyCloakUserMapperUtil keyCloakUserMapperUtil;

    public KeyCloakClientService(@Qualifier("keycloakWebClient") WebClient webClient,
                                 KeyCloakProps keyCloakProps,
                                 KeyCloakUserMapperUtil keyCloakUserMapperUtil) {
        this.webClient = webClient;
        this.keyCloakProps = keyCloakProps;
        this.keyCloakUserMapperUtil = keyCloakUserMapperUtil;
    }

    @Override
    protected TokenResponse fetchToken() {
        log.info("Fetching Keycloak admin token...");
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("client_id", keyCloakProps.getClientId());
        body.add("client_secret", keyCloakProps.getClientSecret());

        try {
            TokenResponse response = webClient
                    .post()
                    .uri("/realms/{realm}/protocol/openid-connect/token", keyCloakProps.getRealm())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(TokenResponse.class)
                    .block();

            if (response == null || response.accessToken() == null)
                throw new KeyCloakException("Invalid token response from Keycloak");

            log.info("Keycloak admin token acquired.");
            return response;

        } catch (Exception e) {
            log.error("Failed to fetch Keycloak token: {}", e.getMessage());
            throw new KeyCloakException("Keycloak authentication failed", e);
        }

    }

    @Override
    public boolean userExists(OneLoginUser oneLoginUser) {
        return findKeycloakUser(oneLoginUser).isPresent();
    }

    private Optional<KeyCloakUser> findKeycloakUser(OneLoginUser user) {
        return searchByAttribute("onelogin_id", String.valueOf(user.getId()))
                .or(() -> searchByParam("username", user.getUsername()))
                .or(() -> searchByParam("email", user.getEmail()))
                .map(u -> fetchFullUser(u.getId()));
    }

    @Override
    public KeyCloakUser findUserByOneLoginId(Long oneLoginId) {
        return searchByAttribute("onelogin_id", String.valueOf(oneLoginId))
                .map(u -> fetchFullUser(u.getId()))
                .orElse(null);
    }

    private static <T> Optional<T> firstOf(List<T> list) {
        return (list != null && !list.isEmpty()) ? Optional.of(list.getFirst()) : Optional.empty();
    }

    private String bearer() {
        return "bearer " + getValidToken();
    }

    private Optional<KeyCloakUser> searchByAttribute(String attrName, String attrValue) {
        log.debug("Searching Keycloak users by attribute {}={}", attrName, attrValue);
        try {
            List<KeyCloakUser> users = webClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/admin/realms/{realm}/users")
                            .queryParam("q", attrName + ":" + attrValue)
                            .queryParam("exact", true)
                            .queryParam("briefRepresentation", false)
                            .build(keyCloakProps.getRealm()))
                    .header(AUTHORIZATION, bearer())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<KeyCloakUser>>() {
                    })
                    .block();

            return firstOf(users);
        } catch (Exception e) {
            log.warn("Attribute search ({}={}) failed: {}", attrName, attrValue, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<KeyCloakUser> searchByParam(String paramName, String paramValue) {
        log.debug("Searching Keycloak users by {}={}", paramName, paramValue);
        try {
            List<KeyCloakUser> users = webClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/admin/realms/{realm}/users")
                            .queryParam(paramName, paramValue)
                            .queryParam("exact", true)
                            .build(keyCloakProps.getRealm()))
                    .header(AUTHORIZATION, bearer())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<KeyCloakUser>>() {
                    })
                    .block();

            return firstOf(users);
        } catch (Exception e) {
            log.error("Param search ({}={}) failed: {}", paramName, paramValue, e.getMessage());
            return Optional.empty();
        }
    }


    @Override
    public String createUser(OneLoginUser oneLoginUser, List<String> roleNames) {
        log.info("Creating Keycloak user: username={}", oneLoginUser.getUsername());
        try {
            ResponseEntity<Void> response = webClient
                    .post()
                    .uri("/admin/realms/{realm}/users", keyCloakProps.getRealm())
                    .header(AUTHORIZATION, bearer())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(keyCloakUserMapperUtil.toCreatePayload(oneLoginUser, roleNames))
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            if (response != null && response.getStatusCode().value() == 201) {

                String keycloakId = extractIdFromLocation(response)
                        .orElseThrow(() -> new KeyCloakException(
                                "User created but Location header missing — cannot extract ID"
                        ));

                linkFederatedIdentity(keycloakId, oneLoginUser);
                log.info("Keycloak user created with id={}", keycloakId);
                return keycloakId;
            }
            throw new KeyCloakException("Failed to create user — unexpected response");
        } catch (Exception e) {
            log.error("Failed to create Keycloak user '{}': {}", oneLoginUser.getUsername(), e.getMessage());
            throw new KeyCloakException("Failed to create user", e);
        }
    }

    private static Optional<String> extractIdFromLocation(ResponseEntity<?> response) {
        URI location = response.getHeaders().getLocation();
        if (location == null) return Optional.empty();
        String path = location.getPath();
        return Optional.of(path.substring(path.lastIndexOf('/') + 1));
    }

    @Override
    public String updateUser(OneLoginUser oneLoginUser, List<String> roleNames) {
        log.info("Updating Keycloak user: username={}", oneLoginUser.getUsername());

        KeyCloakUser existing = findKeycloakUser(oneLoginUser)
                .map(u -> fetchFullUser(u.getId()))
                .orElse(null);

        if (existing == null) {
            log.warn("Cannot update — user not found in Keycloak: {}", oneLoginUser.getUsername());
            throw new KeyCloakException("User not found for update");
        }

        try {
            webClient
                    .put()
                    .uri("/admin/realms/{realm}/users/{id}", keyCloakProps.getRealm(), existing.getId())
                    .header(AUTHORIZATION, bearer())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(keyCloakUserMapperUtil.toUpdatePayload(oneLoginUser, roleNames))
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            log.info("Keycloak user {} updated.", existing.getId());
            linkFederatedIdentity(existing.getId(), oneLoginUser);

            return existing.getId();
        } catch (Exception e) {
            log.error("Failed to update Keycloak user {}: {}", existing.getId(), e.getMessage());
            throw new KeyCloakException("Failed to update user", e);
        }
    }

    @Override
    public void disableUser(OneLoginUser oneLoginUser) {
        findKeycloakUser(oneLoginUser)
                .ifPresentOrElse(
                        u -> executeDisable(u.getId()),
                        () -> log.warn("Cannot disable — user not found: {}", oneLoginUser.getUsername())
                );
    }

    @Override
    public boolean disableUserByOneLoginId(Long oneLoginId) {
        KeyCloakUser user = findUserByOneLoginId(oneLoginId);
        if (user == null) return false;
        executeDisable(user.getId());
        return true;
    }

    private void executeDisable(String keycloakUserId) {
        log.info("Disabling Keycloak user: {}", keycloakUserId);
        try {
            webClient
                    .put()
                    .uri("/admin/realms/{realm}/users/{id}", keyCloakProps.getRealm(), keycloakUserId)
                    .header(AUTHORIZATION, bearer())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(keyCloakUserMapperUtil.toDisablePayload())
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            log.info("Keycloak user {} disabled.", keycloakUserId);
        } catch (Exception e) {
            log.error("Failed to disable Keycloak user {}: {}", keycloakUserId, e.getMessage());
            throw new KeyCloakException("Failed to disable user", e);
        }
    }

    @Override
    public void linkFederatedIdentity(String keycloakUserId, OneLoginUser oneLoginUser) {
        log.info("Linking user {} to IDP '{}' (OneLogin ID: {})",
                keycloakUserId, keyCloakProps.getIdpAlias(), oneLoginUser.getId());
        try {
            deleteExistingFederatedIdentity(keycloakUserId);

            webClient
                    .post()
                    .uri("/admin/realms/{realm}/users/{id}/federated-identity/{idp}",
                            keyCloakProps.getRealm(), keycloakUserId, keyCloakProps.getIdpAlias())
                    .header(AUTHORIZATION, bearer())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(keyCloakUserMapperUtil.toFederatedIdentityPayload(oneLoginUser))
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            log.info("Federated identity linked for user {}.", keycloakUserId);
        } catch (WebClientResponseException.Conflict e) {
            log.info("User {} already linked to IDP — skipping.", keycloakUserId);
        } catch (Exception e) {
            log.error("Failed to link federated identity for user {}: {}", keycloakUserId, e.getMessage());
            throw new KeyCloakException("Failed to link federated identity", e);
        }
    }


    private KeyCloakUser fetchFullUser(String keycloakUserId) {
        try {
            return webClient
                    .get()
                    .uri("/admin/realms/{realm}/users/{id}", keyCloakProps.getRealm(), keycloakUserId)
                    .header(AUTHORIZATION, bearer())
                    .retrieve()
                    .bodyToMono(KeyCloakUser.class)
                    .block();
        } catch (Exception e) {
            log.error("Failed to fetch Keycloak user {}: {}", keycloakUserId, e.getMessage());
            return null;
        }
    }

    private void deleteExistingFederatedIdentity(String keycloakUserId) {
        try {
            webClient
                    .delete()
                    .uri("/admin/realms/{realm}/users/{id}/federated-identity/{idp}",
                            keyCloakProps.getRealm(), keycloakUserId, keyCloakProps.getIdpAlias())
                    .header(AUTHORIZATION, bearer())
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            log.debug("Existing federated identity removed for user {}.", keycloakUserId);
        } catch (WebClientResponseException.NotFound e) {
            log.debug("No existing federated identity found for user {} — nothing to delete.", keycloakUserId);
        } catch (Exception e) {
            log.warn("Failed to delete existing federated identity for user {}: {}", keycloakUserId, e.getMessage());
        }

    }
}
