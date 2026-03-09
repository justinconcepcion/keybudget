package com.keybudget.integration.exception;

import com.keybudget.integration.ProviderType;

/**
 * Thrown when a provider responds with a rate-limit error (HTTP 429 from provider).
 * Mapped to HTTP 429 Too Many Requests by {@code GlobalExceptionHandler}.
 */
public class ProviderRateLimitException extends ProviderException {

    public ProviderRateLimitException(ProviderType providerType, String message) {
        super(providerType, message);
    }

    public ProviderRateLimitException(ProviderType providerType, String message, Throwable cause) {
        super(providerType, message, cause);
    }
}
