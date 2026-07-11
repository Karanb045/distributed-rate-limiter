package com.rateshield;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * RateShield distributed token rate limiter service.
 */
@SpringBootApplication
public class RateShieldApplication {

    public static void main(String[] args) {
        SpringApplication.run(RateShieldApplication.class, args);
    }
}