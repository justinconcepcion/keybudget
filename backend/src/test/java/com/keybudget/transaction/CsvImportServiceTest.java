package com.keybudget.transaction;

import com.keybudget.category.Category;
import com.keybudget.category.CategoryRepository;
import com.keybudget.category.CategoryType;
import com.keybudget.transaction.dto.CsvImportResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CsvImportServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    private CsvImportService csvImportService;

    @BeforeEach
    void setUp() {
        csvImportService = new CsvImportService(transactionRepository, categoryRepository);
    }

    private Category buildCategory(Long id, String name, CategoryType type) {
        Category c = new Category() {
            @Override
            public Long getId() { return id; }
        };
        c.setName(name);
        c.setType(type);
        c.setUserId(1L);
        return c;
    }

    @Test
    void importCsv_givenValidRows_importsAll() {
        String csv = "Date,Description,Amount\n2026-03-01,Grocery Store,-50.00\n2026-03-02,Salary,3000.00\n";
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));

        when(categoryRepository.findByUserIdOrUserIdIsNull(1L))
                .thenReturn(List.of(
                        buildCategory(10L, "Food", CategoryType.EXPENSE),
                        buildCategory(11L, "Salary", CategoryType.INCOME)));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        CsvImportResult result = csvImportService.importCsv(1L, file, null);

        assertThat(result.totalRows()).isEqualTo(2);
        assertThat(result.importedCount()).isEqualTo(2);
        assertThat(result.errors()).isEmpty();
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    void importCsv_givenNegativeAmount_createsExpense() {
        String csv = "Date,Description,Amount\n2026-03-01,Coffee,-5.50\n";
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));

        when(categoryRepository.findByUserIdOrUserIdIsNull(1L))
                .thenReturn(List.of(
                        buildCategory(10L, "Food", CategoryType.EXPENSE),
                        buildCategory(11L, "Salary", CategoryType.INCOME)));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        csvImportService.importCsv(1L, file, null);

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        Transaction tx = captor.getValue();
        assertThat(tx.getType()).isEqualTo(TransactionType.EXPENSE);
        assertThat(tx.getAmount()).isEqualByComparingTo("5.50");
    }

    @Test
    void importCsv_givenMalformedRow_skipsAndReportsError() {
        String csv = "Date,Description,Amount\n2026-03-01,Coffee\n";
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));

        when(categoryRepository.findByUserIdOrUserIdIsNull(1L))
                .thenReturn(List.of(
                        buildCategory(10L, "Food", CategoryType.EXPENSE),
                        buildCategory(11L, "Salary", CategoryType.INCOME)));

        CsvImportResult result = csvImportService.importCsv(1L, file, null);

        assertThat(result.totalRows()).isEqualTo(1);
        assertThat(result.importedCount()).isEqualTo(0);
        assertThat(result.skippedCount()).isEqualTo(1);
        assertThat(result.errors()).hasSize(1);
    }

    @Test
    void importCsv_givenEmptyFile_returnsError() {
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv",
                "".getBytes(StandardCharsets.UTF_8));

        when(categoryRepository.findByUserIdOrUserIdIsNull(1L))
                .thenReturn(List.of(
                        buildCategory(10L, "Food", CategoryType.EXPENSE),
                        buildCategory(11L, "Salary", CategoryType.INCOME)));

        CsvImportResult result = csvImportService.importCsv(1L, file, null);

        assertThat(result.importedCount()).isEqualTo(0);
        assertThat(result.errors()).contains("Empty CSV file");
    }

    @Test
    void importCsv_givenDefaultCategoryId_usesIt() {
        String csv = "Date,Description,Amount\n2026-03-01,Coffee,-5.50\n";
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));

        when(categoryRepository.findByUserIdOrUserIdIsNull(1L))
                .thenReturn(List.of(
                        buildCategory(10L, "Food", CategoryType.EXPENSE),
                        buildCategory(11L, "Salary", CategoryType.INCOME)));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        csvImportService.importCsv(1L, file, 10L);

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getCategoryId()).isEqualTo(10L);
    }

    @Test
    void importCsv_givenAlternativeDateFormat_parses() {
        String csv = "Date,Description,Amount\n03/01/2026,Coffee,-5.50\n";
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));

        when(categoryRepository.findByUserIdOrUserIdIsNull(1L))
                .thenReturn(List.of(
                        buildCategory(10L, "Food", CategoryType.EXPENSE),
                        buildCategory(11L, "Salary", CategoryType.INCOME)));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        CsvImportResult result = csvImportService.importCsv(1L, file, null);

        assertThat(result.importedCount()).isEqualTo(1);
    }
}
