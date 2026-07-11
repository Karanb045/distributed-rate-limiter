package com.rateshield;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the {@code prod} profile hardening. Uses non-default credentials so the
 * production credential guard does not fail-fast, then checks that api-docs is
 * disabled, health details stay hidden, and log levels match the prod config.
 */
@SpringBootTest(properties = {
        "spring.profiles.active=prod",
        "app.security.admin.username=prod-admin",
        "app.security.admin.password=prod-secret-admin",
        "app.security.ops.username=prod-ops",
        "app.security.ops.password=prod-secret-ops",
        "DB_USERNAME=postgres",
        "DB_PASSWORD=admin"
})
class ProdProfileValidationTest {

    @Autowired
    Environment environment;

    @Test
    void bootsWithNonDefaultCredentials() {
        // Context already started: the prod credential guard passed.
        assertThat(environment.getActiveProfiles()).contains("prod");
    }

    @Test
    void springdocApiDocsDisabled() {
        assertThat(environment.getProperty("springdoc.api-docs.enabled")).isEqualTo("false");
    }

    @Test
    void healthShowDetailsNever() {
        assertThat(environment.getProperty("management.endpoint.health.show-details"))
                .isEqualTo("never");
    }

    @Test
    void effectiveLogLevelsMatchProdConfig() {
        assertThat(effectiveLevel("com.rateshield"))
                .as("com.rateshield inherits INFO from the base config")
                .isEqualTo(Level.INFO);
        assertThat(effectiveLevel("org.springframework.security"))
                .isEqualTo(Level.WARN);
        assertThat(effectiveLevel("org.hibernate.SQL"))
                .isEqualTo(Level.WARN);
        assertThat(effectiveLevel("org.hibernate.type.descriptor.sql.BasicBinder"))
                .isEqualTo(Level.WARN);
    }

    private static Level effectiveLevel(String loggerName) {
        return ((Logger) LoggerFactory.getLogger(loggerName)).getEffectiveLevel();
    }
}
