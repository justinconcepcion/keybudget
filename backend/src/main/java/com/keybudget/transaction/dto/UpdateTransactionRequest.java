package com.keybudget.transaction.dto;

import com.keybudget.transaction.TransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateTransactionRequest(
        @NotNull @Positive BigDecimal amount,
        String description,
        @NotNull LocalDate date,
        @NotNull TransactionType type,
        @NotNull Long categoryId
) {}
