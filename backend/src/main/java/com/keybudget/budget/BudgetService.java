package com.keybudget.budget;

import com.keybudget.budget.dto.BudgetResponse;
import com.keybudget.budget.dto.CreateBudgetRequest;
import com.keybudget.budget.dto.UpdateBudgetRequest;

import java.time.YearMonth;
import java.util.List;

/** Business operations for budgets. */
public interface BudgetService {

    /**
     * Returns all budgets for the given user in the specified month, each enriched with
     * the actual amount spent so far in that category.
     *
     * @param userId the authenticated user's id
     * @param month  the target calendar month
     * @return list of budget response DTOs with live spending data
     */
    List<BudgetResponse> getBudgets(Long userId, YearMonth month);

    /**
     * Creates a new budget for the given user.
     * Throws {@link IllegalArgumentException} if a budget already exists for the same
     * user/category/month combination.
     *
     * @param userId the authenticated user's id
     * @param req    the creation payload
     * @return the persisted budget as a response DTO
     */
    BudgetResponse createBudget(Long userId, CreateBudgetRequest req);

    /**
     * Updates the spending limit of an existing budget owned by the given user.
     * Throws {@link IllegalArgumentException} if the budget is not found or does not
     * belong to the user.
     *
     * @param userId   the authenticated user's id
     * @param budgetId the budget primary key
     * @param req      the update payload
     * @return the updated budget as a response DTO
     */
    BudgetResponse updateBudget(Long userId, Long budgetId, UpdateBudgetRequest req);

    /**
     * Deletes an existing budget owned by the given user.
     * Throws {@link IllegalArgumentException} if the budget is not found or does not
     * belong to the user.
     *
     * @param userId   the authenticated user's id
     * @param budgetId the budget primary key
     */
    void deleteBudget(Long userId, Long budgetId);
}
