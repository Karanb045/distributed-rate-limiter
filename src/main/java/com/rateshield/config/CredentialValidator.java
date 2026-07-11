package com.rateshield.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Checks whether the configured management credentials are still the insecure
 * defaults shipped with the project.
 */
public final class CredentialValidator {

    public static final String DEFAULT_ADMIN_USERNAME = "admin";
    public static final String DEFAULT_ADMIN_PASSWORD = "change-me-admin";
    public static final String DEFAULT_OPS_USERNAME = "ops";
    public static final String DEFAULT_OPS_PASSWORD = "change-me-ops";

    private CredentialValidator() {
    }

    /**
     * @throws IllegalStateException when either the admin or ops credentials are
     *                               still equal to their default placeholder values.
     */
    public static void assertNotUsingDefaultCredentials(String adminUsername,
                                                        String adminPassword,
                                                        String opsUsername,
                                                        String opsPassword) {
        List<String> insecure = new ArrayList<>();

        if (isDefault(adminUsername, DEFAULT_ADMIN_USERNAME) || isDefault(adminPassword, DEFAULT_ADMIN_PASSWORD)) {
            insecure.add("admin");
        }
        if (isDefault(opsUsername, DEFAULT_OPS_USERNAME) || isDefault(opsPassword, DEFAULT_OPS_PASSWORD)) {
            insecure.add("ops");
        }

        if (!insecure.isEmpty()) {
            throw new IllegalStateException(
                    "Refusing to start with default credentials for: " + insecure
                            + ". Set ADMIN_USERNAME/ADMIN_PASSWORD and OPS_USERNAME/OPS_PASSWORD "
                            + "(or app.security.*) to non-default values in the prod profile.");
        }
    }

    private static boolean isDefault(String actual, String defaultValue) {
        return actual == null || defaultValue.equals(actual);
    }
}
