package com.dreamstartlabs.dreamlink.identity.old_config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "sync")
public class SyncConfig {

    private String cron;
    private String stateFile = "sync-state.json";
    private boolean deleteKeycloakUsers = false;



    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public String getStateFile() {
        return stateFile;
    }

    public void setStateFile(String stateFile) {
        this.stateFile = stateFile;
    }

    public boolean isDeleteKeycloakUsers() {
        return deleteKeycloakUsers;
    }

    public void setDeleteKeycloakUsers(boolean deleteKeycloakUsers) {
        this.deleteKeycloakUsers = deleteKeycloakUsers;
    }

}
