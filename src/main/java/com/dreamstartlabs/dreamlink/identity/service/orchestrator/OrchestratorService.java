package com.dreamstartlabs.dreamlink.identity.service.orchestrator;

import com.dreamstartlabs.dreamlink.identity.models.dto.SyncState;
import com.dreamstartlabs.dreamlink.identity.models.dto.OneLoginEvent;
import com.dreamstartlabs.dreamlink.identity.models.dto.OneLoginUser;
import com.dreamstartlabs.dreamlink.identity.service.keycloak.KeyCloakClientService;
import com.dreamstartlabs.dreamlink.identity.service.onelogin.OneLoginClientService;
import com.dreamstartlabs.dreamlink.identity.utils.RoleResolverUtil;
import com.dreamstartlabs.dreamlink.identity.utils.StateManagerUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * @author Heshan Karunaratne
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrchestratorService {

    private final OneLoginClientService oneLoginClientService;
    private final KeyCloakClientService keyCloakClientService;
    private final StateManagerUtil stateManagerUtil;
    private final RoleResolverUtil roleResolverUtil;
    private boolean isSyncRunning = false;

    public synchronized void syncAll() {
        if (isSyncRunning) {
            log.warn("Sync execution is already running. Skipping this cycle.");
            return;
        }
        isSyncRunning = true;

        try {
            roleResolverUtil.clearCache();
            SyncState state = stateManagerUtil.loadState();
            if (!state.isInitialSyncCompleted()) executeInitialSync(state);
            else executeIncrementalSync(state);

        } finally {
            isSyncRunning = false;
        }
    }

    private void executeInitialSync(SyncState state) {
        log.info("Starting initial bulk sync from OneLogin...");
        Instant syncStartTime = Instant.now();
        List<OneLoginUser> summaries = oneLoginClientService.getUsers(null);
        int created = 0, updated = 0, failed = 0;

        for (OneLoginUser summary : summaries) {
            try {
                OneLoginUser user = oneLoginClientService.getUserById(summary.getId());
                if (user == null) {
                    log.warn("Skipping user ID {} — could not fetch full details.", summary.getId());
                    continue;
                }

                List<String> roleNames = roleResolverUtil.resolveRoleNames(user);

                if (!keyCloakClientService.userExists(user)) {
                    keyCloakClientService.createUser(user, roleNames);
                    created++;
                } else {
                    keyCloakClientService.updateUser(user, roleNames);
                    updated++;
                }

            } catch (Exception e) {
                log.error("Failed to sync user ID {}: {}", summary.getId(), e.getMessage());
                failed++;
            }
        }
        log.info("Initial sync complete — created={}, updated={}, failed={}", created, updated, failed);

        state.setInitialSyncCompleted(true);
        state.setLastSyncTimestamp(syncStartTime.toString());
        stateManagerUtil.saveState(state);
    }

    private void executeIncrementalSync(SyncState state) {
        Instant syncStartTime = Instant.now();
        String lastSyncTime = state.getLastSyncTimestamp();
        log.info("Starting incremental sync since {}...", lastSyncTime);

        syncUpdatedUsers(lastSyncTime);
        syncDeletedUsers(lastSyncTime);

        state.setLastSyncTimestamp(syncStartTime.toString());
        stateManagerUtil.saveState(state);
    }

    private void syncUpdatedUsers(String lastSyncTime) {
        List<OneLoginUser> summaries = oneLoginClientService.getUsers(lastSyncTime);
        int created = 0, updated = 0, disabled = 0, failed = 0;

        for (OneLoginUser summary : summaries) {
            try {
                OneLoginUser user = oneLoginClientService.getUserById(summary.getId());
                if (user == null) {
                    log.warn("Skipping user ID {} — could not fetch full details.", summary.getId());
                    continue;
                }

                if (!isActive(user)) {
                    keyCloakClientService.disableUser(user);
                    disabled++;
                    continue;
                }

                List<String> roleNames = roleResolverUtil.resolveRoleNames(user);

                if (!keyCloakClientService.userExists(user)) {
                    keyCloakClientService.createUser(user, roleNames);
                    created++;
                } else {
                    keyCloakClientService.updateUser(user, roleNames);
                    updated++;
                }

            } catch (Exception e) {
                log.error("Failed to sync updated user ID {}: {}", summary.getId(), e.getMessage());
                failed++;
            }
        }

        log.info("Updated users sync — created={}, updated={}, disabled={}, failed={}", created, updated, disabled, failed);
    }

    private void syncDeletedUsers(String lastSyncTime) {
        List<OneLoginEvent> deletionEvents = oneLoginClientService.getDeletionEvents(lastSyncTime);
        int disabled = 0, notFound = 0, failed = 0;

        for (OneLoginEvent event : deletionEvents) {
            if (event.getUserId() == null) continue;
            try {
                boolean wasDisabled = keyCloakClientService.disableUserByOneLoginId(event.getUserId());
                if (wasDisabled) {
                    disabled++;
                    log.debug("Disabled Keycloak user for deleted OneLogin ID {}", event.getUserId());
                } else {
                    notFound++;
                    log.debug("No Keycloak user found for deleted OneLogin ID {}", event.getUserId());
                }
            } catch (Exception e) {
                log.error("Failed to disable user for OneLogin ID {}: {}", event.getUserId(), e.getMessage());
                failed++;
            }
        }

        log.info("Deleted users sync — disabled={}, notFound={}, failed={}", disabled, notFound, failed);
    }

    private boolean isActive(OneLoginUser user) {
        return user.getStatus() != null && user.getStatus() == 1;
    }

}
