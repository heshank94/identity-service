package com.dreamstartlabs.dreamlink.identity.old_state;

import java.util.ArrayList;
import java.util.List;

public class SyncState {

    private String lastSyncTimestamp;
    private boolean initialSyncCompleted;
    private List<String> roleValues = new ArrayList<>();

    public String getLastSyncTimestamp() {
        return lastSyncTimestamp;
    }

    public void setLastSyncTimestamp(String lastSyncTimestamp) {
        this.lastSyncTimestamp = lastSyncTimestamp;
    }

    public boolean isInitialSyncCompleted() {
        return initialSyncCompleted;
    }

    public void setInitialSyncCompleted(boolean initialSyncCompleted) {
        this.initialSyncCompleted = initialSyncCompleted;
    }

    public List<String> getRoleValues() {
        if (roleValues == null) {
            roleValues = new ArrayList<>();
        }
        return roleValues;
    }

    public void setRoleValues(List<String> roleValues) {
        this.roleValues = roleValues;
    }
}


