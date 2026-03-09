package com.keybudget.transaction;

import com.keybudget.category.Category;
import com.keybudget.category.CategoryRepository;
import com.keybudget.category.CategoryType;
import com.keybudget.transaction.dto.CreateTransactionRequest;
import com.keybudget.transaction.dto.MonthlySummaryResponse;
import com.keybudget.transaction.dto.TransactionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    private TransactionServiceImpl transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionServiceImpl(transactionRepository, categoryRepository);
    }

    // -------------------------------------------------------------------------
    // getTransactions
    // -------------------------------------------------------------------------

    @Test
    void getTransactions_givenNoFilters_returnsPagedResults() {
        Long userId = 1L;
        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 3, 31);
        Pageable pageable = PageRequest.of(0, 20);

        Category cat = buildCategory(5L, "Housing");
        Transaction tx = buildTransaction(10L, userId, 5L, new BigDecimal("100.00"), TransactionType.EXPENSE);

        when(categoryRepository.findByUserIdOrUserIdIsNull(userId)).thenReturn(List.of(cat));
        when(transactionRepository.findByUserIdAndDateBetween(userId, start, end, pageable))
                .thenReturn(new PageImpl<>(List.of(tx)));

        Page<TransactionResponse> result = transactionService.getTransactions(
                userId, start, end, null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).id()).isEqualTo(10L);
        assertThat(result.getContent().get(0).categoryName()).isEqualTo("Housing");
    }

    @Test
    void getTransactions_givenCategoryFilter_usesCategoryQuery() {
        Long userId = 1L;
        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 3, 31);
        Pageable pageable = PageRequest.of(0, 20);

        Category cat = buildCategory(5L, "Housing");
        Transaction tx = buildTransaction(10L, userId, 5L, new BigDecimal("200.00"), TransactionType.EXPENSE);

        when(categoryRepository.findByUserIdOrUserIdIsNull(userId)).thenReturn(List.of(cat));
        when(transactionRepository.findByUserIdAndCategoryIdAndDateBetween(userId, 5L, start, end, pageable))
                .thenReturn(new PageImpl<>(List.of(tx)));

        Page<TransactionResponse> result = transactionService.getTransactions(
                userId, start, end, 5L, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void getTransactions_givenTypeFilter_usesTypeQuery() {
        Long userId = 1L;
        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 3, 31);
        Pageable pageable = PageRequest.of(0, 20);

        Category cat = buildCategory(5L, "Salary");
        Transaction tx = buildTransaction(11L, userId, 5L, new BigDecimal("3000.00"), TransactionType.INCOME);

        when(categoryRepository.findByUserIdOrUserIdIsNull(userId)).thenReturn(List.of(cat));
        when(transactionRepository.findByUserIdAndDateBetweenAndType(userId, start, end, TransactionType.INCOME, pageable))
                .thenReturn(new PageImpl<>(List.of(tx)));

        Page<TransactionResponse> result = transactionService.getTransactions(
                userId, start, end, null, TransactionType.INCOME, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).type()).isEqualTo(TransactionType.INCOME);
    }

    @Test
    void getTransactions_givenCategoryAndTypeFilter_usesCombinedQuery() {
        Long userId = 1L;
        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 3, 31);
        Pageable pageable = PageRequest.of(0, 20);

        Category cat = buildCategory(5L, "Food");
        Transaction tx = buildTransaction(12L, userId, 5L, new BigDecimal("50.00"), TransactionType.EXPENSE);

        when(categoryRepository.findByUserIdOrUserIdIsNull(userId)).thenReturn(List.of(cat));
        when(transactionRepository.findByUserIdAndCategoryIdAndDateBetweenAndType(
                userId, 5L, start, end, TransactionType.EXPENSE, pageable))
                .thenReturn(new PageImpl<>(List.of(tx)));

        Page<TransactionResponse> result = transactionService.getTransactions(
                userId, start, end, 5L, TransactionType.EXPENSE, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    // -------------------------------------------------------------------------
    // createTransaction
    // -------------------------------------------------------------------------

    @Test
    void createTransaction_givenValidRequest_savesAndReturnsResponse() {
        Long userId = 1L;
        CreateTransactionRequest req = new CreateTransactionRequest(
                new BigDecimal("75.00"), "Dinner", LocalDate.of(2026, 3, 15),
                TransactionType.EXPENSE, 5L);

        Category cat = buildCategory(5L, "Food");
        Transaction saved = buildTransaction(20L, userId, 5L, new BigDecimal("75.00"), TransactionType.EXPENSE);

        when(categoryRepository.findByUserIdOrUserIdIsNull(userId)).thenReturn(List.of(cat));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(saved);

        TransactionResponse result = transactionService.createTransaction(userId, req);

        assertThat(result.id()).isEqualTo(20L);
        assertThat(result.amount()).isEqualByComparingTo("75.00");
        assertThat(result.categoryName()).isEqualTo("Food");
    }

    @Test
    void createTransaction_givenInaccessibleCategory_throwsIllegalArgument() {
        Long userId = 1L;
        CreateTransactionRequest req = new CreateTransactionRequest(
                new BigDecimal("50.00"), null, LocalDate.now(), TransactionType.EXPENSE, 999L);

        when(categoryRepository.findByUserIdOrUserIdIsNull(userId)).thenReturn(List.of());

        assertThatThrownBy(() -> transactionService.createTransaction(userId, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Category not found or not accessible");
    }

    // -------------------------------------------------------------------------
    // getMonthlySummary
    // -------------------------------------------------------------------------

    @Test
    void getMonthlySummary_givenTransactions_computesTotalsCorrectly() {
        Long userId = 1L;
        YearMonth month = YearMonth.of(2026, 3);
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();

        Category expenseCat = buildCategory(5L, "Food");
        Category incomeCat = buildCategory(6L, "Salary");

        Transaction expense = buildTransaction(1L, userId, 5L, new BigDecimal("200.00"), TransactionType.EXPENSE);
        Transaction income = buildTransaction(2L, userId, 6L, new BigDecimal("3000.00"), TransactionType.INCOME);

        when(categoryRepository.findByUserIdOrUserIdIsNull(userId)).thenReturn(List.of(expenseCat, incomeCat));
        when(transactionRepository.findByUserIdAndDateBetween(userId, start, end))
                .thenReturn(List.of(expense, income));

        MonthlySummaryResponse result = transactionService.getMonthlySummary(userId, month);

        assertThat(result.totalIncome()).isEqualByComparingTo("3000.00");
        assertThat(result.totalExpenses()).isEqualByComparingTo("200.00");
        assertThat(result.netSavings()).isEqualByComparingTo("2800.00");
        assertThat(result.byCategory()).hasSize(2);
    }

    @Test
    void getMonthlySummary_givenNoTransactions_returnsZeroTotals() {
        Long userId = 1L;
        YearMonth month = YearMonth.of(2026, 3);
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();

        when(categoryRepository.findByUserIdOrUserIdIsNull(userId)).thenReturn(List.of());
        when(transactionRepository.findByUserIdAndDateBetween(userId, start, end)).thenReturn(List.of());

        MonthlySummaryResponse result = transactionService.getMonthlySummary(userId, month);

        assertThat(result.totalIncome()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.totalExpenses()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.netSavings()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.byCategory()).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Category buildCategory(Long id, String name) {
        Category c = new Category() {
            @Override
            public Long getId() { return id; }
        };
        c.setName(name);
        c.setType(CategoryType.EXPENSE);
        return c;
    }

    private Transaction buildTransaction(Long id, Long userId, Long categoryId,
                                         BigDecimal amount, TransactionType type) {
        Transaction t = new Transaction() {
            @Override
            public Long getId() { return id; }
        };
        t.setUserId(userId);
        t.setCategoryId(categoryId);
        t.setAmount(amount);
        t.setDate(LocalDate.of(2026, 3, 15));
        t.setType(type);
        return t;
    }
}
