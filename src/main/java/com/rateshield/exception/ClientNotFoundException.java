package com.rateshield.exception;

/**
 * Exception thrown when a client configuration is not found.
 */
public class ClientNotFoundException extends RuntimeException {

    private final String clientKey;

    public ClientNotFoundException(String clientKey) {
        super("Client configuration not found for key: " + clientKey);
        this.clientKey = clientKey;
    }

    public String getClientKey() {
        return clientKey;
    }
}