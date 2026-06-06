package com.dreamstartlabs.dreamlink.identity.service.onelogin;

import com.dreamstartlabs.dreamlink.identity.config.OneLoginProps;
import com.dreamstartlabs.dreamlink.identity.core.AbstractTokenManager;
import com.dreamstartlabs.dreamlink.identity.core.client.onelogin.OneLoginClient;
import com.dreamstartlabs.dreamlink.identity.exception.OneLoginException;
import com.dreamstartlabs.dreamlink.identity.models.response.EventResponse;
import com.dreamstartlabs.dreamlink.identity.models.response.TokenResponse;
import com.dreamstartlabs.dreamlink.identity.models.dto.OneLoginEvent;
import com.dreamstartlabs.dreamlink.identity.models.dto.OneLoginRole;
import com.dreamstartlabs.dreamlink.identity.models.dto.OneLoginUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

import static com.dreamstartlabs.dreamlink.identity.utils.Constants.AUTHORIZATION;

/**
 * @author Heshan Karunaratne
 */
@Service
@Slf4j
public class OneLoginClientService extends AbstractTokenManager implements OneLoginClient {

    private final WebClient webClient;
    private final OneLoginProps oneLoginProps;

    public OneLoginClientService(@Qualifier("oneLoginWebClient") WebClient webClient, OneLoginProps oneLoginProps) {
        this.webClient = webClient;
        this.oneLoginProps = oneLoginProps;
    }

    @Override
    public TokenResponse fetchToken() {
        log.info("Fetching OneLogin access token...");
        try {

            TokenResponse response = webClient
                    .post()
                    .uri("/auth/oauth2/v2/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .bodyValue(buildTokenRequestBody())
                    .retrieve()
                    .bodyToMono(TokenResponse.class)
                    .block();

            validateTokenResponse(response);
            log.info("Successfully fetched OneLogin token (expires_in={})", response.expiresIn());
            return response;

        } catch (Exception e) {
            log.error("Token fetch failed", e);
            throw new OneLoginException("Failed to fetch OneLogin token", e);
        }
    }

    private MultiValueMap<String, String> buildTokenRequestBody() {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("client_id", oneLoginProps.getClientId());
        body.add("client_secret", oneLoginProps.getClientSecret());
        return body;
    }

    private void validateTokenResponse(TokenResponse response) {
        if (response == null || response.accessToken() == null) {
            throw new OneLoginException("Invalid token response from OneLogin");
        }
    }

    @Override
    public List<OneLoginUser> getUsers(String updatedSince) {
        log.info("Fetching OneLogin users (updatedSince={})", updatedSince);
        List<OneLoginUser> result = new ArrayList<>();
        String cursor = null;

        do {
            ResponseEntity<List<OneLoginUser>> response = executePagedUserRequest(updatedSince, cursor);

            List<OneLoginUser> users = response.getBody();
            if (users != null) {
                result.addAll(users);
                log.debug("Fetched {} users in page", users.size());
            }

            cursor = extractCursor(response);

        } while (cursor != null);

        log.info("Fetched total {} users", result.size());
        return result;
    }

    private ResponseEntity<List<OneLoginUser>> executePagedUserRequest(String updatedSince, String cursor) {

        UriComponentsBuilder uri = UriComponentsBuilder
                .fromPath("/api/2/users")
                .queryParam("limit", 100);

        if (updatedSince != null) {
            uri.queryParam("updated_since", updatedSince);
        }

        if (cursor != null) {
            uri.queryParam("cursor", cursor);
        }

        try {
            return webClient
                    .get()
                    .uri(uri.toUriString())
                    .header(AUTHORIZATION, bearer())
                    .retrieve()
                    .toEntity(new ParameterizedTypeReference<List<OneLoginUser>>() {
                    })
                    .block();

        } catch (Exception e) {
            log.error("User pagination failed", e);
            throw new OneLoginException("Failed to fetch users page", e);
        }
    }

    private String extractCursor(ResponseEntity<?> response) {
        List<String> headers = response.getHeaders().get("After-Cursor");

        if (headers != null && !headers.isEmpty()) {
            String cursor = headers.getFirst();
            if (cursor != null && !cursor.isBlank()) return cursor;
        }
        return null;
    }

    private String bearer() {
        return "bearer " + getValidToken();
    }


    @Override
    public OneLoginUser getUserById(Long userId) {
        log.info("Fetching full OneLogin user details for ID: {}", userId);
        try {
            return webClient
                    .get()
                    .uri("/api/2/users/{userId}", userId)
                    .header(AUTHORIZATION, bearer())
                    .retrieve()
                    .bodyToMono(OneLoginUser.class)
                    .block();
        } catch (Exception e) {
            log.error("Failed to fetch OneLogin user details for ID {}: {}", userId, e.getMessage());
            return null;
        }
    }

    @Override
    public List<OneLoginRole> getAllRoles() {
        log.info("Fetching all OneLogin roles");
        try {
            return webClient
                    .get()
                    .uri("/api/2/roles")
                    .header(AUTHORIZATION, bearer())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<OneLoginRole>>() {
                    })
                    .block();
        } catch (Exception e) {
            log.error("Failed to fetch OneLogin roles: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public OneLoginRole getRoleById(Long roleId) {
        log.info("Fetching OneLogin role details for ID: {}", roleId);
        try {
            return webClient
                    .get()
                    .uri("/api/2/roles/{roleId}", roleId)
                    .header(AUTHORIZATION, bearer())
                    .retrieve()
                    .bodyToMono(OneLoginRole.class)
                    .block();
        } catch (Exception e) {
            log.error("Failed to fetch OneLogin role details for ID {}: {}", roleId, e.getMessage());
            return null;
        }
    }

    @Override
    public List<OneLoginEvent> getDeletionEvents(String since) {
        List<OneLoginEvent> allEvents = new ArrayList<>();
        String cursor = null;
        boolean hasMore = true;

        log.info("Fetching user deletion events from OneLogin (since={})...", since);

        while (hasMore) {
            UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/api/1/events")
                    .queryParam("event_type_id", 17)
                    .queryParam("limit", 100);

            if (since != null && !since.isEmpty()) builder.queryParam("since", since);
            if (cursor != null) builder.queryParam("cursor", cursor);

            try {
                ResponseEntity<EventResponse> response = webClient
                        .get()
                        .uri(builder.toUriString())
                        .header(AUTHORIZATION, bearer())
                        .retrieve()
                        .toEntity(EventResponse.class)
                        .block();

                if (response == null) break;

                EventResponse body = response.getBody();
                if (body != null && body.data() != null && !body.data().isEmpty()) {
                    allEvents.addAll(body.data());
                    log.debug("Fetched {} deletion events in current page", body.data().size());
                }

                List<String> afterCursor = response.getHeaders().get("After-Cursor");
                if (afterCursor != null && !afterCursor.isEmpty()
                    && afterCursor.getFirst() != null
                    && !afterCursor.getFirst().trim().isEmpty()) {
                    cursor = afterCursor.getFirst();
                } else {
                    hasMore = false;
                }

            } catch (Exception e) {
                log.error("Failed to fetch deletion events: {}", e.getMessage(), e);
                throw new OneLoginException("Error fetching OneLogin deletion events", e);
            }
        }

        log.info("Fetched {} total deletion events.", allEvents.size());
        return allEvents;
    }

}
