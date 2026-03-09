package com.keybudget.integration.exception;

import com.keybudget.integration.ProviderType;

/**
 * Thrown when a provider rejects the stored or supplied credentials (HTTP 401 from provider).
 * Mapped to HTTP 401 Unauthorized by {@code GlobalExceptionHandler}.
 */
public class ProviderAuthException extends ProviderException {

    public ProviderAuthException(ProviderType providerType, String message) {
        super(providerType, message);
    }

    public ProviderAuthException(ProviderType providerType, String message, Throwable cause) {
        super(providerType, message, cause);
    }
}
