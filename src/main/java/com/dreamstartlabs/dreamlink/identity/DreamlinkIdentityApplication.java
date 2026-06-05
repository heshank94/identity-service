package com.dreamstartlabs.dreamlink.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DreamlinkIdentityApplication {
    public static void main(String[] args) {
        SpringApplication.run(DreamlinkIdentityApplication.class, args);
    }
}
