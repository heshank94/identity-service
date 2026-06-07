package com.dreamstartlabs.dreamlink.identity.models.dto;

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
public class KeyCloakUser {
    private String id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private boolean enabled;
    private Map<String, List<String>> attributes;

    public String getOneLoginId() {
        if (attributes == null) return null;
        List<String> values = attributes.get("onelogin_id");
        return (values != null && !values.isEmpty()) ? values.getFirst() : null;
    }
}