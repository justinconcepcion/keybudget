package com.keybudget.integration.dto;

import com.keybudget.integration.ProviderType;

import java.math.BigDecimal;

/**
 * USD total for all accounts under a single provider, used in net-worth breakdowns.
 */
public record ProviderTotal(
    ProviderType providerType,
    BigDecimal totalUsd,
    int accountCount
) {}
