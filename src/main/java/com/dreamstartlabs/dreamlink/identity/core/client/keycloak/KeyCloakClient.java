package com.dreamstartlabs.dreamlink.identity.core.client.keycloak;

import com.dreamstartlabs.dreamlink.identity.models.dto.OneLoginUser;
import com.dreamstartlabs.dreamlink.identity.models.dto.KeyCloakUser;

import java.util.List;

/**
 * @author Heshan Karunaratne
 */
public interface KeyCloakClient {

    void authenticate();

    boolean userExists(OneLoginUser oneLoginUser);
    KeyCloakUser findUserByOneLoginId(Long oneLoginId);

    void createUser(OneLoginUser oneLoginUser, List<String> roleNames);
    void updateUser(OneLoginUser oneLoginUser, List<String> roleNames);
    void disableUser(OneLoginUser oneLoginUser);
    boolean disableUserByOneLoginId(Long oneLoginId);

    void linkFederatedIdentity(String keycloakUserId, OneLoginUser oneLoginUser);
}
