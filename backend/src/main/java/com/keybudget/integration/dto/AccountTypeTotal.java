package com.keybudget.integration.dto;

import com.keybudget.integration.AccountType;

import java.math.BigDecimal;

/**
 * USD total for all accounts of a given account type, used in net-worth breakdowns.
 */
public record AccountTypeTotal(
    AccountType accountType,
    BigDecimal totalUsd,
    int accountCount
) {}
