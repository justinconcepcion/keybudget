package com.keybudget.budget.dto;

import java.math.BigDecimal;

public record BudgetAlertResponse(
        Long budgetId,
        Long categoryId,
        String categoryName,
        String categoryColor,
        BigDecimal limitAmount,
        BigDecimal spentAmount,
        int percentUsed,
        AlertLevel alertLevel
) {
    public enum AlertLevel {
        WARNING,
        EXCEEDED
    }
}
