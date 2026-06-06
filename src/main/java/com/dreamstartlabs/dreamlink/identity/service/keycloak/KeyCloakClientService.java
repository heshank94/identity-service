package com.dreamstartlabs.dreamlink.identity.service.keycloak;

import com.dreamstartlabs.dreamlink.identity.core.AbstractTokenManager;
import com.dreamstartlabs.dreamlink.identity.models.response.TokenResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author Heshan Karunaratne
 */
@Service
@Slf4j
public class KeyCloakClientService extends AbstractTokenManager {
    @Override
    protected TokenResponse fetchToken() {
        return null;
    }
}
