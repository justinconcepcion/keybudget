package com.keybudget.budget.dto;

import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * API response record for a single budget, including live spending data.
 *
 * @param id              budget primary key
 * @param categoryId      the category this budget applies to
 * @param categoryName    display name of the category
 * @param categoryColor   hex color of the category (may be null)
 * @param monthYear       the budget month
 * @param limitAmount     the configured spending limit
 * @param spentAmount     actual amount spent in this category for the month
 * @param remainingAmount limitAmount minus spentAmount (may be negative if over budget)
 */
public record BudgetResponse(
        Long id,
        Long categoryId,
        String categoryName,
        String categoryColor,
        YearMonth monthYear,
        BigDecimal limitAmount,
        BigDecimal spentAmount,
        BigDecimal remainingAmount
) {}
