package com.dreamstartlabs.dreamlink.identity.service.orchestrator;

import com.dreamstartlabs.dreamlink.identity.models.response.TokenResponse;
import com.dreamstartlabs.dreamlink.identity.service.onelogin.OneLoginClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author Heshan Karunaratne
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrchestratorService {

    private final OneLoginClientService oneLoginClientService;

    public void syncAll() {

        try {
            TokenResponse tokenResponse = oneLoginClientService.fetchToken();
            String oneLoginAccessToken = tokenResponse.accessToken();


        } catch (Exception e) {
            throw e;
        }
    }
}
