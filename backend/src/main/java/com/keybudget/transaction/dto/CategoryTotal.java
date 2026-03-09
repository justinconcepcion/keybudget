package com.keybudget.transaction.dto;

import java.math.BigDecimal;

/**
 * Aggregated spending total for a single category within a monthly summary.
 *
 * @param categoryId   the category primary key
 * @param categoryName the category display name
 * @param total        total amount spent or earned in this category for the month
 */
public record CategoryTotal(
        Long categoryId,
        String categoryName,
        BigDecimal total
) {}
