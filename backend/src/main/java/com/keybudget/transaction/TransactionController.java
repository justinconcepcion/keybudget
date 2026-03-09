package com.keybudget.transaction;

import com.keybudget.transaction.dto.CreateTransactionRequest;
import com.keybudget.transaction.dto.CsvImportResult;
import com.keybudget.transaction.dto.MonthlySummaryResponse;
import com.keybudget.transaction.dto.TransactionResponse;
import com.keybudget.transaction.dto.UpdateTransactionRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/** REST endpoints for transaction management. */
@Validated
@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private static final DateTimeFormatter YEAR_MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final TransactionService transactionService;
    private final CsvImportService csvImportService;

    public TransactionController(TransactionService transactionService, CsvImportService csvImportService) {
        this.transactionService = transactionService;
        this.csvImportService = csvImportService;
    }

    /**
     * GET /api/v1/transactions
     * Returns a paginated list of transactions with optional filters.
     *
     * @param start      inclusive start date (defaults to first day of current month)
     * @param end        inclusive end date (defaults to today)
     * @param categoryId optional category filter
     * @param type       optional type filter (INCOME or EXPENSE)
     * @param page       zero-based page index (default 0)
     * @param size       page size (default 20)
     */
    @GetMapping
    public ResponseEntity<Page<TransactionResponse>> getTransactions(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Long userId = jwt.getClaim("userId");

        LocalDate effectiveStart = start != null ? start : LocalDate.now().withDayOfMonth(1);
        LocalDate effectiveEnd = end != null ? end : LocalDate.now();

        int effectiveSize = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, effectiveSize, Sort.by(Sort.Direction.DESC, "date", "id"));

        return ResponseEntity.ok(
                transactionService.getTransactions(userId, effectiveStart, effectiveEnd, categoryId, type, pageable));
    }

    /**
     * POST /api/v1/transactions
     * Creates a new transaction for the authenticated user.
     */
    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateTransactionRequest req) {
        Long userId = jwt.getClaim("userId");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.createTransaction(userId, req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponse> updateTransaction(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody UpdateTransactionRequest req) {
        Long userId = jwt.getClaim("userId");
        return ResponseEntity.ok(transactionService.updateTransaction(userId, id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {
        Long userId = jwt.getClaim("userId");
        transactionService.deleteTransaction(userId, id);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/v1/transactions/import
     * Imports transactions from a CSV file. Expected format: Date,Description,Amount
     * Negative amounts are treated as expenses, positive as income.
     *
     * @param file              the CSV file to import
     * @param defaultCategoryId optional category to assign all imports to
     */
    @PostMapping("/import")
    public ResponseEntity<CsvImportResult> importCsv(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "categoryId", required = false) Long defaultCategoryId) {
        Long userId = jwt.getClaim("userId");
        return ResponseEntity.ok(csvImportService.importCsv(userId, file, defaultCategoryId));
    }

    /**
     * GET /api/v1/transactions/summary?month=2026-03
     * Returns an aggregated monthly summary for the authenticated user.
     *
     * @param month target month in "YYYY-MM" format (defaults to current month)
     */
    @GetMapping("/summary")
    public ResponseEntity<MonthlySummaryResponse> getMonthlySummary(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String month) {

        Long userId = jwt.getClaim("userId");
        YearMonth yearMonth = month != null
                ? YearMonth.parse(month, YEAR_MONTH_FORMAT)
                : YearMonth.now();

        return ResponseEntity.ok(transactionService.getMonthlySummary(userId, yearMonth));
    }
}
