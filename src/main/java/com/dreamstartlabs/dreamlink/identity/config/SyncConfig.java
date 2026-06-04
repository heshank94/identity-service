package com.dreamstartlabs.dreamlink.identity.config;

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



    @Configuration
    @ConfigurationProperties(prefix = "onelogin")
    public static class OneLoginProps {
        private String apiUrl;
        private String clientId;
        private String clientSecret;

        public String getApiUrl() {
            return apiUrl;
        }

        public void setApiUrl(String apiUrl) {
            this.apiUrl = apiUrl;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }
    }

    @Configuration
    @ConfigurationProperties(prefix = "keycloak")
    public static class KeycloakProps {
        private String apiUrl;
        private String realm;
        private String clientId;
        private String clientSecret;
        private String idpAlias = "onelogin";

        public String getApiUrl() {
            return apiUrl;
        }

        public void setApiUrl(String apiUrl) {
            this.apiUrl = apiUrl;
        }

        public String getRealm() {
            return realm;
        }

        public void setRealm(String realm) {
            this.realm = realm;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public String getIdpAlias() {
            return idpAlias;
        }

        public void setIdpAlias(String idpAlias) {
            this.idpAlias = idpAlias;
        }
    }
}
