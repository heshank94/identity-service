package com.dreamstartlabs.dreamlink.identity.service.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author Heshan Karunaratne
 */
@Component
@Slf4j
public class SyncScheduleService {

    @Scheduled(cron = "${sync.cron}")
    public void triggerSyncService() {
        log.info("Triggering user synchronization process...");
        //TODO: Implement the logic to trigger the synchronization process
    }
}
