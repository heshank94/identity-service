package com.dreamstartlabs.dreamlink.identity.models.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

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
public class OneLoginEvent {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("event_type_id")
    private Integer eventTypeId;

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("created_at")
    private String createdAt;

}
