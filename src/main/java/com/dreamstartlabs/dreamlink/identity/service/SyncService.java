package com.dreamstartlabs.dreamlink.identity.service;


import com.dreamstartlabs.dreamlink.identity.client.KeycloakClient;
import com.dreamstartlabs.dreamlink.identity.client.OneLoginClient;
import com.dreamstartlabs.dreamlink.identity.config.SyncConfig;
import com.dreamstartlabs.dreamlink.identity.model.OneLoginEvent;
import com.dreamstartlabs.dreamlink.identity.model.OneLoginUser;
import com.dreamstartlabs.dreamlink.identity.state.StateManager;
import com.dreamstartlabs.dreamlink.identity.state.SyncState;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SyncService.class);

    private final OneLoginClient oneLoginClient;
    private final KeycloakClient keycloakClient;
    private final StateManager stateManager;
    private final SyncConfig syncConfig;

    private boolean isSyncRunning = false;

    public SyncService(OneLoginClient oneLoginClient,
                       KeycloakClient keycloakClient,
                       StateManager stateManager,
                       SyncConfig syncConfig) {
        this.oneLoginClient = oneLoginClient;
        this.keycloakClient = keycloakClient;
        this.stateManager = stateManager;
        this.syncConfig = syncConfig;
    }

    /**
     * Optional: Run sync on startup to guarantee the initial sync runs immediately.
     */
    @PostConstruct
    public void init() {
        LOGGER.info("Application started. Triggering user synchronization immediately on startup...");
        new Thread(this::triggerSync).start();
    }

    /**
     * Scheduled synchronization execution.
     * The cron expression is resolved from properties (default: every 5 minutes).
     */
    @Scheduled(cron = "${sync.cron:0 */5 * * * *}")
    public void scheduledSync() {
        LOGGER.info("Triggering scheduled user synchronization...");
        triggerSync();
    }

    /**
     * Executes the synchronization cycle.
     */
    public synchronized void triggerSync() {
        if (isSyncRunning) {
            LOGGER.warn("Sync execution is already running. Skipping this cycle.");
            return;
        }

        isSyncRunning = true;
        Instant syncStartTime = Instant.now();
        LOGGER.info("User synchronization cycle started at {}", syncStartTime);

        try {
            SyncState state = stateManager.loadState();

            if (!state.isInitialSyncCompleted()) {
                executeInitialSync(state, syncStartTime);
            } else {
                executeIncrementalSync(state, syncStartTime);
            }

            LOGGER.info("User synchronization cycle completed successfully in {} ms.",
                    Instant.now().toEpochMilli() - syncStartTime.toEpochMilli());
        } catch (Exception e) {
            LOGGER.error("User synchronization cycle failed: {}", e.getMessage(), e);
            // We do NOT update the state file timestamp on failure, allowing the next run to retry
        } finally {
            isSyncRunning = false;
        }
    }

    /**
     * Performs a complete synchronization of all users from OneLogin to Keycloak.
     */
    private void executeInitialSync(SyncState state, Instant syncStartTime) {
        LOGGER.info("Starting initial bulk sync of all users from OneLogin...");

        // Fetch all users (no updatedSince filter) - these are summaries
        List<OneLoginUser> oneLoginSummaries = oneLoginClient.getUsers(null);
        List<OneLoginUser> fullUsers = new ArrayList<>();

        int createdCount = 0;
        int updatedCount = 0;

        for (OneLoginUser summary : oneLoginSummaries) {
            try {
                // Fetch full details (contains custom_attributes)
                OneLoginUser olUser = oneLoginClient.getUserById(summary.getId());
                if (olUser == null) {
                    LOGGER.warn("Skipping sync for user ID {} because full details could not be retrieved.", summary.getId());
                    continue;
                }
                fullUsers.add(olUser);

                LOGGER.debug("Fetched full user details: ID={}, Username={}, Email={}, Firstname={}, Lastname={}, Status={}, State={}, CustomAttributes={}",
                        olUser.getId(), olUser.getUsername(), olUser.getEmail(), olUser.getFirstName(), olUser.getLastName(),
                        olUser.getStatus(), olUser.getState(), olUser.getCustomAttributes());

                KeycloakClient.KeycloakUser kcUser = keycloakClient.findUser(olUser);
                if (kcUser == null) {
                    keycloakClient.createUser(olUser);
                    createdCount++;
                } else {
                    boolean wasNotLinked = (kcUser.getOneLoginId() == null);
                    keycloakClient.updateUser(kcUser.getId(), olUser);
                    if (wasNotLinked) {
                        keycloakClient.linkFederatedIdentity(kcUser.getId(), olUser);
                    }
                    updatedCount++;
                }
            } catch (Exception e) {
                LOGGER.error("Failed to sync user: ID={}, Username={}. Error: {}", summary.getId(), summary.getUsername(), e.getMessage());
            }
        }

        LOGGER.info("Initial bulk sync summary: {} users created, {} users updated.", createdCount, updatedCount);

        // Update state
        state.setInitialSyncCompleted(true);
        state.setLastSyncTimestamp(syncStartTime.toString());
        stateManager.saveState(state);
    }

    /**
     * Performs an incremental sync of user additions, modifications, and deletions.
     */
    private void executeIncrementalSync(SyncState state, Instant syncStartTime) {
        String lastSyncTime = state.getLastSyncTimestamp();
        LOGGER.info("Starting incremental sync. Fetching updates since: {}", lastSyncTime);

        // Load existing users backup
        List<OneLoginUser> existingUsers = stateManager.loadUsersBackup();
        Map<Long, OneLoginUser> userMap = new HashMap<>();
        if (existingUsers.isEmpty()) {
            LOGGER.warn("Local users backup file (onelogin-users.json) is empty or missing. Rebuilding backup cache from OneLogin...");
            List<OneLoginUser> allSummaries = oneLoginClient.getUsers(null);
            for (OneLoginUser summary : allSummaries) {
                try {
                    OneLoginUser olUser = oneLoginClient.getUserById(summary.getId());
                    if (olUser != null) {
                        userMap.put(olUser.getId(), olUser);
                        LOGGER.debug("Rebuilt backup cache for User: ID={}, Username={}, Email={}, Firstname={}, Lastname={}, Status={}, State={}, CustomAttributes={}",
                                olUser.getId(), olUser.getUsername(), olUser.getEmail(), olUser.getFirstName(), olUser.getLastName(),
                                olUser.getStatus(), olUser.getState(), olUser.getCustomAttributes());
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to fetch full user details for ID={} while rebuilding backup: {}", summary.getId(), e.getMessage());
                }
            }
            LOGGER.info("Successfully rebuilt backup cache with {} users.", userMap.size());
        } else {
            for (OneLoginUser u : existingUsers) {
                userMap.put(u.getId(), u);
            }
        }

        // 1. Fetch updated/new users (summaries)
        List<OneLoginUser> updatedSummaries = oneLoginClient.getUsers(lastSyncTime);
        int createdCount = 0;
        int updatedCount = 0;

        for (OneLoginUser summary : updatedSummaries) {
            try {
                // Fetch full details (contains custom_attributes)
                OneLoginUser olUser = oneLoginClient.getUserById(summary.getId());
                if (olUser == null) {
                    LOGGER.warn("Skipping sync for updated user ID {} because full details could not be retrieved.", summary.getId());
                    continue;
                }
                userMap.put(olUser.getId(), olUser); // Update in backup map

                LOGGER.debug("Fetched full user details: ID={}, Username={}, Email={}, Firstname={}, Lastname={}, Status={}, State={}, CustomAttributes={}",
                        olUser.getId(), olUser.getUsername(), olUser.getEmail(), olUser.getFirstName(), olUser.getLastName(),
                        olUser.getStatus(), olUser.getState(), olUser.getCustomAttributes());

                KeycloakClient.KeycloakUser kcUser = keycloakClient.findUser(olUser);
                if (kcUser == null) {
                    keycloakClient.createUser(olUser);
                    createdCount++;
                } else {
                    boolean wasNotLinked = (kcUser.getOneLoginId() == null);
                    keycloakClient.updateUser(kcUser.getId(), olUser);
                    if (wasNotLinked) {
                        keycloakClient.linkFederatedIdentity(kcUser.getId(), olUser);
                    }
                    updatedCount++;
                }
            } catch (Exception e) {
                LOGGER.error("Failed to sync updated user: ID={}, Username={}. Error: {}", summary.getId(), summary.getUsername(), e.getMessage());
            }
        }

        // 2. Fetch deleted users (deletions are tracked via events)
        List<OneLoginEvent> deleteEvents = oneLoginClient.getDeletionEvents(lastSyncTime);
        int deletedCount = 0;

        for (OneLoginEvent event : deleteEvents) {
            if (event.getUserId() == null) continue;
            try {
                // Remove from local backup map
                userMap.remove(event.getUserId());

                KeycloakClient.KeycloakUser kcUser = keycloakClient.findUserByOneLoginId(event.getUserId());
                if (kcUser != null) {
                    if (syncConfig.isDeleteKeycloakUsers()) {
                        keycloakClient.deleteUser(kcUser.getId());
                        deletedCount++;
                    } else {
                        // Deactivate instead of deleting
                        olUserDeactivatedFallback(kcUser, event.getUserId());
                        deletedCount++;
                    }
                } else {
                    LOGGER.debug("OneLogin user ID {} was deleted, but no matching user was found in Keycloak.", event.getUserId());
                }
            } catch (Exception e) {
                LOGGER.error("Failed to process deletion for OneLogin user ID {}. Error: {}", event.getUserId(), e.getMessage());
            }
        }

        LOGGER.info("Incremental sync summary: {} users created, {} users updated, {} users processed for deletion.",
                createdCount, updatedCount, deletedCount);

        // Update checkpoint timestamp
        state.setLastSyncTimestamp(syncStartTime.toString());
        stateManager.saveState(state);
    }

    /**
     * Fallback to disable the user in Keycloak if deletion is disabled in configuration.
     */
    private void olUserDeactivatedFallback(KeycloakClient.KeycloakUser kcUser, Long oneLoginId) {
        LOGGER.info("Deactivating user {} in Keycloak instead of deleting (OneLogin ID: {})", kcUser.getId(), oneLoginId);
        OneLoginUser dummyUser = new OneLoginUser();
        dummyUser.setId(oneLoginId);
        dummyUser.setUsername(kcUser.getUsername());
        dummyUser.setEmail(kcUser.getEmail());
        // Status 2 is Suspended (disabled)
        dummyUser.setStatus(2);
        keycloakClient.updateUser(kcUser.getId(), dummyUser);
    }
}
