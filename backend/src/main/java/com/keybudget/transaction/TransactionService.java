package com.keybudget.transaction;

import com.keybudget.transaction.dto.CreateTransactionRequest;
import com.keybudget.transaction.dto.MonthlySummaryResponse;
import com.keybudget.transaction.dto.TransactionResponse;
import com.keybudget.transaction.dto.UpdateTransactionRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

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
     * TRANSFER transactions are excluded from all aggregations.
     *
     * @param userId the authenticated user's id
     * @param month  the calendar month to summarize
     * @return the summary DTO
     */
    MonthlySummaryResponse getMonthlySummary(Long userId, YearMonth month);

    TransactionResponse updateTransaction(Long userId, Long transactionId, UpdateTransactionRequest req);

    void deleteTransaction(Long userId, Long transactionId);

    /**
     * Produces a {@link StreamingResponseBody} that writes CSV rows for all transactions
     * belonging to the given user within the specified date range. Rows are ordered by
     * date ascending, then by id ascending for deterministic output. The first row is a
     * header line: {@code Date,Description,Amount,Category,Type}.
     *
     * <p>CSV fields are RFC 4180 compliant: any field containing a comma, double-quote,
     * or newline is wrapped in double-quotes and internal double-quotes are escaped by
     * doubling them.
     *
     * @param userId the authenticated user's id
     * @param start  inclusive start date
     * @param end    inclusive end date
     * @return a streaming body that can be passed directly to a {@code ResponseEntity}
     */
    StreamingResponseBody exportTransactions(Long userId, LocalDate start, LocalDate end);
}
