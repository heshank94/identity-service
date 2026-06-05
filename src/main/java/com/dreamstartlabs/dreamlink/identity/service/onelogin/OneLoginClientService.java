package com.dreamstartlabs.dreamlink.identity.service.onelogin;

import com.dreamstartlabs.dreamlink.identity.config.OneLoginProps;
import com.dreamstartlabs.dreamlink.identity.core.AbstractTokenManager;
import com.dreamstartlabs.dreamlink.identity.core.client.onelogin.OneLoginClient;
import com.dreamstartlabs.dreamlink.identity.exception.OneLoginException;
import com.dreamstartlabs.dreamlink.identity.old_model.OneLoginEvent;
import com.dreamstartlabs.dreamlink.identity.old_model.OneLoginRole;
import com.dreamstartlabs.dreamlink.identity.old_model.OneLoginUser;
import com.dreamstartlabs.dreamlink.identity.models.response.TokenResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

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

            TokenResponse response = webClient.post().uri("/auth/oauth2/v2/token").contentType(MediaType.APPLICATION_FORM_URLENCODED).bodyValue(buildTokenRequestBody()).retrieve().bodyToMono(TokenResponse.class).block();

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
        return List.of();
    }

    @Override
    public OneLoginUser getUserById(Long userId) {
        return null;
    }

    @Override
    public List<OneLoginRole> getAllRoles() {
        return List.of();
    }

    @Override
    public OneLoginRole getRoleById(Long roleId) {
        return null;
    }

    @Override
    public List<OneLoginEvent> getDeletionEvents(String since) {
        return List.of();
    }
}
