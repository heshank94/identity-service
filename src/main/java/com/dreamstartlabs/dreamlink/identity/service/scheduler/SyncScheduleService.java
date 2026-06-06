package com.dreamstartlabs.dreamlink.identity.service.scheduler;

import com.dreamstartlabs.dreamlink.identity.service.orchestrator.OrchestratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

import static com.dreamstartlabs.dreamlink.identity.utils.TimeUtils.elapsedMillis;
import static com.dreamstartlabs.dreamlink.identity.utils.TimeUtils.formatTimestamp;


/**
 * @author Heshan Karunaratne
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SyncScheduleService {

    private final OrchestratorService orchestratorService;

    @Scheduled(fixedDelayString = "${sync.fixedDelay}")
    public void triggerSyncService() {
        Instant syncStartTime = Instant.now();
        log.info("SyncSchedulerService triggered at {}", formatTimestamp(syncStartTime));

        try {
            orchestratorService.syncAll(syncStartTime);
            log.info("SyncSchedulerService completed successfully in {}ms", elapsedMillis(syncStartTime));

        } catch (Exception e) {
            log.error("SyncSchedulerService failed after {}", elapsedMillis(syncStartTime), e);
        }
    }
}
