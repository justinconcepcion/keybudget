package com.keybudget.budget.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * Request payload for creating a new budget entry.
 *
 * @param categoryId  the category to budget for — must not be null
 * @param monthYear   the target month — must not be null
 * @param limitAmount the spending limit — must not be null and must be positive
 */
public record CreateBudgetRequest(
        @NotNull Long categoryId,
        @NotNull YearMonth monthYear,
        @NotNull @Positive BigDecimal limitAmount
) {}
