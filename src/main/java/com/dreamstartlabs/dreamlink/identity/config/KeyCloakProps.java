package com.dreamstartlabs.dreamlink.identity.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author Heshan Karunaratne
 */
@Configuration
@ConfigurationProperties(prefix = "keycloak")
@Getter
@Setter
public class KeyCloakProps {
    private String apiUrl;
    private String realm;
    private String clientId;
    private String clientSecret;
    private String idpAlias;
}
