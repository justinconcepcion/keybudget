package com.keybudget.budget;

import com.keybudget.budget.dto.BudgetResponse;
import com.keybudget.budget.dto.CreateBudgetRequest;
import com.keybudget.budget.dto.UpdateBudgetRequest;
import com.keybudget.category.Category;
import com.keybudget.category.CategoryRepository;
import com.keybudget.category.CategoryType;
import com.keybudget.transaction.Transaction;
import com.keybudget.transaction.TransactionRepository;
import com.keybudget.transaction.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetServiceImplTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TransactionRepository transactionRepository;

    private BudgetServiceImpl budgetService;

    @BeforeEach
    void setUp() {
        budgetService = new BudgetServiceImpl(budgetRepository, categoryRepository, transactionRepository);
    }

    // -------------------------------------------------------------------------
    // getBudgets
    // -------------------------------------------------------------------------

    @Test
    void getBudgets_givenBudgetsExist_returnsResponsesWithSpentAmount() {
        Long userId = 1L;
        YearMonth month = YearMonth.of(2026, 3);
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();

        Category cat = buildCategory(5L, "Food", "#FF9800");
        Budget budget = buildBudget(10L, userId, 5L, month, new BigDecimal("500.00"));

        when(budgetRepository.findByUserIdAndMonthYear(userId, month)).thenReturn(List.of(budget));
        when(categoryRepository.findByUserIdOrUserIdIsNull(userId)).thenReturn(List.of(cat));
        List<Object[]> spentRows = new java.util.ArrayList<>();
        spentRows.add(new Object[]{5L, new BigDecimal("200.00")});
        when(transactionRepository.sumExpensesByCategory(userId, start, end))
                .thenReturn(spentRows);

        List<BudgetResponse> result = budgetService.getBudgets(userId, month);

        assertThat(result).hasSize(1);
        BudgetResponse response = result.get(0);
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.categoryName()).isEqualTo("Food");
        assertThat(response.categoryColor()).isEqualTo("#FF9800");
        assertThat(response.spentAmount()).isEqualByComparingTo("200.00");
        assertThat(response.remainingAmount()).isEqualByComparingTo("300.00");
    }

    @Test
    void getBudgets_givenNoBudgets_returnsEmptyList() {
        Long userId = 1L;
        YearMonth month = YearMonth.of(2026, 3);

        when(budgetRepository.findByUserIdAndMonthYear(userId, month)).thenReturn(List.of());
        when(categoryRepository.findByUserIdOrUserIdIsNull(userId)).thenReturn(List.of());

        List<BudgetResponse> result = budgetService.getBudgets(userId, month);

        assertThat(result).isEmpty();
    }

    @Test
    void getBudgets_givenNoExpenseTransactions_spentAmountIsZero() {
        Long userId = 1L;
        YearMonth month = YearMonth.of(2026, 3);
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();

        Category cat = buildCategory(5L, "Food", "#FF9800");
        Budget budget = buildBudget(10L, userId, 5L, month, new BigDecimal("500.00"));

        when(budgetRepository.findByUserIdAndMonthYear(userId, month)).thenReturn(List.of(budget));
        when(categoryRepository.findByUserIdOrUserIdIsNull(userId)).thenReturn(List.of(cat));
        when(transactionRepository.sumExpensesByCategory(userId, start, end))
                .thenReturn(List.of());

        List<BudgetResponse> result = budgetService.getBudgets(userId, month);

        assertThat(result.get(0).spentAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.get(0).remainingAmount()).isEqualByComparingTo("500.00");
    }

    // -------------------------------------------------------------------------
    // createBudget
    // -------------------------------------------------------------------------

    @Test
    void createBudget_givenValidRequest_savesAndReturnsResponse() {
        Long userId = 1L;
        YearMonth month = YearMonth.of(2026, 3);
        CreateBudgetRequest req = new CreateBudgetRequest(5L, month, new BigDecimal("300.00"));

        Category cat = buildCategory(5L, "Food", "#FF9800");
        Budget saved = buildBudget(20L, userId, 5L, month, new BigDecimal("300.00"));

        when(categoryRepository.findByUserIdOrUserIdIsNull(userId)).thenReturn(List.of(cat));
        when(budgetRepository.save(any(Budget.class))).thenReturn(saved);
        when(transactionRepository.sumExpensesByCategory(eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());

        BudgetResponse result = budgetService.createBudget(userId, req);

        assertThat(result.id()).isEqualTo(20L);
        assertThat(result.limitAmount()).isEqualByComparingTo("300.00");
        assertThat(result.spentAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void createBudget_givenInaccessibleCategory_throwsIllegalArgument() {
        Long userId = 1L;
        CreateBudgetRequest req = new CreateBudgetRequest(999L, YearMonth.now(), new BigDecimal("100.00"));

        when(categoryRepository.findByUserIdOrUserIdIsNull(userId)).thenReturn(List.of());

        assertThatThrownBy(() -> budgetService.createBudget(userId, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Category not found or not accessible");
    }

    // -------------------------------------------------------------------------
    // updateBudget
    // -------------------------------------------------------------------------

    @Test
    void updateBudget_givenValidRequest_updatesLimitAmount() {
        Long userId = 1L;
        YearMonth month = YearMonth.of(2026, 3);
        UpdateBudgetRequest req = new UpdateBudgetRequest(new BigDecimal("600.00"));

        Category cat = buildCategory(5L, "Food", "#FF9800");
        Budget budget = buildBudget(10L, userId, 5L, month, new BigDecimal("500.00"));
        Budget updated = buildBudget(10L, userId, 5L, month, new BigDecimal("600.00"));

        when(budgetRepository.findById(10L)).thenReturn(Optional.of(budget));
        when(budgetRepository.save(budget)).thenReturn(updated);
        when(categoryRepository.findByUserIdOrUserIdIsNull(userId)).thenReturn(List.of(cat));
        when(transactionRepository.sumExpensesByCategory(eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());

        BudgetResponse result = budgetService.updateBudget(userId, 10L, req);

        assertThat(result.limitAmount()).isEqualByComparingTo("600.00");
    }

    @Test
    void updateBudget_givenBudgetNotFound_throwsIllegalArgument() {
        when(budgetRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> budgetService.updateBudget(1L, 99L, new UpdateBudgetRequest(new BigDecimal("100.00"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Budget not found");
    }

    @Test
    void updateBudget_givenBudgetOwnedByDifferentUser_throwsIllegalArgument() {
        Budget budget = buildBudget(10L, 99L, 5L, YearMonth.now(), new BigDecimal("200.00"));
        when(budgetRepository.findById(10L)).thenReturn(Optional.of(budget));

        assertThatThrownBy(() -> budgetService.updateBudget(1L, 10L, new UpdateBudgetRequest(new BigDecimal("300.00"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Budget not found");
    }

    // -------------------------------------------------------------------------
    // deleteBudget
    // -------------------------------------------------------------------------

    @Test
    void deleteBudget_givenValidBudget_deletesCalled() {
        Long userId = 1L;
        Budget budget = buildBudget(10L, userId, 5L, YearMonth.now(), new BigDecimal("200.00"));
        when(budgetRepository.findById(10L)).thenReturn(Optional.of(budget));

        budgetService.deleteBudget(userId, 10L);

        verify(budgetRepository).delete(budget);
    }

    @Test
    void deleteBudget_givenBudgetNotFound_throwsIllegalArgument() {
        when(budgetRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> budgetService.deleteBudget(1L, 99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Budget not found");
    }

    @Test
    void deleteBudget_givenBudgetOwnedByDifferentUser_throwsIllegalArgument() {
        Budget budget = buildBudget(10L, 99L, 5L, YearMonth.now(), new BigDecimal("200.00"));
        when(budgetRepository.findById(10L)).thenReturn(Optional.of(budget));

        assertThatThrownBy(() -> budgetService.deleteBudget(1L, 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Budget not found");

        verify(budgetRepository, never()).delete(any());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Category buildCategory(Long id, String name, String color) {
        Category c = new Category() {
            @Override
            public Long getId() { return id; }
        };
        c.setName(name);
        c.setColor(color);
        c.setType(CategoryType.EXPENSE);
        return c;
    }

    private Budget buildBudget(Long id, Long userId, Long categoryId,
                                YearMonth monthYear, BigDecimal limitAmount) {
        Budget b = new Budget() {
            @Override
            public Long getId() { return id; }
        };
        b.setUserId(userId);
        b.setCategoryId(categoryId);
        b.setMonthYear(monthYear);
        b.setLimitAmount(limitAmount);
        return b;
    }

    private Transaction buildTransaction(Long userId, Long categoryId,
                                          BigDecimal amount, TransactionType type) {
        Transaction t = new Transaction();
        t.setUserId(userId);
        t.setCategoryId(categoryId);
        t.setAmount(amount);
        t.setDate(LocalDate.of(2026, 3, 10));
        t.setType(type);
        return t;
    }
}
