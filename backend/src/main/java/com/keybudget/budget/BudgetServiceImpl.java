package com.keybudget.budget;

import com.keybudget.budget.dto.BudgetResponse;
import com.keybudget.budget.dto.CreateBudgetRequest;
import com.keybudget.budget.dto.UpdateBudgetRequest;
import com.keybudget.category.Category;
import com.keybudget.category.CategoryRepository;
import com.keybudget.transaction.TransactionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Default implementation of {@link BudgetService}. */
@Service
public class BudgetServiceImpl implements BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    public BudgetServiceImpl(
            BudgetRepository budgetRepository,
            CategoryRepository categoryRepository,
            TransactionRepository transactionRepository) {
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<BudgetResponse> getBudgets(Long userId, YearMonth month) {
        List<Budget> budgets = budgetRepository.findByUserIdAndMonthYear(userId, month);
        Map<Long, Category> categoryMap = buildCategoryMap(userId);

        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();

        Map<Long, BigDecimal> spentByCategory = buildSpentMap(userId, start, end);

        return budgets.stream()
                .map(b -> toBudgetResponse(b, categoryMap, spentByCategory))
                .toList();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public BudgetResponse createBudget(Long userId, CreateBudgetRequest req) {
        // Verify the category is accessible to this user
        Map<Long, Category> categoryMap = buildCategoryMap(userId);
        if (!categoryMap.containsKey(req.categoryId())) {
            throw new IllegalArgumentException(
                    "Category not found or not accessible: " + req.categoryId());
        }

        Budget budget = new Budget();
        budget.setUserId(userId);
        budget.setCategoryId(req.categoryId());
        budget.setMonthYear(req.monthYear());
        budget.setLimitAmount(req.limitAmount());

        try {
            Budget saved = budgetRepository.save(budget);
            LocalDate start = req.monthYear().atDay(1);
            LocalDate end = req.monthYear().atEndOfMonth();
            Map<Long, BigDecimal> spentMap = buildSpentMap(userId, start, end);
            return toBudgetResponse(saved, categoryMap, spentMap);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException(
                    "A budget already exists for this category and month");
        }
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public BudgetResponse updateBudget(Long userId, Long budgetId, UpdateBudgetRequest req) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new IllegalArgumentException("Budget not found: " + budgetId));

        if (!budget.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Budget not found: " + budgetId);
        }

        budget.setLimitAmount(req.limitAmount());
        Budget saved = budgetRepository.save(budget);

        Map<Long, Category> categoryMap = buildCategoryMap(userId);
        LocalDate start = saved.getMonthYear().atDay(1);
        LocalDate end = saved.getMonthYear().atEndOfMonth();
        Map<Long, BigDecimal> spentMap = buildSpentMap(userId, start, end);
        return toBudgetResponse(saved, categoryMap, spentMap);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void deleteBudget(Long userId, Long budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new IllegalArgumentException("Budget not found: " + budgetId));

        if (!budget.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Budget not found: " + budgetId);
        }

        budgetRepository.delete(budget);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Map<Long, Category> buildCategoryMap(Long userId) {
        return categoryRepository.findByUserIdOrUserIdIsNull(userId).stream()
                .collect(Collectors.toMap(Category::getId, c -> c));
    }

    private Map<Long, BigDecimal> buildSpentMap(Long userId, LocalDate start, LocalDate end) {
        return transactionRepository.sumExpensesByCategory(userId, start, end).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (BigDecimal) row[1]
                ));
    }

    private BigDecimal computeSpent(Long userId, Long categoryId, LocalDate start, LocalDate end) {
        Map<Long, BigDecimal> spentMap = buildSpentMap(userId, start, end);
        return spentMap.getOrDefault(categoryId, BigDecimal.ZERO);
    }

    private BudgetResponse toBudgetResponse(
            Budget b,
            Map<Long, Category> categoryMap,
            Map<Long, BigDecimal> spentByCategory) {

        Category category = categoryMap.get(b.getCategoryId());
        String categoryName = category != null ? category.getName() : "Unknown";
        String categoryColor = category != null ? category.getColor() : null;

        BigDecimal spent = spentByCategory.getOrDefault(b.getCategoryId(), BigDecimal.ZERO);
        BigDecimal remaining = b.getLimitAmount().subtract(spent);

        return new BudgetResponse(
                b.getId(),
                b.getCategoryId(),
                categoryName,
                categoryColor,
                b.getMonthYear(),
                b.getLimitAmount(),
                spent,
                remaining
        );
    }
}
