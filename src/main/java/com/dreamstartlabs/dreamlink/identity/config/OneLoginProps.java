package com.dreamstartlabs.dreamlink.identity.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author Heshan Karunaratne
 */
@Configuration
@ConfigurationProperties(prefix = "onelogin")
@Getter
@Setter
public class OneLoginProps {
    private String apiUrl;
    private String clientId;
    private String clientSecret;
}
