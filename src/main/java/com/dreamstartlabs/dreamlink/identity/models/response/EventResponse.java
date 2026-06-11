package com.dreamstartlabs.dreamlink.identity.models.response;

import com.dreamstartlabs.dreamlink.identity.models.dto.OneLoginEvent;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Response containing a collection of OneLogin events.
 *
 * @param data list of OneLogin events returned by the API
 * @author Heshan Karunaratne
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EventResponse(List<OneLoginEvent> data) {
}
