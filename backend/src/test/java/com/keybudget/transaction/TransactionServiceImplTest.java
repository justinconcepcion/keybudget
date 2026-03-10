package com.keybudget.transaction;

import com.keybudget.category.Category;
import com.keybudget.category.CategoryRepository;
import com.keybudget.category.CategoryType;
import com.keybudget.shared.ResourceNotFoundException;
import com.keybudget.transaction.dto.CreateTransactionRequest;
import com.keybudget.transaction.dto.MonthlySummaryResponse;
import com.keybudget.transaction.dto.TransactionResponse;
import com.keybudget.transaction.dto.UpdateTransactionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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
    void getMonthlySummary_givenTransferTransaction_excludedFromAllAggregations() {
        Long userId = 1L;
        YearMonth month = YearMonth.of(2026, 3);
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();

        Category expenseCat = buildCategory(5L, "Food");
        Category transferCat = buildCategory(7L, "Transfers");

        Transaction expense = buildTransaction(1L, userId, 5L, new BigDecimal("200.00"), TransactionType.EXPENSE);
        Transaction transfer = buildTransaction(3L, userId, 7L, new BigDecimal("500.00"), TransactionType.TRANSFER);

        when(categoryRepository.findByUserIdOrUserIdIsNull(userId)).thenReturn(List.of(expenseCat, transferCat));
        when(transactionRepository.findByUserIdAndDateBetween(userId, start, end))
                .thenReturn(List.of(expense, transfer));

        MonthlySummaryResponse result = transactionService.getMonthlySummary(userId, month);

        // Transfer must not contribute to income, expenses, or net savings
        assertThat(result.totalIncome()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.totalExpenses()).isEqualByComparingTo("200.00");
        assertThat(result.netSavings()).isEqualByComparingTo("-200.00");
        // Only the expense category should appear in byCategory — transfer category excluded
        assertThat(result.byCategory()).hasSize(1);
        assertThat(result.byCategory().get(0).categoryId()).isEqualTo(5L);
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
    // updateTransaction
    // -------------------------------------------------------------------------

    @Test
    void updateTransaction_givenValidRequest_updatesAndReturnsResponse() {
        Long userId = 1L;
        Long txId = 20L;
        UpdateTransactionRequest req = new UpdateTransactionRequest(
                new BigDecimal("80.00"), "Updated", LocalDate.of(2026, 3, 16),
                TransactionType.EXPENSE, 5L);

        Category cat = buildCategory(5L, "Food");
        Transaction existing = buildTransaction(txId, userId, 5L, new BigDecimal("50.00"), TransactionType.EXPENSE);

        when(transactionRepository.findByIdAndUserId(txId, userId)).thenReturn(Optional.of(existing));
        when(categoryRepository.findByUserIdOrUserIdIsNull(userId)).thenReturn(List.of(cat));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(existing);

        TransactionResponse result = transactionService.updateTransaction(userId, txId, req);

        assertThat(result.id()).isEqualTo(txId);
        assertThat(existing.getAmount()).isEqualByComparingTo("80.00");
        assertThat(existing.getDescription()).isEqualTo("Updated");
    }

    @Test
    void updateTransaction_givenNotFound_throwsResourceNotFoundException() {
        Long userId = 1L;
        UpdateTransactionRequest req = new UpdateTransactionRequest(
                new BigDecimal("50.00"), null, LocalDate.now(), TransactionType.EXPENSE, 5L);

        when(transactionRepository.findByIdAndUserId(999L, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.updateTransaction(userId, 999L, req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Transaction not found");
    }

    @Test
    void updateTransaction_givenInaccessibleCategory_throwsIllegalArgument() {
        Long userId = 1L;
        Long txId = 20L;
        UpdateTransactionRequest req = new UpdateTransactionRequest(
                new BigDecimal("50.00"), null, LocalDate.now(), TransactionType.EXPENSE, 999L);

        Transaction existing = buildTransaction(txId, userId, 5L, new BigDecimal("50.00"), TransactionType.EXPENSE);

        when(transactionRepository.findByIdAndUserId(txId, userId)).thenReturn(Optional.of(existing));
        when(categoryRepository.findByUserIdOrUserIdIsNull(userId)).thenReturn(List.of());

        assertThatThrownBy(() -> transactionService.updateTransaction(userId, txId, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Category not found or not accessible");
    }

    // -------------------------------------------------------------------------
    // deleteTransaction
    // -------------------------------------------------------------------------

    @Test
    void deleteTransaction_givenValidId_softDeletesTransaction() {
        Long userId = 1L;
        Long txId = 20L;
        Transaction existing = buildTransaction(txId, userId, 5L, new BigDecimal("50.00"), TransactionType.EXPENSE);

        when(transactionRepository.findByIdAndUserId(txId, userId)).thenReturn(Optional.of(existing));
        when(transactionRepository.save(existing)).thenReturn(existing);

        transactionService.deleteTransaction(userId, txId);

        assertThat(existing.getDeletedAt()).isNotNull();
        assertThat(existing.getDeletedAt()).isBeforeOrEqualTo(Instant.now());
        verify(transactionRepository).save(existing);
    }

    @Test
    void deleteTransaction_givenNotFound_throwsResourceNotFoundException() {
        Long userId = 1L;

        when(transactionRepository.findByIdAndUserId(999L, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.deleteTransaction(userId, 999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Transaction not found");
    }

    // -------------------------------------------------------------------------
    // exportTransactions
    // -------------------------------------------------------------------------

    @Test
    void exportTransactions_givenTransactions_writesCsvWithHeader() throws Exception {
        Long userId = 1L;
        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 3, 31);

        Category cat = buildCategory(5L, "Food");
        Transaction tx = buildTransaction(10L, userId, 5L, new BigDecimal("12.50"), TransactionType.EXPENSE);
        tx.setDescription("Lunch");

        when(categoryRepository.findByUserIdOrUserIdIsNull(userId)).thenReturn(List.of(cat));
        when(transactionRepository.findByUserIdAndDateBetweenOrderByDateAscIdAsc(userId, start, end))
                .thenReturn(List.of(tx));

        StreamingResponseBody body = transactionService.exportTransactions(userId, start, end);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        body.writeTo(out);
        String csv = out.toString(StandardCharsets.UTF_8);

        assertThat(csv).startsWith("Date,Description,Amount,Category,Type\r\n");
        assertThat(csv).contains("2026-03-15");
        assertThat(csv).contains("Lunch");
        assertThat(csv).contains("12.50");
        assertThat(csv).contains("Food");
        assertThat(csv).contains("EXPENSE");
    }

    @Test
    void exportTransactions_givenNoTransactions_writesHeaderOnly() throws Exception {
        Long userId = 1L;
        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 3, 31);

        when(categoryRepository.findByUserIdOrUserIdIsNull(userId)).thenReturn(List.of());
        when(transactionRepository.findByUserIdAndDateBetweenOrderByDateAscIdAsc(userId, start, end))
                .thenReturn(List.of());

        StreamingResponseBody body = transactionService.exportTransactions(userId, start, end);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        body.writeTo(out);
        String csv = out.toString(StandardCharsets.UTF_8);

        assertThat(csv).isEqualTo("Date,Description,Amount,Category,Type\r\n");
    }

    @Test
    void exportTransactions_givenNullDescription_writesEmptyField() throws Exception {
        Long userId = 1L;
        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 3, 31);

        Category cat = buildCategory(5L, "Food");
        Transaction tx = buildTransaction(10L, userId, 5L, new BigDecimal("5.00"), TransactionType.EXPENSE);
        // description is null by default from buildTransaction

        when(categoryRepository.findByUserIdOrUserIdIsNull(userId)).thenReturn(List.of(cat));
        when(transactionRepository.findByUserIdAndDateBetweenOrderByDateAscIdAsc(userId, start, end))
                .thenReturn(List.of(tx));

        StreamingResponseBody body = transactionService.exportTransactions(userId, start, end);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        body.writeTo(out);
        String csv = out.toString(StandardCharsets.UTF_8);

        // Null description must produce an empty field, not "null"
        assertThat(csv).contains("2026-03-15,,5.00,Food,EXPENSE");
    }

    @Test
    void exportTransactions_givenDescriptionWithComma_quotesField() throws Exception {
        Long userId = 1L;
        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 3, 31);

        Category cat = buildCategory(5L, "Food");
        Transaction tx = buildTransaction(10L, userId, 5L, new BigDecimal("8.75"), TransactionType.EXPENSE);
        tx.setDescription("Coffee, latte");

        when(categoryRepository.findByUserIdOrUserIdIsNull(userId)).thenReturn(List.of(cat));
        when(transactionRepository.findByUserIdAndDateBetweenOrderByDateAscIdAsc(userId, start, end))
                .thenReturn(List.of(tx));

        StreamingResponseBody body = transactionService.exportTransactions(userId, start, end);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        body.writeTo(out);
        String csv = out.toString(StandardCharsets.UTF_8);

        assertThat(csv).contains("\"Coffee, latte\"");
    }

    @Test
    void exportTransactions_givenDescriptionWithDoubleQuote_escapesQuotes() throws Exception {
        Long userId = 1L;
        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 3, 31);

        Category cat = buildCategory(5L, "Food");
        Transaction tx = buildTransaction(10L, userId, 5L, new BigDecimal("3.00"), TransactionType.EXPENSE);
        tx.setDescription("Say \"hello\"");

        when(categoryRepository.findByUserIdOrUserIdIsNull(userId)).thenReturn(List.of(cat));
        when(transactionRepository.findByUserIdAndDateBetweenOrderByDateAscIdAsc(userId, start, end))
                .thenReturn(List.of(tx));

        StreamingResponseBody body = transactionService.exportTransactions(userId, start, end);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        body.writeTo(out);
        String csv = out.toString(StandardCharsets.UTF_8);

        assertThat(csv).contains("\"Say \"\"hello\"\"\"");
    }

    @Test
    void exportTransactions_givenUnknownCategory_writesUnknown() throws Exception {
        Long userId = 1L;
        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 3, 31);

        Transaction tx = buildTransaction(10L, userId, 999L, new BigDecimal("20.00"), TransactionType.EXPENSE);
        tx.setDescription("Mystery");

        when(categoryRepository.findByUserIdOrUserIdIsNull(userId)).thenReturn(List.of());
        when(transactionRepository.findByUserIdAndDateBetweenOrderByDateAscIdAsc(userId, start, end))
                .thenReturn(List.of(tx));

        StreamingResponseBody body = transactionService.exportTransactions(userId, start, end);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        body.writeTo(out);
        String csv = out.toString(StandardCharsets.UTF_8);

        assertThat(csv).contains("Unknown");
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
