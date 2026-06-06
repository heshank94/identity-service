package com.dreamstartlabs.dreamlink.identity.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author Heshan Karunaratne
 */
@Configuration
@ConfigurationProperties(prefix = "sync")
@Getter
@Setter
public class SyncConfigProps {
    private String cron;
    private String stateFile = "sync-state.json";
    private boolean deleteKeycloakUsers = false;
}
