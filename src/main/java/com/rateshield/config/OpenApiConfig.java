package com.rateshield.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI metadata for the RateShield API (surfaced via springdoc/Swagger UI).
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI rateShieldOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("RateShield API")
                .description("Production-quality token bucket and sliding window rate limiter service")
                .version("1.0.0"));
    }
}
