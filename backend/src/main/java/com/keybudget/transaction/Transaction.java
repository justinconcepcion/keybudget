package com.keybudget.transaction;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Represents a single financial transaction (income or expense) belonging to a user.
 */
@Entity
@Table(name = "transactions")
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

    // Setters

    public void setUserId(Long userId) { this.userId = userId; }

    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public void setDescription(String description) { this.description = description; }

    public void setDate(LocalDate date) { this.date = date; }

    public void setType(TransactionType type) { this.type = type; }
}
