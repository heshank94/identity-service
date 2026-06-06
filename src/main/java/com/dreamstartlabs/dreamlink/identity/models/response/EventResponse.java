package com.dreamstartlabs.dreamlink.identity.models.response;

import com.dreamstartlabs.dreamlink.identity.old_model.OneLoginEvent;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * @author Heshan Karunaratne
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EventResponse(List<OneLoginEvent> data) {
}