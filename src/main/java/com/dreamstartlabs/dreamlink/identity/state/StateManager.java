package com.dreamstartlabs.dreamlink.identity.state;

import com.dreamstartlabs.dreamlink.identity.config.SyncConfig;
import com.dreamstartlabs.dreamlink.identity.model.OneLoginUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class StateManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(StateManager.class);
    private final SyncConfig syncConfig;
    private final ObjectMapper objectMapper;

    public StateManager(SyncConfig syncConfig) {
        this.syncConfig = syncConfig;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Loads the sync state from the configured file.
     * Returns a new state if the file does not exist or fails to read.
     */
    public SyncState loadState() {
        File file = new File(syncConfig.getStateFile());
        if (!file.exists()) {
            LOGGER.info("Sync state file '{}' does not exist. Starting with clean state.", file.getAbsolutePath());
            SyncState defaultState = new SyncState();
            defaultState.setInitialSyncCompleted(false);
            return defaultState;
        }

        try {
            SyncState state = objectMapper.readValue(file, SyncState.class);
            LOGGER.debug("Successfully loaded sync state from '{}'. Last sync: {}", file.getAbsolutePath(), state.getLastSyncTimestamp());
            return state;
        } catch (IOException e) {
            LOGGER.error("Failed to read sync state file '{}'. Returning clean state. Error: {}", file.getAbsolutePath(), e.getMessage());
            SyncState defaultState = new SyncState();
            defaultState.setInitialSyncCompleted(false);
            return defaultState;
        }
    }

    /**
     * Saves the sync state to the configured file.
     */
    public synchronized void saveState(SyncState state) {
        File file = new File(syncConfig.getStateFile());
        try {
            // Ensure parent directories exist
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, state);
            LOGGER.debug("Successfully saved sync state to '{}'. Last sync set to: {}", file.getAbsolutePath(), state.getLastSyncTimestamp());
        } catch (IOException e) {
            LOGGER.error("Failed to write sync state file '{}'. Error: {}", file.getAbsolutePath(), e.getMessage());
        }
    }

    /**
     * Loads the synced users backup from the local JSON file.
     */
    public List<OneLoginUser> loadUsersBackup() {
        File file = new File("onelogin-users.json");
        if (!file.exists()) {
            return new ArrayList<>();
        }
        try {
            OneLoginUser[] users = objectMapper.readValue(file, OneLoginUser[].class);
            List<OneLoginUser> list = new ArrayList<>();
            if (users != null) {
                for (OneLoginUser u : users) {
                    list.add(u);
                }
            }
            return list;
        } catch (IOException e) {
            LOGGER.error("Failed to read users backup file: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

}
