package com.keybudget.transaction;

import com.keybudget.category.Category;
import com.keybudget.category.CategoryRepository;
import com.keybudget.category.CategoryType;
import com.keybudget.transaction.dto.CsvImportResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @Transactional
    public CsvImportResult importCsv(Long userId, MultipartFile file, Long defaultCategoryId) {
        List<String> errors = new ArrayList<>();
        int importedCount = 0;
        int totalRows = 0;

        Map<Long, Category> categoryMap = categoryRepository.findByUserIdOrUserIdIsNull(userId)
                .stream()
                .collect(Collectors.toMap(Category::getId, c -> c));

        if (defaultCategoryId != null && !categoryMap.containsKey(defaultCategoryId)) {
            return new CsvImportResult(0, 0, 0, List.of("Default category not found: " + defaultCategoryId));
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
            return new CsvImportResult(0, 0, 0,
                    List.of("No default categories found. Create at least one INCOME and one EXPENSE category first."));
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null) {
                return new CsvImportResult(0, 0, 0, List.of("Empty CSV file"));
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

                    Transaction tx = new Transaction();
                    tx.setUserId(userId);
                    tx.setDate(date);
                    tx.setDescription(description);

                    if (rawAmount.compareTo(BigDecimal.ZERO) < 0) {
                        tx.setType(TransactionType.EXPENSE);
                        tx.setAmount(rawAmount.abs());
                        tx.setCategoryId(expenseCategoryId);
                    } else {
                        tx.setType(TransactionType.INCOME);
                        tx.setAmount(rawAmount);
                        tx.setCategoryId(incomeCategoryId);
                    }

                    transactionRepository.save(tx);
                    importedCount++;
                } catch (Exception e) {
                    errors.add("Row " + totalRows + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            return new CsvImportResult(totalRows, importedCount, totalRows - importedCount,
                    List.of("Failed to read CSV file: " + e.getMessage()));
        }

        return new CsvImportResult(totalRows, importedCount, totalRows - importedCount, errors);
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
