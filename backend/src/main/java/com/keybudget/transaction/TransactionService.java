package com.keybudget.transaction;

import com.keybudget.transaction.dto.CreateTransactionRequest;
import com.keybudget.transaction.dto.MonthlySummaryResponse;
import com.keybudget.transaction.dto.TransactionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.YearMonth;

/** Business operations for transactions. */
public interface TransactionService {

    /**
     * Returns a paginated list of transactions for the given user, optionally filtered by
     * date range, category, and type.
     *
     * @param userId     the authenticated user's id
     * @param start      inclusive start date filter
     * @param end        inclusive end date filter
     * @param categoryId optional category filter (null means no filter)
     * @param type       optional type filter (null means no filter)
     * @param pageable   pagination and sort parameters
     * @return page of matching transactions
     */
    Page<TransactionResponse> getTransactions(
            Long userId,
            LocalDate start,
            LocalDate end,
            Long categoryId,
            TransactionType type,
            Pageable pageable);

    /**
     * Creates and persists a new transaction for the given user.
     *
     * @param userId the authenticated user's id
     * @param req    the creation payload
     * @return the persisted transaction as a response DTO
     */
    TransactionResponse createTransaction(Long userId, CreateTransactionRequest req);

    /**
     * Builds an aggregated monthly summary of income, expenses, and per-category totals.
     *
     * @param userId the authenticated user's id
     * @param month  the calendar month to summarize
     * @return the summary DTO
     */
    MonthlySummaryResponse getMonthlySummary(Long userId, YearMonth month);
}
