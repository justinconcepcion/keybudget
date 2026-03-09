package com.keybudget.transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
