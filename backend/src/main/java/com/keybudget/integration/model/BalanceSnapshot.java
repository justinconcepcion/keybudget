package com.keybudget.integration.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Immutable historical record of a {@link FinancialAccount} balance at a point in time.
 * Written on every sync cycle to support net-worth history queries.
 */
@Entity
@Table(
    name = "balance_snapshots",
    indexes = @Index(
        name = "idx_balance_snapshot_account_recorded_at",
        columnList = "account_id, recorded_at DESC"
    )
)
public class BalanceSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private FinancialAccount account;

    /**
     * Native-currency balance at the time of the snapshot.
     * Precision 18, scale 8 to accommodate crypto denominations.
     */
    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal balance;

    /**
     * USD equivalent at the time of the snapshot.
     * Precision 14, scale 2 for fiat representation.
     */
    @Column(name = "balance_usd", nullable = false, precision = 14, scale = 2)
    private BigDecimal balanceUsd;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    @PrePersist
    void prePersist() {
        if (this.recordedAt == null) {
            this.recordedAt = Instant.now();
        }
    }

    // Getters

    public Long getId() { return id; }
    public FinancialAccount getAccount() { return account; }
    public BigDecimal getBalance() { return balance; }
    public BigDecimal getBalanceUsd() { return balanceUsd; }
    public Instant getRecordedAt() { return recordedAt; }

    // Setters

    public void setAccount(FinancialAccount account) { this.account = account; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public void setBalanceUsd(BigDecimal balanceUsd) { this.balanceUsd = balanceUsd; }
    public void setRecordedAt(Instant recordedAt) { this.recordedAt = recordedAt; }
}
