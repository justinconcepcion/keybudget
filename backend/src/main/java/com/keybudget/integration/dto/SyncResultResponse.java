package com.keybudget.integration.dto;

import com.keybudget.integration.ProviderType;
import com.keybudget.integration.SyncStatus;

import java.time.Instant;

/**
 * Outcome of a manual or scheduled provider sync operation.
 */
public record SyncResultResponse(
    ProviderType providerType,
    Instant syncedAt,
    int accountsUpdated,
    SyncStatus status,
    String errorMessage
) {}
