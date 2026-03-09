package com.keybudget.integration.dto;

import com.keybudget.integration.ProviderType;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * Request payload for connecting a new external financial provider.
 * The {@code credentials} map contains provider-specific key/value pairs
 * (e.g., {@code apiKey}, {@code apiSecret} for Coinbase).
 */
public record ConnectAccountRequest(

    @NotNull(message = "providerType is required")
    ProviderType providerType,

    @NotNull(message = "credentials map is required")
    Map<String, String> credentials
) {}
