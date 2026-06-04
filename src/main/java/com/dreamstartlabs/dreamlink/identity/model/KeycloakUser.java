package com.dreamstartlabs.dreamlink.identity.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * @author Heshan Karunaratne
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
public class KeycloakUser {

    private String id;
    private String username;
    private String email;
    private boolean enabled;
    private Map<String, List<String>> attributes;

    /**
     * Convenience accessor for the stored onelogin_id attribute.
     */
    public String getOneLoginId() {
        if (attributes == null) return null;
        List<String> values = attributes.get("onelogin_id");
        return (values != null && !values.isEmpty()) ? values.getFirst() : null;
    }
}