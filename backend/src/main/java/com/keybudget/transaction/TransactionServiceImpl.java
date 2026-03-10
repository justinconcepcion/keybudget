package com.keybudget.transaction;

import com.keybudget.category.Category;
import com.keybudget.category.CategoryRepository;
import com.keybudget.shared.ResourceNotFoundException;
import com.keybudget.transaction.dto.CategoryTotal;
import com.keybudget.transaction.dto.CreateTransactionRequest;
import com.keybudget.transaction.dto.MonthlySummaryResponse;
import com.keybudget.transaction.dto.TransactionResponse;
import com.keybudget.transaction.dto.UpdateTransactionRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Default implementation of {@link TransactionService}. */
@Service
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;

    public TransactionServiceImpl(
            TransactionRepository transactionRepository,
            CategoryRepository categoryRepository) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactions(
            Long userId, LocalDate start, LocalDate end,
            Long categoryId, TransactionType type, Pageable pageable) {

        Map<Long, String> categoryNames = buildCategoryNameMap(userId);
        Page<Transaction> page;

        if (categoryId != null && type != null) {
            page = transactionRepository.findByUserIdAndCategoryIdAndDateBetweenAndType(
                    userId, categoryId, start, end, type, pageable);
        } else if (categoryId != null) {
            page = transactionRepository.findByUserIdAndCategoryIdAndDateBetween(
                    userId, categoryId, start, end, pageable);
        } else if (type != null) {
            page = transactionRepository.findByUserIdAndDateBetweenAndType(
                    userId, start, end, type, pageable);
        } else {
            page = transactionRepository.findByUserIdAndDateBetween(userId, start, end, pageable);
        }

        return page.map(t -> toResponse(t, categoryNames));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public TransactionResponse createTransaction(Long userId, CreateTransactionRequest req) {
        categoryRepository.findByUserIdOrUserIdIsNull(userId).stream()
                .filter(c -> c.getId().equals(req.categoryId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Category not found or not accessible: " + req.categoryId()));

        Transaction tx = new Transaction();
        tx.setUserId(userId);
        tx.setCategoryId(req.categoryId());
        tx.setAmount(req.amount());
        tx.setDescription(req.description());
        tx.setDate(req.date());
        tx.setType(req.type());

        Transaction saved = transactionRepository.save(tx);
        Map<Long, String> categoryNames = buildCategoryNameMap(userId);
        return toResponse(saved, categoryNames);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public MonthlySummaryResponse getMonthlySummary(Long userId, YearMonth month) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();

        List<Transaction> all = transactionRepository.findByUserIdAndDateBetween(userId, start, end);
        Map<Long, String> categoryNames = buildCategoryNameMap(userId);

        BigDecimal totalIncome = all.stream()
                .filter(t -> t.getType() == TransactionType.INCOME)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpenses = all.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netSavings = totalIncome.subtract(totalExpenses);

        Map<Long, BigDecimal> totalsMap = all.stream()
                .filter(t -> t.getType() == TransactionType.INCOME
                        || t.getType() == TransactionType.EXPENSE)
                .collect(Collectors.groupingBy(
                        Transaction::getCategoryId,
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ));

        List<CategoryTotal> byCategory = totalsMap.entrySet().stream()
                .map(e -> new CategoryTotal(
                        e.getKey(),
                        categoryNames.getOrDefault(e.getKey(), "Unknown"),
                        e.getValue()))
                .toList();

        return new MonthlySummaryResponse(totalIncome, totalExpenses, netSavings, byCategory);
    }

    @Override
    @Transactional
    public TransactionResponse updateTransaction(Long userId, Long transactionId, UpdateTransactionRequest req) {
        Transaction tx = transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + transactionId));

        categoryRepository.findByUserIdOrUserIdIsNull(userId).stream()
                .filter(c -> c.getId().equals(req.categoryId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Category not found or not accessible: " + req.categoryId()));

        tx.setAmount(req.amount());
        tx.setDescription(req.description());
        tx.setDate(req.date());
        tx.setType(req.type());
        tx.setCategoryId(req.categoryId());

        Transaction saved = transactionRepository.save(tx);
        Map<Long, String> categoryNames = buildCategoryNameMap(userId);
        return toResponse(saved, categoryNames);
    }

    @Override
    @Transactional
    public void deleteTransaction(Long userId, Long transactionId) {
        Transaction tx = transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + transactionId));
        tx.setDeletedAt(Instant.now());
        transactionRepository.save(tx);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public StreamingResponseBody exportTransactions(Long userId, LocalDate start, LocalDate end) {
        List<Transaction> transactions =
                transactionRepository.findByUserIdAndDateBetweenOrderByDateAscIdAsc(userId, start, end);
        Map<Long, String> categoryNames = buildCategoryNameMap(userId);

        return outputStream -> {
            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
                // RFC 4180 requires CRLF line endings
                writer.write("Date,Description,Amount,Category,Type\r\n");
                for (Transaction t : transactions) {
                    writer.write(escapeCsvField(t.getDate().toString()));
                    writer.write(',');
                    writer.write(escapeCsvField(t.getDescription() != null ? t.getDescription() : ""));
                    writer.write(',');
                    writer.write(escapeCsvField(t.getAmount().toPlainString()));
                    writer.write(',');
                    writer.write(escapeCsvField(categoryNames.getOrDefault(t.getCategoryId(), "Unknown")));
                    writer.write(',');
                    writer.write(escapeCsvField(t.getType().name()));
                    writer.write("\r\n");
                }
            }
        };
    }

    private Map<Long, String> buildCategoryNameMap(Long userId) {
        return categoryRepository.findByUserIdOrUserIdIsNull(userId).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
    }

    private TransactionResponse toResponse(Transaction t, Map<Long, String> categoryNames) {
        return new TransactionResponse(
                t.getId(),
                t.getAmount(),
                t.getDescription(),
                t.getDate(),
                t.getType(),
                t.getCategoryId(),
                categoryNames.getOrDefault(t.getCategoryId(), "Unknown"));
    }

    /**
     * Escapes a single CSV field per RFC 4180. If the value contains a comma, double-quote,
     * or newline, it is wrapped in double-quotes and internal double-quotes are doubled.
     *
     * @param value the raw field value (never null)
     * @return the escaped field ready to be written to the CSV stream
     */
    private String escapeCsvField(String value) {
        if (value.indexOf(',') >= 0 || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }
}
