package com.dreamstartlabs.dreamlink.identity.utils;

import com.dreamstartlabs.dreamlink.identity.config.SyncConfigProps;
import com.dreamstartlabs.dreamlink.identity.models.dto.SyncState;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Component
@Slf4j
public class StateManagerUtil {

    private final File stateFile;
    private final ObjectMapper objectMapper;

    public StateManagerUtil(SyncConfigProps syncConfigProps) {
        this.stateFile = new File(syncConfigProps.getStateFile());
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    public SyncState loadState() {
        if (!stateFile.exists()) {
            log.info("Sync state file '{}' not found. Starting with clean state.", stateFile.getAbsolutePath());
            return defaultState();
        }

        try {
            SyncState state = objectMapper.readValue(stateFile, SyncState.class);
            log.debug("Sync state loaded. Last sync: {}", state.getLastSyncTimestamp());
            return state;
        } catch (IOException e) {
            log.error("Failed to read sync state file '{}'. Falling back to clean state. Error: {}",
                    stateFile.getAbsolutePath(),
                    e.getMessage());
            return defaultState();
        }
    }

    private SyncState defaultState() {
        return SyncState.builder().initialSyncCompleted(false).build();
    }

    public synchronized void saveState(SyncState state) {
        try {
            ensureParentDirectoriesExist();
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(stateFile, state);
            log.debug("Sync state saved. Last sync: {}", state.getLastSyncTimestamp());
        } catch (IOException e) {
            log.error("Failed to save sync state to '{}'. Error: {}", stateFile.getAbsolutePath(), e.getMessage());
        }
    }

    private void ensureParentDirectoriesExist() {
        File parent = stateFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
    }

}
