package com.keybudget.transaction.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Monthly financial summary aggregated across all categories.
 *
 * @param totalIncome    sum of all INCOME transactions for the month
 * @param totalExpenses  sum of all EXPENSE transactions for the month
 * @param netSavings     totalIncome minus totalExpenses
 * @param byCategory     per-category breakdown of totals
 */
public record MonthlySummaryResponse(
        BigDecimal totalIncome,
        BigDecimal totalExpenses,
        BigDecimal netSavings,
        List<CategoryTotal> byCategory
) {}
