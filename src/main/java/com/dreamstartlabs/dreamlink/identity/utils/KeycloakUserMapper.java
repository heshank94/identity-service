package com.dreamstartlabs.dreamlink.identity.utils;

import com.dreamstartlabs.dreamlink.identity.model.OneLoginUser;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Heshan Karunaratne
 */
@Component
public class KeycloakUserMapper {

    /**
     * Builds the full user-creation / user-update body for the Keycloak Admin API.
     */
    public Map<String, Object> toKeycloakPayload(OneLoginUser user) {
        Map<String, Object> body = new HashMap<>();
        body.put("username", user.getUsername());
        body.put("email", user.getEmail());
        body.put("firstName", user.getFirstName());
        body.put("lastName", user.getLastName());
        body.put("enabled", isEnabled(user.getStatus()));
        body.put("emailVerified", true);
        body.put("attributes", buildAttributes(user));
        return body;
    }

    /**
     * Builds the federated-identity link body.
     */
    public Map<String, String> toFederatedIdentityPayload(OneLoginUser user, String idpAlias) {
        Map<String, String> body = new HashMap<>();
        body.put("identityProvider", idpAlias);
        body.put("userId", String.valueOf(user.getId()));
        body.put("userName", user.getUsername());
        return body;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Map<String, List<String>> buildAttributes(OneLoginUser user) {
        Map<String, List<String>> attrs = new HashMap<>();

        attrs.put("onelogin_id", Collections.singletonList(String.valueOf(user.getId())));

        if (user.getCustomAttributes() != null) {
            Object tenant = user.getCustomAttributes().get("tenant");
            if (tenant != null) {
                attrs.put("tenant", Collections.singletonList(String.valueOf(tenant)));
            }
        }

        if (user.getRoleIds() != null && !user.getRoleIds().isEmpty()) {
            attrs.put("role_ids", user.getRoleIds().stream().map(String::valueOf).toList());
        }

        return attrs;
    }

    /**
     * Status 2 = Suspended, 3 = Locked → disabled in Keycloak.
     * Null or anything else → enabled.
     */
    private boolean isEnabled(Integer status) {
        if (status == null) return true;
        return status != 2 && status != 3;
    }
}