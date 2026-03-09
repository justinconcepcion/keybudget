package com.keybudget.budget.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Request payload for updating the limit amount of an existing budget.
 *
 * @param limitAmount the new spending limit — must not be null and must be positive
 */
public record UpdateBudgetRequest(
        @NotNull @Positive BigDecimal limitAmount
) {}
