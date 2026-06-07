package com.dreamstartlabs.dreamlink.identity.service.orchestrator;

import com.dreamstartlabs.dreamlink.identity.models.dto.OneLoginEvent;
import com.dreamstartlabs.dreamlink.identity.models.dto.OneLoginUser;
import com.dreamstartlabs.dreamlink.identity.models.dto.SyncState;
import com.dreamstartlabs.dreamlink.identity.service.keycloak.KeyCloakClientService;
import com.dreamstartlabs.dreamlink.identity.service.onelogin.OneLoginClientService;
import com.dreamstartlabs.dreamlink.identity.utils.RoleResolverUtil;
import com.dreamstartlabs.dreamlink.identity.utils.StateManagerUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
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
        log.info("Fetched {} user summaries from OneLogin for initial sync.", summaries.size());
        int created = 0;
        int updated = 0;
        int failed = 0;
        List<Long> failedUserIds = new ArrayList<>();

        for (OneLoginUser summary : summaries) {
            try {
                OneLoginUser user = oneLoginClientService.getUserById(summary.getId());
                if (user == null) {
                    log.warn("SYNC_SKIP | type=initial | userID={} | reason=full-details-unavailable", summary.getId());
                    failedUserIds.add(summary.getId());
                    failed++;
                    continue;
                }

                List<String> roleNames = roleResolverUtil.resolveRoleNames(user);

                if (!keyCloakClientService.userExists(user)) {
                    keyCloakClientService.createUser(user, roleNames);
                    log.debug("SYNC_CREATE | type=initial | userID={} | username={} | roles={}",
                            user.getId(), user.getUsername(), roleNames);
                    created++;
                } else {
                    keyCloakClientService.updateUser(user, roleNames);
                    log.debug("SYNC_UPDATE | type=initial | userID={} | username={} | roles={}",
                            user.getId(), user.getUsername(), roleNames);
                    updated++;
                }

            } catch (Exception e) {
                log.error("SYNC_FAIL | type=initial | userID={} | username={} | reason={} | action=manual-review-required",
                        summary.getId(), summary.getUsername(), e.getMessage(), e);
                failedUserIds.add(summary.getId());
                failed++;
            }
        }
        log.info("Initial sync complete — created={}, updated={}, failed={} | duration={}ms",
                created, updated, failed,
                Instant.now().toEpochMilli() - syncStartTime.toEpochMilli());

        if (!failedUserIds.isEmpty()) {
            log.warn("Initial sync failed user IDs — review required: {}", failedUserIds);
        }

        state.setInitialSyncCompleted(true);
        state.setLastSyncTimestamp(syncStartTime.toString());
        stateManagerUtil.saveState(state);
    }

    private void executeIncrementalSync(SyncState state) {
        Instant syncStartTime = Instant.now();
        String lastSyncTime = state.getLastSyncTimestamp();
        log.info("Starting incremental sync since {}...", lastSyncTime);

        int updateFailed = syncUpdatedUsers(lastSyncTime);
        int deleteFailed = syncDeletedUsers(lastSyncTime);

        if (updateFailed > 0 || deleteFailed > 0) {
            log.warn("SYNC_PARTIAL | updateFailed={} | deleteFailed={} | action=timestamp-advanced-check-logs",
                    updateFailed, deleteFailed);
        }

        // Always advance timestamp — prevents infinite retry on bad data
        state.setLastSyncTimestamp(syncStartTime.toString());
        stateManagerUtil.saveState(state);

        log.info("Incremental sync complete | duration={}ms",
                Instant.now().toEpochMilli() - syncStartTime.toEpochMilli());
    }

    private int syncUpdatedUsers(String lastSyncTime) {
        List<OneLoginUser> summaries = oneLoginClientService.getUsers(lastSyncTime);
        log.info("Fetched {} updated user summaries from OneLogin.", summaries.size());

        int created = 0;
        int updated = 0;
        int disabled = 0;
        int failed = 0;
        List<Long> failedUserIds = new ArrayList<>();

        for (OneLoginUser summary : summaries) {
            try {
                OneLoginUser user = oneLoginClientService.getUserById(summary.getId());
                if (user == null) {
                    log.warn("SYNC_SKIP | type=incremental | userID={} | reason=full-details-unavailable", summary.getId());
                    failedUserIds.add(summary.getId());
                    failed++;
                    continue;
                }

                if (!isActive(user)) {
                    keyCloakClientService.disableUser(user);
                    log.debug("SYNC_DISABLE | type=incremental | userID={} | username={} | status={}",
                            user.getId(), user.getUsername(), user.getStatus());
                    disabled++;
                    continue;
                }

                List<String> roleNames = roleResolverUtil.resolveRoleNames(user);

                if (!keyCloakClientService.userExists(user)) {
                    keyCloakClientService.createUser(user, roleNames);
                    log.debug("SYNC_CREATE | type=incremental | userID={} | username={} | roles={}",
                            user.getId(), user.getUsername(), roleNames);
                    created++;
                } else {
                    keyCloakClientService.updateUser(user, roleNames);
                    log.debug("SYNC_UPDATE | type=incremental | userID={} | username={} | roles={}",
                            user.getId(), user.getUsername(), roleNames);
                    updated++;
                }

            } catch (Exception e) {
                log.error("SYNC_FAIL | type=incremental | userID={} | username={} | reason={} | action=manual-review-required",
                        summary.getId(), summary.getUsername(), e.getMessage(), e);
                failedUserIds.add(summary.getId());
                failed++;
            }
        }

        log.info("Updated users sync — created={}, updated={}, disabled={}, failed={}",
                created, updated, disabled, failed);

        if (!failedUserIds.isEmpty()) {
            log.warn("Incremental sync failed user IDs — review required: {}", failedUserIds);
        }

        return failed;
    }

    private int syncDeletedUsers(String lastSyncTime) {
        List<OneLoginEvent> deletionEvents = oneLoginClientService.getDeletionEvents(lastSyncTime);
        log.info("Fetched {} deletion events from OneLogin.", deletionEvents.size());

        int disabled = 0, notFound = 0, failed = 0;
        List<Long> failedUserIds = new ArrayList<>();

        for (OneLoginEvent event : deletionEvents) {
            if (event.getUserId() == null) {
                log.warn("SYNC_SKIP | type=deletion | eventID={} | reason=null-user-id", event.getId());
                continue;
            }
            try {
                boolean wasDisabled = keyCloakClientService.disableUserByOneLoginId(event.getUserId());
                if (wasDisabled) {
                    log.debug("SYNC_DISABLE | type=deletion | oneLoginUserID={} | eventID={}",
                            event.getUserId(), event.getId());
                    disabled++;
                } else {
                    log.warn("SYNC_NOT_FOUND | type=deletion | oneLoginUserID={} | eventID={} | reason=no-matching-keycloak-user",
                            event.getUserId(), event.getId());
                    notFound++;
                }
            } catch (Exception e) {
                log.error("SYNC_FAIL | type=deletion | oneLoginUserID={} | eventID={} | reason={} | action=manual-review-required",
                        event.getUserId(), event.getId(), e.getMessage(), e);
                failedUserIds.add(event.getUserId());
                failed++;
            }
        }

        log.info("Deleted users sync — disabled={}, notFound={}, failed={}", disabled, notFound, failed);

        if (!failedUserIds.isEmpty()) {
            log.warn("Deletion sync failed OneLogin user IDs — review required: {}", failedUserIds);
        }

        return failed;
    }

    private boolean isActive(OneLoginUser user) {
        return user.getStatus() != null && user.getStatus() == 1;
    }

}
