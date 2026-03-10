package com.keybudget.transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Data access for {@link Transaction} entities. */
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * Returns a page of transactions for a user within a date range.
     *
     * @param userId   the owning user's id
     * @param start    inclusive start date
     * @param end      inclusive end date
     * @param pageable pagination and sorting parameters
     * @return page of matching transactions
     */
    Page<Transaction> findByUserIdAndDateBetween(
            Long userId, LocalDate start, LocalDate end, Pageable pageable);

    /**
     * Returns all transactions for a user within a specific category and date range.
     * Used for budget spending calculations and monthly summaries.
     *
     * @param userId     the owning user's id
     * @param categoryId the category to filter by
     * @param start      inclusive start date
     * @param end        inclusive end date
     * @return list of matching transactions
     */
    List<Transaction> findByUserIdAndCategoryIdAndDateBetween(
            Long userId, Long categoryId, LocalDate start, LocalDate end);

    /**
     * Returns all transactions for a user within a date range, filtered by type.
     *
     * @param userId the owning user's id
     * @param start  inclusive start date
     * @param end    inclusive end date
     * @param type   INCOME or EXPENSE
     * @return list of matching transactions
     */
    List<Transaction> findByUserIdAndDateBetweenAndType(
            Long userId, LocalDate start, LocalDate end, TransactionType type);

    /**
     * Returns a page of transactions for a user within a date range filtered by category.
     *
     * @param userId     the owning user's id
     * @param categoryId the category to filter by
     * @param start      inclusive start date
     * @param end        inclusive end date
     * @param pageable   pagination and sorting parameters
     * @return page of matching transactions
     */
    Page<Transaction> findByUserIdAndCategoryIdAndDateBetween(
            Long userId, Long categoryId, LocalDate start, LocalDate end, Pageable pageable);

    /**
     * Returns a page of transactions for a user within a date range filtered by type.
     *
     * @param userId   the owning user's id
     * @param start    inclusive start date
     * @param end      inclusive end date
     * @param type     INCOME or EXPENSE
     * @param pageable pagination and sorting parameters
     * @return page of matching transactions
     */
    Page<Transaction> findByUserIdAndDateBetweenAndType(
            Long userId, LocalDate start, LocalDate end, TransactionType type, Pageable pageable);

    /**
     * Returns a page of transactions for a user within a date range filtered by category and type.
     *
     * @param userId     the owning user's id
     * @param categoryId the category to filter by
     * @param start      inclusive start date
     * @param end        inclusive end date
     * @param type       INCOME or EXPENSE
     * @param pageable   pagination and sorting parameters
     * @return page of matching transactions
     */
    Page<Transaction> findByUserIdAndCategoryIdAndDateBetweenAndType(
            Long userId, Long categoryId, LocalDate start, LocalDate end, TransactionType type,
            Pageable pageable);

    /**
     * Returns all transactions for a user within a date range (unpaged).
     * Used for monthly summary aggregation.
     *
     * @param userId the owning user's id
     * @param start  inclusive start date
     * @param end    inclusive end date
     * @return list of matching transactions
     */
    List<Transaction> findByUserIdAndDateBetween(Long userId, LocalDate start, LocalDate end);

    Optional<Transaction> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndCategoryId(Long userId, Long categoryId);

    /**
     * Returns {@code true} if a transaction with the given import hash already exists.
     * Used by {@link CsvImportService} to detect duplicate CSV rows before attempting
     * a save, avoiding unnecessary constraint-violation exceptions.
     *
     * @param importHash the 64-character SHA-256 hex hash to look up
     * @return {@code true} if a matching row exists
     */
    boolean existsByImportHash(String importHash);

    @Query("SELECT t.categoryId, SUM(t.amount) FROM Transaction t " +
           "WHERE t.userId = :userId AND t.type = 'EXPENSE' " +
           "AND t.date BETWEEN :start AND :end " +
           "GROUP BY t.categoryId")
    List<Object[]> sumExpensesByCategory(Long userId, LocalDate start, LocalDate end);

    /**
     * Returns all transactions for a user within a date range, ordered by date ascending
     * then by id ascending. Used for deterministic CSV export output.
     * Soft-deleted rows are excluded automatically via the {@code @SQLRestriction} on
     * {@link Transaction}.
     *
     * @param userId the owning user's id
     * @param start  inclusive start date
     * @param end    inclusive end date
     * @return ordered list of matching transactions
     */
    @Query("SELECT t FROM Transaction t WHERE t.userId = :userId " +
           "AND t.date BETWEEN :start AND :end ORDER BY t.date ASC, t.id ASC")
    List<Transaction> findByUserIdAndDateBetweenOrderByDateAscIdAsc(
            Long userId, LocalDate start, LocalDate end);
}
