package com.keybudget.transaction.dto;

import com.keybudget.transaction.TransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request payload for creating a new transaction.
 *
 * @param amount      monetary amount — must not be null and must be positive
 * @param description optional note
 * @param date        transaction date — must not be null
 * @param type        INCOME or EXPENSE — must not be null
 * @param categoryId  the owning category's id — must not be null
 */
public record CreateTransactionRequest(
        @NotNull @Positive BigDecimal amount,
        String description,
        @NotNull LocalDate date,
        @NotNull TransactionType type,
        @NotNull Long categoryId
) {}
