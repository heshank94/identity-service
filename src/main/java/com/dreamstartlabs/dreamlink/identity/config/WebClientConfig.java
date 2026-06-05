package com.dreamstartlabs.dreamlink.identity.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author Heshan Karunaratne
 */
@Configuration
@RequiredArgsConstructor
public class WebClientConfig {
    private final OneLoginProps oneLoginProps;
    private final KeyCloakProps keycloakProps;

    @Bean
    public WebClient oneLoginWebClient() {
        return WebClient.builder()
                .baseUrl(oneLoginProps.getApiUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Bean
    public WebClient keycloakWebClient() {
        return WebClient.builder()
                .baseUrl(keycloakProps.getApiUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
