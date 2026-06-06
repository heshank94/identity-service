package com.dreamstartlabs.dreamlink.identity.models.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * @author Heshan Karunaratne
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class OneLoginUser {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("username")
    private String username;

    @JsonProperty("email")
    private String email;

    @JsonProperty("firstname")
    private String firstName;

    @JsonProperty("lastname")
    private String lastName;

    @JsonProperty("status")
    private Integer status;

    @JsonProperty("company")
    private String company;

    @JsonProperty("title")
    private String title;

    @JsonProperty("role_ids")
    private List<Long> roleIds;

    @JsonProperty("department")
    private String department;

    @JsonProperty("custom_attributes")
    private Map<String, Object> customAttributes;

    @JsonProperty("phone")
    private String phone;

    @JsonProperty("updated_at")
    private String updatedAt;

}
