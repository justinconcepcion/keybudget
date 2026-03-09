package com.keybudget.integration.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Holds the most recent balance for a {@link FinancialAccount}.
 * One-to-one with FinancialAccount; upserted on each sync cycle.
 */
@Entity
@Table(name = "financial_account_balances")
public class FinancialAccountBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private FinancialAccount account;

    /**
     * Native-currency balance (e.g., BTC, ETH, USD).
     * Precision 18, scale 8 to accommodate crypto denominations.
     */
    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal balance;

    /**
     * USD equivalent of {@code balance} at the time of the last sync.
     * Precision 14, scale 2 for fiat representation.
     */
    @Column(name = "balance_usd", nullable = false, precision = 14, scale = 2)
    private BigDecimal balanceUsd;

    /** Timestamp at which the provider reported this balance. */
    @Column(name = "as_of", nullable = false)
    private Instant asOf;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void preWrite() {
        this.updatedAt = Instant.now();
    }

    // Getters

    public Long getId() { return id; }
    public FinancialAccount getAccount() { return account; }
    public BigDecimal getBalance() { return balance; }
    public BigDecimal getBalanceUsd() { return balanceUsd; }
    public Instant getAsOf() { return asOf; }
    public Instant getUpdatedAt() { return updatedAt; }

    // Setters

    public void setAccount(FinancialAccount account) { this.account = account; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public void setBalanceUsd(BigDecimal balanceUsd) { this.balanceUsd = balanceUsd; }
    public void setAsOf(Instant asOf) { this.asOf = asOf; }
}
