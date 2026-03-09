package com.keybudget.integration.dto;

import com.keybudget.integration.AccountType;
import com.keybudget.integration.ProviderType;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response DTO representing a single financial account with its most recent balance.
 */
public record AccountResponse(
    Long id,
    Long credentialId,
    ProviderType providerType,
    AccountType accountType,
    String displayName,
    String currency,
    BigDecimal balance,
    BigDecimal balanceUsd,
    Instant asOf,
    boolean active
) {}
