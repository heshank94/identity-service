package com.dreamstartlabs.dreamlink.identity.service.orchestrator;

import com.dreamstartlabs.dreamlink.identity.models.dto.SyncState;
import com.dreamstartlabs.dreamlink.identity.service.onelogin.OneLoginClientService;
import com.dreamstartlabs.dreamlink.identity.utils.StateManagerUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * @author Heshan Karunaratne
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrchestratorService {

    private final OneLoginClientService oneLoginClientService;
    private final StateManagerUtil stateManagerUtil;
    private boolean isSyncRunning = false;

    public synchronized void syncAll(Instant syncStartTime) {

        try {
            if (isSyncRunning) {
                log.warn("Sync execution is already running. Skipping this cycle.");
                return;
            }

            isSyncRunning = true;
            SyncState state = stateManagerUtil.loadState();
            if (!state.isInitialSyncCompleted()) executeInitialSync(state, syncStartTime);
            else executeIncrementalSync(state, syncStartTime);

        } finally {
            isSyncRunning = false;
        }
    }

    private void executeIncrementalSync(SyncState state, Instant syncStartTime) {
    }

    private void executeInitialSync(SyncState state, Instant syncStartTime) {
    }
}
