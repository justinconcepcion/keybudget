package com.keybudget.transaction;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Represents a single financial transaction (income or expense) belonging to a user.
 * Rows are never physically deleted — setting {@code deletedAt} hides them from all queries.
 */
@Entity
@Table(name = "transactions")
@SQLRestriction("deleted_at IS NULL")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    private String description;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @PrePersist
    void prePersist() {
        this.createdAt = Instant.now();
    }

    // Getters

    public Long getId() { return id; }

    public Long getUserId() { return userId; }

    public Long getCategoryId() { return categoryId; }

    public BigDecimal getAmount() { return amount; }

    public String getDescription() { return description; }

    public LocalDate getDate() { return date; }

    public TransactionType getType() { return type; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getDeletedAt() { return deletedAt; }

    // Setters

    public void setUserId(Long userId) { this.userId = userId; }

    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public void setDescription(String description) { this.description = description; }

    public void setDate(LocalDate date) { this.date = date; }

    public void setType(TransactionType type) { this.type = type; }

    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
}
