package com.dreamstartlabs.dreamlink.identity.service.onelogin;

import com.dreamstartlabs.dreamlink.identity.config.OneLoginProps;
import com.dreamstartlabs.dreamlink.identity.core.AbstractTokenManager;
import com.dreamstartlabs.dreamlink.identity.core.client.onelogin.OneLoginClient;
import com.dreamstartlabs.dreamlink.identity.old_model.OneLoginEvent;
import com.dreamstartlabs.dreamlink.identity.old_model.OneLoginRole;
import com.dreamstartlabs.dreamlink.identity.old_model.OneLoginUser;
import com.dreamstartlabs.dreamlink.identity.old_response.TokenResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
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

    public OneLoginClientService(
            @Qualifier("oneLoginWebClient") WebClient webClient,
            OneLoginProps oneLoginProps) {
        this.webClient = webClient;
        this.oneLoginProps = oneLoginProps;
    }

    @Override
    protected TokenResponse fetchToken() {
        return null;
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
