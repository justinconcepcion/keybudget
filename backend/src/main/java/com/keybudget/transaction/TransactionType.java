package com.keybudget.transaction;

/**
 * Classifies a transaction as INCOME, EXPENSE, or TRANSFER.
 * TRANSFER represents money moved between accounts (e.g., bank to brokerage).
 * Transfers are excluded from income/expense totals and per-category spending
 * aggregations so they do not distort budget calculations.
 */
public enum TransactionType {
    INCOME,
    EXPENSE,
    TRANSFER
}
