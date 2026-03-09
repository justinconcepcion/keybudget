package com.keybudget.integration.exception;

import com.keybudget.integration.ProviderType;

/**
 * Base exception for all external financial provider errors.
 * Mapped to HTTP 502 Bad Gateway by {@code GlobalExceptionHandler}.
 */
public class ProviderException extends RuntimeException {

    private final ProviderType providerType;

    public ProviderException(ProviderType providerType, String message) {
        super(message);
        this.providerType = providerType;
    }

    public ProviderException(ProviderType providerType, String message, Throwable cause) {
        super(message, cause);
        this.providerType = providerType;
    }

    /** Returns the provider that raised this error. */
    public ProviderType getProviderType() {
        return providerType;
    }
}
