package com.keybudget.budget;

import com.keybudget.budget.dto.BudgetResponse;
import com.keybudget.budget.dto.CreateBudgetRequest;
import com.keybudget.budget.dto.UpdateBudgetRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** REST endpoints for budget management. */
@Validated
@RestController
@RequestMapping("/api/v1/budgets")
public class BudgetController {

    private static final DateTimeFormatter YEAR_MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    /**
     * GET /api/v1/budgets?month=2026-03
     * Returns all budgets for the authenticated user in the specified month.
     *
     * @param month target month in "YYYY-MM" format (defaults to current month)
     */
    @GetMapping
    public ResponseEntity<List<BudgetResponse>> getBudgets(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String month) {
        Long userId = jwt.getClaim("userId");
        YearMonth yearMonth = month != null
                ? YearMonth.parse(month, YEAR_MONTH_FORMAT)
                : YearMonth.now();
        return ResponseEntity.ok(budgetService.getBudgets(userId, yearMonth));
    }

    /**
     * POST /api/v1/budgets
     * Creates a new budget for the authenticated user.
     */
    @PostMapping
    public ResponseEntity<BudgetResponse> createBudget(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateBudgetRequest req) {
        Long userId = jwt.getClaim("userId");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(budgetService.createBudget(userId, req));
    }

    /**
     * PUT /api/v1/budgets/{id}
     * Updates the spending limit of an existing budget owned by the authenticated user.
     */
    @PutMapping("/{id}")
    public ResponseEntity<BudgetResponse> updateBudget(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody UpdateBudgetRequest req) {
        Long userId = jwt.getClaim("userId");
        return ResponseEntity.ok(budgetService.updateBudget(userId, id, req));
    }

    /**
     * DELETE /api/v1/budgets/{id}
     * Deletes a budget owned by the authenticated user.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBudget(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {
        Long userId = jwt.getClaim("userId");
        budgetService.deleteBudget(userId, id);
        return ResponseEntity.noContent().build();
    }
}
