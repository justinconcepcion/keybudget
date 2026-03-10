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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

    private List<Category> defaultCategories() {
        return List.of(
                buildCategory(10L, "Food", CategoryType.EXPENSE),
                buildCategory(11L, "Salary", CategoryType.INCOME));
    }

    // ---------------------------------------------------------------------------
    // Existing behaviour — unchanged paths
    // ---------------------------------------------------------------------------

    @Test
    void importCsv_givenValidRows_importsAll() {
        String csv = "Date,Description,Amount\n2026-03-01,Grocery Store,-50.00\n2026-03-02,Salary,3000.00\n";
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));

        when(categoryRepository.findByUserIdOrUserIdIsNull(1L)).thenReturn(defaultCategories());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        CsvImportResult result = csvImportService.importCsv(1L, file, null);

        assertThat(result.totalRows()).isEqualTo(2);
        assertThat(result.importedCount()).isEqualTo(2);
        assertThat(result.skippedDuplicates()).isZero();
        assertThat(result.errors()).isEmpty();
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    void importCsv_givenNegativeAmount_createsExpense() {
        String csv = "Date,Description,Amount\n2026-03-01,Coffee,-5.50\n";
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));

        when(categoryRepository.findByUserIdOrUserIdIsNull(1L)).thenReturn(defaultCategories());
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

        when(categoryRepository.findByUserIdOrUserIdIsNull(1L)).thenReturn(defaultCategories());

        CsvImportResult result = csvImportService.importCsv(1L, file, null);

        assertThat(result.totalRows()).isEqualTo(1);
        assertThat(result.importedCount()).isEqualTo(0);
        assertThat(result.skippedCount()).isEqualTo(1);
        assertThat(result.skippedDuplicates()).isZero();
        assertThat(result.errors()).hasSize(1);
    }

    @Test
    void importCsv_givenEmptyFile_returnsError() {
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv",
                "".getBytes(StandardCharsets.UTF_8));

        when(categoryRepository.findByUserIdOrUserIdIsNull(1L)).thenReturn(defaultCategories());

        CsvImportResult result = csvImportService.importCsv(1L, file, null);

        assertThat(result.importedCount()).isEqualTo(0);
        assertThat(result.errors()).contains("Empty CSV file");
    }

    @Test
    void importCsv_givenDefaultCategoryId_usesIt() {
        String csv = "Date,Description,Amount\n2026-03-01,Coffee,-5.50\n";
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));

        when(categoryRepository.findByUserIdOrUserIdIsNull(1L)).thenReturn(defaultCategories());
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

        when(categoryRepository.findByUserIdOrUserIdIsNull(1L)).thenReturn(defaultCategories());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        CsvImportResult result = csvImportService.importCsv(1L, file, null);

        assertThat(result.importedCount()).isEqualTo(1);
    }

    // ---------------------------------------------------------------------------
    // Duplicate detection
    // ---------------------------------------------------------------------------

    @Test
    void importCsv_givenDuplicateRow_skipsAndCountsDuplicate() {
        // The repository reports the hash already exists — row must be skipped silently.
        String csv = "Date,Description,Amount\n2026-03-01,Coffee,-5.50\n";
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));

        when(categoryRepository.findByUserIdOrUserIdIsNull(1L)).thenReturn(defaultCategories());
        when(transactionRepository.existsByImportHash(anyString())).thenReturn(true);

        CsvImportResult result = csvImportService.importCsv(1L, file, null);

        assertThat(result.totalRows()).isEqualTo(1);
        assertThat(result.importedCount()).isZero();
        assertThat(result.skippedDuplicates()).isEqualTo(1);
        assertThat(result.skippedCount()).isZero();
        assertThat(result.errors()).isEmpty();
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void importCsv_givenMixedNewAndDuplicateRows_countsEachCorrectly() {
        // Row 1 is new; row 2 is a duplicate that already exists in the database.
        String csv = "Date,Description,Amount\n2026-03-01,Salary,3000.00\n2026-03-02,Coffee,-5.50\n";
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));

        when(categoryRepository.findByUserIdOrUserIdIsNull(1L)).thenReturn(defaultCategories());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        // First hash (Salary row) is new; second hash (Coffee row) already exists.
        when(transactionRepository.existsByImportHash(anyString()))
                .thenReturn(false)
                .thenReturn(true);

        CsvImportResult result = csvImportService.importCsv(1L, file, null);

        assertThat(result.totalRows()).isEqualTo(2);
        assertThat(result.importedCount()).isEqualTo(1);
        assertThat(result.skippedDuplicates()).isEqualTo(1);
        assertThat(result.skippedCount()).isZero();
        assertThat(result.errors()).isEmpty();
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void importCsv_givenRaceConditionOnSave_treatsConstraintViolationAsDuplicate() {
        // existsByImportHash returns false (hash not present yet), but the save throws
        // DataIntegrityViolationException because a concurrent request saved the same
        // hash between our check and our insert.
        String csv = "Date,Description,Amount\n2026-03-01,Coffee,-5.50\n";
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));

        when(categoryRepository.findByUserIdOrUserIdIsNull(1L)).thenReturn(defaultCategories());
        when(transactionRepository.existsByImportHash(anyString())).thenReturn(false);
        when(transactionRepository.save(any(Transaction.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value"));

        CsvImportResult result = csvImportService.importCsv(1L, file, null);

        assertThat(result.totalRows()).isEqualTo(1);
        assertThat(result.importedCount()).isZero();
        assertThat(result.skippedDuplicates()).isEqualTo(1);
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void importCsv_givenSameRowImportedByDifferentUsers_hashesAreDistinct() {
        // Two users importing identical CSV content must not collide.
        LocalDate date = LocalDate.of(2026, 3, 1);
        BigDecimal amount = new BigDecimal("-5.50");
        String description = "Coffee";

        String hashUser1 = csvImportService.computeImportHash(1L, date, amount, description);
        String hashUser2 = csvImportService.computeImportHash(2L, date, amount, description);

        assertThat(hashUser1).isNotEqualTo(hashUser2);
        assertThat(hashUser1).hasSize(64);
        assertThat(hashUser2).hasSize(64);
    }

    @Test
    void importCsv_givenHashStampedOnTransaction_hashIsSetBeforeSave() {
        // Verify the importHash field is populated on the entity that reaches the repository.
        String csv = "Date,Description,Amount\n2026-03-01,Coffee,-5.50\n";
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));

        when(categoryRepository.findByUserIdOrUserIdIsNull(1L)).thenReturn(defaultCategories());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        csvImportService.importCsv(1L, file, null);

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getImportHash()).isNotNull().hasSize(64);
    }
}
