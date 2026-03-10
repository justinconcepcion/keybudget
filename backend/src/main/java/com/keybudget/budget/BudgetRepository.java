package com.keybudget.budget;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/** Data access for {@link Budget} entities. */
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    /**
     * Returns all budgets for a given user in the specified month.
     *
     * @param userId    the owning user's id
     * @param monthYear the calendar month
     * @return list of matching budgets
     */
    List<Budget> findByUserIdAndMonthYear(Long userId, YearMonth monthYear);

    /**
     * Finds a soft-deleted budget matching the unique (user, category, month) triple.
     * This bypasses the {@code @SQLRestriction} filter via a native SQL query so that
     * {@link com.keybudget.budget.BudgetServiceImpl#createBudget} can detect and un-delete
     * an existing row rather than attempting an insert that would violate the unique constraint.
     *
     * @param userId     the owning user's id
     * @param categoryId the category id
     * @param monthYear  the calendar month stored as a VARCHAR(7) in the form {@code yyyy-MM}
     * @return the soft-deleted budget, if one exists
     */
    @Query(value = "SELECT * FROM budgets " +
                   "WHERE user_id = :userId " +
                   "AND category_id = :categoryId " +
                   "AND month_year = :monthYear " +
                   "AND deleted_at IS NOT NULL",
           nativeQuery = true)
    Optional<Budget> findSoftDeletedByUserIdAndCategoryIdAndMonthYear(
            Long userId, Long categoryId, String monthYear);
}
