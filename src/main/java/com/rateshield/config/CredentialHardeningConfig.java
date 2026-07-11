package com.rateshield.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Production-only guardrail. When the {@code prod} profile is active we must not
 * boot with the placeholder credentials that ship in the default configuration.
 * Failing fast here prevents an accidentally-exposed management plane.
 *
 * Local/non-prod profiles keep the convenience defaults and do not trigger
 * this check.
 */
@Configuration
@Profile("prod")
public class CredentialHardeningConfig {

    @Bean
    public Object credentialSecurityGuard(
            @Value("${app.security.admin.username}") String adminUsername,
            @Value("${app.security.admin.password}") String adminPassword,
            @Value("${app.security.ops.username}") String opsUsername,
            @Value("${app.security.ops.password}") String opsPassword) {

        CredentialValidator.assertNotUsingDefaultCredentials(
                adminUsername, adminPassword, opsUsername, opsPassword);

        // Returned bean is irrelevant; the validation above runs at context start.
        return new Object();
    }
}
