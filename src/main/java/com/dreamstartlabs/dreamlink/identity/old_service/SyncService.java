package com.dreamstartlabs.dreamlink.identity.old_service;


import com.dreamstartlabs.dreamlink.identity.config.SyncConfigProps;
import com.dreamstartlabs.dreamlink.identity.models.dto.SyncState;
import com.dreamstartlabs.dreamlink.identity.old_client.KeycloakClient;
import com.dreamstartlabs.dreamlink.identity.old_client.OneLoginClient;
import com.dreamstartlabs.dreamlink.identity.old_model.KeycloakRole;
import com.dreamstartlabs.dreamlink.identity.old_model.KeycloakUser;
import com.dreamstartlabs.dreamlink.identity.old_model.OneLoginEvent;
import com.dreamstartlabs.dreamlink.identity.old_model.OneLoginRole;
import com.dreamstartlabs.dreamlink.identity.old_model.OneLoginUser;
import com.dreamstartlabs.dreamlink.identity.utils.StateManagerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final StateManagerUtil stateManagerUtil;
    private final SyncConfigProps syncConfig;
    private final RoleCache roleCache;

    private boolean isSyncRunning = false;

    public SyncService(OneLoginClient oneLoginClient,
                       KeycloakClient keycloakClient,
                       StateManagerUtil stateManagerUtil,
                       SyncConfigProps syncConfig,
                       RoleCache roleCache) {
        this.oneLoginClient = oneLoginClient;
        this.keycloakClient = keycloakClient;
        this.stateManagerUtil = stateManagerUtil;
        this.syncConfig = syncConfig;
        this.roleCache = roleCache;
    }


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
            SyncState state = stateManagerUtil.loadState();

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

        // Sync roles first
        syncKeycloakRoles();

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

                KeycloakUser kcUser = keycloakClient.findUser(olUser);
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

                // Sync user roles
                syncUserRoles(kcUser != null ? kcUser.getId() : null, olUser);

            } catch (Exception e) {
                LOGGER.error("Failed to sync user: ID={}, Username={}. Error: {}", summary.getId(), summary.getUsername(), e.getMessage());
            }
        }

        LOGGER.info("Initial bulk sync summary: {} users created, {} users updated.", createdCount, updatedCount);

        // Update state
        state.setInitialSyncCompleted(true);
        state.setLastSyncTimestamp(syncStartTime.toString());
        stateManagerUtil.saveState(state);
    }

    /**
     * Performs an incremental sync of user additions, modifications, and deletions.
     */
    private void executeIncrementalSync(SyncState state, Instant syncStartTime) {
        String lastSyncTime = state.getLastSyncTimestamp();
        LOGGER.info("Starting incremental sync. Fetching updates since: {}", lastSyncTime);

        // Sync roles (in case new roles were added in OneLogin)
        syncKeycloakRoles();


        Map<Long, OneLoginUser> userMap = new HashMap<>();
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

                KeycloakUser kcUser = keycloakClient.findUser(olUser);
                if (kcUser == null) {
                    keycloakClient.createUser(olUser);
                    createdCount++;
                    kcUser = keycloakClient.findUser(olUser); // Fetch the created user to get their ID
                } else {
                    boolean wasNotLinked = (kcUser.getOneLoginId() == null);
                    keycloakClient.updateUser(kcUser.getId(), olUser);
                    if (wasNotLinked) {
                        keycloakClient.linkFederatedIdentity(kcUser.getId(), olUser);
                    }
                    updatedCount++;
                }

                // Sync user roles
                if (kcUser != null) {
                    syncUserRoles(kcUser.getId(), olUser);
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

                KeycloakUser kcUser = keycloakClient.findUserByOneLoginId(event.getUserId());
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
        stateManagerUtil.saveState(state);
    }

    /**
     * Fallback to disable the user in Keycloak if deletion is disabled in configuration.
     */
    private void olUserDeactivatedFallback(KeycloakUser kcUser, Long oneLoginId) {
        LOGGER.info("Deactivating user {} in Keycloak instead of deleting (OneLogin ID: {})", kcUser.getId(), oneLoginId);
        OneLoginUser dummyUser = new OneLoginUser();
        dummyUser.setId(oneLoginId);
        dummyUser.setUsername(kcUser.getUsername());
        dummyUser.setEmail(kcUser.getEmail());
        // Status 2 is Suspended (disabled)
        dummyUser.setStatus(2);
        keycloakClient.updateUser(kcUser.getId(), dummyUser);
    }

    /**
     * Synchronizes all Keycloak roles from OneLogin role IDs.
     * This method:
     * 1. Fetches all roles from Keycloak
     * 2. For each role, updates the roleCache with mappings
     * 3. Stores role values in the state for tracking
     */
    private void syncKeycloakRoles() {
        LOGGER.info("Starting to synchronize Keycloak roles...");
        try {
            List<KeycloakRole> kcRoles = keycloakClient.getAllRoles();
            LOGGER.debug("Found {} roles in Keycloak.", kcRoles.size());

            List<String> roleValues = new ArrayList<>();

            for (KeycloakRole kcRole : kcRoles) {
                if (kcRole.getName() != null) {
                    roleValues.add(kcRole.getName());
                    // Cache the Keycloak role mapping
                    roleCache.cacheKeycloakRole(kcRole.getName(), kcRole.getId());
                    LOGGER.debug("Cached Keycloak role: name={}, id={}", kcRole.getName(), kcRole.getId());
                }
            }

            // Store role values in state for future reference
            SyncState state = stateManagerUtil.loadState();
            state.setRoleValues(roleValues);
            stateManagerUtil.saveState(state);

            LOGGER.info("Successfully synchronized {} Keycloak roles to cache.", roleValues.size());
        } catch (Exception e) {
            LOGGER.error("Failed to synchronize Keycloak roles: {}", e.getMessage(), e);
        }
    }

    /**
     * Synchronizes user roles from OneLogin to Keycloak.
     * This method:
     * 1. Fetches the user's role IDs from OneLogin
     * 2. Resolves each role ID to its name via the OneLogin API
     * 3. Ensures the role exists in Keycloak (creates if necessary)
     * 4. Assigns the role to the user in Keycloak
     *
     * @param keycloakUserId the Keycloak user ID
     * @param oneLoginUser the OneLogin user object containing roleIds
     */
    private void syncUserRoles(String keycloakUserId, OneLoginUser oneLoginUser) {
        if (keycloakUserId == null || oneLoginUser == null) {
            LOGGER.debug("Cannot sync roles: keycloakUserId={}, oneLoginUser={}", keycloakUserId, oneLoginUser);
            return;
        }

        if (oneLoginUser.getRoleIds() == null || oneLoginUser.getRoleIds().isEmpty()) {
            LOGGER.debug("User {} (OneLogin ID: {}) has no roles assigned.", keycloakUserId, oneLoginUser.getId());
            return;
        }

        LOGGER.debug("Syncing {} roles for user {} (OneLogin ID: {})",
                oneLoginUser.getRoleIds().size(), keycloakUserId, oneLoginUser.getId());

        List<String> assignedRoles = new ArrayList<>();

        for (Long roleId : oneLoginUser.getRoleIds()) {
            try {
                // Step 1: Get or fetch the role name from OneLogin
                String roleName = roleCache.getOneLoginRoleName(roleId);

                if (roleName == null) {
                    // Not in cache, fetch from OneLogin API
                    OneLoginRole olRole = oneLoginClient.getRoleById(roleId);
                    if (olRole == null) {
                        LOGGER.warn("Could not fetch OneLogin role with ID {}. Skipping.", roleId);
                        continue;
                    }
                    roleName = olRole.getName();
                    // Cache it for future use
                    roleCache.cacheOneLoginRole(roleId, roleName);
                    LOGGER.debug("Fetched and cached OneLogin role: ID={}, Name={}", roleId, roleName);
                }

                if (roleName == null || roleName.isEmpty()) {
                    LOGGER.warn("OneLogin role ID {} has no name. Skipping.", roleId);
                    continue;
                }

                // Step 2: Ensure the role exists in Keycloak
                KeycloakRole kcRole = keycloakClient.getRoleByName(roleName);
                if (kcRole == null) {
                    LOGGER.info("Role '{}' does not exist in Keycloak. Creating it...", roleName);
                    kcRole = keycloakClient.createRole(roleName);
                    if (kcRole == null) {
                        LOGGER.warn("Failed to create role '{}' in Keycloak. Skipping assignment.", roleName);
                        continue;
                    }
                    // Cache the newly created role
                    roleCache.cacheKeycloakRole(roleName, kcRole.getId());
                } else {
                    // Ensure the role is cached
                    roleCache.cacheKeycloakRole(roleName, kcRole.getId());
                }

                // Step 3: Assign the role to the user in Keycloak
                boolean assigned = keycloakClient.assignRoleToUser(keycloakUserId, roleName);
                if (assigned) {
                    assignedRoles.add(roleName);
                    LOGGER.debug("Successfully assigned role '{}' to user {}", roleName, keycloakUserId);
                } else {
                    LOGGER.warn("Failed to assign role '{}' to user {}", roleName, keycloakUserId);
                }

            } catch (Exception e) {
                LOGGER.error("Error processing role ID {} for user {}: {}", roleId, keycloakUserId, e.getMessage(), e);
            }
        }

        // Set the dreamlink_roles user attribute for token claim mapping
        if (!assignedRoles.isEmpty()) {
            boolean attributeSet = keycloakClient.setUserRolesAttribute(keycloakUserId, assignedRoles);
            if (attributeSet) {
                LOGGER.info("Successfully set dreamlink_roles attribute for user {} with {} roles",
                        keycloakUserId, assignedRoles.size());
            } else {
                LOGGER.warn("Failed to set dreamlink_roles attribute for user {}", keycloakUserId);
            }
        }

        LOGGER.debug("User {} has been assigned {} roles: {}", keycloakUserId, assignedRoles.size(), assignedRoles);
    }

}
