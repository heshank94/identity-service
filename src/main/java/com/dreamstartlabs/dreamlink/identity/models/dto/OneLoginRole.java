package com.dreamstartlabs.dreamlink.identity.models.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
public class OneLoginRole {
    private Long id;
    private String name;
}
