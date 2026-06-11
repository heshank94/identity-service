package com.dreamstartlabs.dreamlink.identity.models.dto;

import lombok.*;


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
}
