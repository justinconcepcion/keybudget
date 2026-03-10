package com.keybudget.transaction;

import com.keybudget.category.Category;
import com.keybudget.category.CategoryRepository;
import com.keybudget.category.CategoryType;
import com.keybudget.transaction.dto.CsvImportResult;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Parses and persists transactions from an uploaded CSV file.
 *
 * <p>CSV format (header required): {@code Date, Description, Amount}
 * <ul>
 *   <li>Negative amounts are treated as EXPENSE; positive as INCOME.</li>
 *   <li>A SHA-256 hash is computed for each row and stored in {@code import_hash}.
 *       Rows whose hash already exists in the database are silently skipped and
 *       counted as duplicates rather than failures.</li>
 * </ul>
 */
@Service
public class CsvImportService {

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("M/d/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy")
    );

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;

    public CsvImportService(TransactionRepository transactionRepository,
                            CategoryRepository categoryRepository) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
    }

    /**
     * Imports transactions from a CSV file for the given user.
     *
     * <p>Each row is hashed before saving. If an identical hash already exists in the
     * database the row is counted as a duplicate and skipped without an error entry.
     * Parse/validation failures are captured in the {@code errors} list.
     *
     * @param userId            the authenticated user's id
     * @param file              the uploaded CSV file
     * @param defaultCategoryId optional category override; when null the first matching
     *                          EXPENSE/INCOME category is used
     * @return summary of the import operation
     */
    @Transactional
    public CsvImportResult importCsv(Long userId, MultipartFile file, Long defaultCategoryId) {
        List<String> errors = new ArrayList<>();
        int importedCount = 0;
        int skippedDuplicates = 0;
        int totalRows = 0;

        Map<Long, Category> categoryMap = categoryRepository.findByUserIdOrUserIdIsNull(userId)
                .stream()
                .collect(Collectors.toMap(Category::getId, c -> c));

        if (defaultCategoryId != null && !categoryMap.containsKey(defaultCategoryId)) {
            return new CsvImportResult(0, 0, 0, 0,
                    List.of("Default category not found: " + defaultCategoryId));
        }

        Long expenseCategoryId = defaultCategoryId;
        Long incomeCategoryId = defaultCategoryId;

        if (defaultCategoryId == null) {
            expenseCategoryId = categoryMap.values().stream()
                    .filter(c -> c.getType() == CategoryType.EXPENSE)
                    .map(Category::getId)
                    .findFirst()
                    .orElse(null);
            incomeCategoryId = categoryMap.values().stream()
                    .filter(c -> c.getType() == CategoryType.INCOME)
                    .map(Category::getId)
                    .findFirst()
                    .orElse(null);
        }

        if (expenseCategoryId == null || incomeCategoryId == null) {
            return new CsvImportResult(0, 0, 0, 0,
                    List.of("No default categories found. Create at least one INCOME and one EXPENSE category first."));
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null) {
                return new CsvImportResult(0, 0, 0, 0, List.of("Empty CSV file"));
            }

            String line;
            while ((line = reader.readLine()) != null) {
                totalRows++;
                line = line.trim();
                if (line.isEmpty()) continue;

                try {
                    String[] fields = parseCsvLine(line);
                    if (fields.length < 3) {
                        errors.add("Row " + totalRows + ": Expected at least 3 columns (Date, Description, Amount)");
                        continue;
                    }

                    LocalDate date = parseDate(fields[0].trim());
                    String description = fields[1].trim();
                    BigDecimal rawAmount = new BigDecimal(fields[2].trim().replaceAll("[\"$,]", ""));

                    String hash = computeImportHash(userId, date, rawAmount, description);

                    // Pre-flight duplicate check: avoids the round-trip to the DB constraint
                    // on every row and gives a cleaner code path for the common case.
                    if (transactionRepository.existsByImportHash(hash)) {
                        skippedDuplicates++;
                        continue;
                    }

                    Transaction tx = new Transaction();
                    tx.setUserId(userId);
                    tx.setDate(date);
                    tx.setDescription(description);
                    tx.setImportHash(hash);

                    if (rawAmount.compareTo(BigDecimal.ZERO) < 0) {
                        tx.setType(TransactionType.EXPENSE);
                        tx.setAmount(rawAmount.abs());
                        tx.setCategoryId(expenseCategoryId);
                    } else {
                        tx.setType(TransactionType.INCOME);
                        tx.setAmount(rawAmount);
                        tx.setCategoryId(incomeCategoryId);
                    }

                    try {
                        transactionRepository.save(tx);
                        importedCount++;
                    } catch (DataIntegrityViolationException e) {
                        // Race condition: another concurrent import saved the same hash between
                        // our existsByImportHash check and the save. Treat it as a duplicate.
                        skippedDuplicates++;
                    }
                } catch (Exception e) {
                    errors.add("Row " + totalRows + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            return new CsvImportResult(totalRows, importedCount,
                    totalRows - importedCount - skippedDuplicates, skippedDuplicates,
                    List.of("Failed to read CSV file: " + e.getMessage()));
        }

        return new CsvImportResult(
                totalRows,
                importedCount,
                totalRows - importedCount - skippedDuplicates,
                skippedDuplicates,
                errors);
    }

    /**
     * Computes a SHA-256 hash from the combination of userId, date, amount, and
     * normalised description. The userId is included so that two users can import
     * identical transactions without triggering a false duplicate.
     *
     * @param userId      the owning user's id
     * @param date        transaction date
     * @param amount      raw amount (may be negative)
     * @param description raw description (trimmed and lower-cased internally)
     * @return 64-character lowercase hex string
     */
    String computeImportHash(Long userId, LocalDate date, BigDecimal amount, String description) {
        String input = userId + "|" + date + "|" + amount.toPlainString() + "|"
                + description.trim().toLowerCase();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is mandated by the JVM spec — this branch is unreachable in practice.
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }

    private LocalDate parseDate(String dateStr) {
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return LocalDate.parse(dateStr, fmt);
            } catch (DateTimeParseException ignored) {
            }
        }
        throw new IllegalArgumentException("Unrecognized date format: " + dateStr);
    }

    private String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }
}
