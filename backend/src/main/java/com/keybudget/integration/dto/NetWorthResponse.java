package com.keybudget.integration.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Aggregate net-worth snapshot across all of the user's active financial accounts.
 */
public record NetWorthResponse(
    BigDecimal totalNetWorthUsd,
    List<ProviderTotal> byProvider,
    List<AccountTypeTotal> byAccountType,
    Instant asOf
) {}
