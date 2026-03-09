package com.keybudget.budget;

import com.keybudget.shared.converter.YearMonthConverter;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;

/**
 * Represents a monthly spending limit set by a user for a specific category.
 * The combination of (userId, categoryId, monthYear) is unique.
 */
@Entity
@Table(
        name = "budgets",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_budget_user_category_month",
                columnNames = {"user_id", "category_id", "month_year"}
        )
)
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Convert(converter = YearMonthConverter.class)
    @Column(name = "month_year", nullable = false, length = 7)
    private YearMonth monthYear;

    @Column(name = "limit_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal limitAmount;

    @Version
    private Long version;

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

    public YearMonth getMonthYear() { return monthYear; }

    public BigDecimal getLimitAmount() { return limitAmount; }

    public Long getVersion() { return version; }

    public Instant getCreatedAt() { return createdAt; }

    // Setters

    public void setUserId(Long userId) { this.userId = userId; }

    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public void setMonthYear(YearMonth monthYear) { this.monthYear = monthYear; }

    public void setLimitAmount(BigDecimal limitAmount) { this.limitAmount = limitAmount; }
}
