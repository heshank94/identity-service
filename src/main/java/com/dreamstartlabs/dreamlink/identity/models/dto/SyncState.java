package com.dreamstartlabs.dreamlink.identity.models.dto;

import lombok.*;

import java.util.List;

/**
 * @author Heshan Karunaratne
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode
public class SyncState {
    private String lastSyncTimestamp;
    private boolean initialSyncCompleted;
    private List<String> roleValues;
}