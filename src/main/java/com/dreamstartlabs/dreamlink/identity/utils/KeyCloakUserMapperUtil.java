package com.dreamstartlabs.dreamlink.identity.utils;

import com.dreamstartlabs.dreamlink.identity.config.KeyCloakProps;
import com.dreamstartlabs.dreamlink.identity.models.dto.OneLoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
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
        Map<String, Object> payload = buildBasePayload(user, roleNames);
        payload.put("username", nullSafe(user.getUsername())); // username only on create
        return payload;
    }

    public Map<String, Object> toUpdatePayload(OneLoginUser user, List<String> roleNames) {
        return buildBasePayload(user, roleNames);
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

    private Map<String, Object> buildBasePayload(OneLoginUser user, List<String> roleNames) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("email", nullSafe(user.getEmail()));
        payload.put("firstName", nullSafe(user.getFirstName()));
        payload.put("lastName", nullSafe(user.getLastName()));
        payload.put("enabled", isActive(user));
        payload.put("emailVerified", true);
        payload.put("attributes", buildAttributes(user, roleNames));
        return payload;
    }

    private Map<String, List<String>> buildAttributes(OneLoginUser user, List<String> roleNames) {
        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put("onelogin_id", List.of(String.valueOf(user.getId())));
        attributes.put("dreamlink_roles", roleNames != null ? roleNames : List.of());
        attributes.put("department", List.of(nullSafe(user.getDepartment())));
        attributes.put("title", List.of(nullSafe(user.getTitle())));
        attributes.put("phone", List.of(nullSafe(user.getPhone())));
        attributes.put("company", List.of(nullSafe(user.getCompany())));
        attributes.put("dreamlink_tenant", List.of(extractCustomAttribute(user, "tenant")));
        attributes.put("region", List.of(extractCustomAttribute(user, "region")));
        return attributes;
    }

    private boolean isActive(OneLoginUser user) {
        return user.getStatus() != null && user.getStatus() == 1;
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }

    private String extractCustomAttribute(OneLoginUser user, String key) {
        if (user.getCustomAttributes() == null) return "";
        Object value = user.getCustomAttributes().get(key);
        return value != null ? value.toString() : "";
    }
}
