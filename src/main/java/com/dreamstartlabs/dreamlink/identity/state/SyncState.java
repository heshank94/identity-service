package com.dreamstartlabs.dreamlink.identity.state;

public class SyncState {

    private String lastSyncTimestamp;
    private boolean initialSyncCompleted;

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
}
