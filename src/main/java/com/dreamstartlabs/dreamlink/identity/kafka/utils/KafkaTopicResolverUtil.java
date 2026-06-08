package com.dreamstartlabs.dreamlink.identity.kafka.utils;

import com.dreamstartlabs.dreamlink.identity.models.dto.OneLoginUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static com.dreamstartlabs.dreamlink.identity.utils.Constants.TOPIC_PATTERN;
import static com.dreamstartlabs.dreamlink.identity.utils.CustomAttributeUtil.extract;

/**
 * @author Heshan Karunaratne
 */
@Component
@Slf4j
public class KafkaTopicResolverUtil {

    @Value("${kafka.topic.environment}")
    private String environment;

    @Value("${kafka.topic.application}")
    private String application;

    @Value("${kafka.topic.version}")
    private String version;

    public String resolve(OneLoginUser user) {
        String region = extract(user, "region");
        String tenant = extract(user, "tenant");

        if (region.isBlank() || tenant.isBlank()) {
            log.warn("Missing region or tenant for user ID {} — cannot resolve topic. region={}, tenant={}",
                    user.getId(), region, tenant);
            throw new IllegalStateException(
                    "Cannot resolve Kafka topic — region or tenant missing for user: " + user.getId());
        }

        String topic = String.format(TOPIC_PATTERN, region, tenant, environment, application, version);
        log.debug("Resolved Kafka topic: {} for user ID {}", topic, user.getId());
        return topic;
    }

}
