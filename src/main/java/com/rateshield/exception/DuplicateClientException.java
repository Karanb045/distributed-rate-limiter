package com.rateshield.exception;

/**
 * Exception thrown when attempting to create a client configuration whose
 * key already exists.
 */
public class DuplicateClientException extends RuntimeException {

    private final String clientKey;

    public DuplicateClientException(String clientKey) {
        super("Client configuration already exists for key: " + clientKey);
        this.clientKey = clientKey;
    }

    public String getClientKey() {
        return clientKey;
    }
}
