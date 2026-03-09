package com.keybudget.budget;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.YearMonth;
import java.util.List;

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
}
