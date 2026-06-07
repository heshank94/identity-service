package com.dreamstartlabs.dreamlink.identity.utils;

import com.dreamstartlabs.dreamlink.identity.config.KeyCloakProps;
import com.dreamstartlabs.dreamlink.identity.models.dto.OneLoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * @author Heshan Karunaratne
 */
@Component
@RequiredArgsConstructor
public class KeyCloakUserMapperUtil {
    private final KeyCloakProps keyCloakProps;

    public Map<String, Object> toCreatePayload(OneLoginUser user, List<String> roleNames) {
        return Map.of(
                "username", nullSafe(user.getUsername()),
                "email", nullSafe(user.getEmail()),
                "firstName", nullSafe(user.getFirstName()),
                "lastName", nullSafe(user.getLastName()),
                "enabled", isActive(user),
                "emailVerified", true,
                "attributes", buildAttributes(user, roleNames)
        );
    }

    public Map<String, Object> toUpdatePayload(OneLoginUser user, List<String> roleNames) {
        return Map.of(
                "firstName", nullSafe(user.getFirstName()),
                "lastName", nullSafe(user.getLastName()),
                "email", nullSafe(user.getEmail()),
                "enabled", isActive(user),
                "emailVerified", true,
                "attributes", buildAttributes(user, roleNames)
        );
    }

    public Map<String, Object> toDisablePayload() {
        return Map.of("enabled", false);
    }

    public Map<String, String> toFederatedIdentityPayload(OneLoginUser user) {
        return Map.of(
                "identityProvider", keyCloakProps.getIdpAlias(),
                "userId", String.valueOf(user.getId()),
                "userName", nullSafe(user.getUsername())
        );
    }

    private Map<String, List<String>> buildAttributes(OneLoginUser user, List<String> roleNames) {
        return Map.of(
                "onelogin_id", List.of(String.valueOf(user.getId())),
                "onelogin_roles", roleNames,
                "department", List.of(nullSafe(user.getDepartment())),
                "title", List.of(nullSafe(user.getTitle())),
                "phone", List.of(nullSafe(user.getPhone())),
                "company", List.of(nullSafe(user.getCompany()))
        );
    }

    private boolean isActive(OneLoginUser user) {
        return user.getStatus() != null && user.getStatus() == 1;
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }
}
