package com.keybudget.transaction.dto;

import com.keybudget.transaction.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * API response record for a single transaction.
 *
 * @param id           transaction primary key
 * @param amount       monetary amount (never null, always positive)
 * @param description  optional user note
 * @param date         date of the transaction
 * @param type         INCOME or EXPENSE
 * @param categoryId   the category primary key
 * @param categoryName the category display name
 */
public record TransactionResponse(
        Long id,
        BigDecimal amount,
        String description,
        LocalDate date,
        TransactionType type,
        Long categoryId,
        String categoryName
) {}
