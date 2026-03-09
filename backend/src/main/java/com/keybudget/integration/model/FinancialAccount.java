package com.keybudget.integration.model;

import com.keybudget.integration.AccountType;
import com.keybudget.integration.ProviderType;
import com.keybudget.user.User;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * Represents a single financial account discovered from an external provider
 * (e.g., a Coinbase wallet, M1 brokerage account, or Marcus savings account).
 */
@Entity
@Table(
    name = "financial_accounts",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_financial_account_credential_external_id",
        columnNames = {"credential_id", "external_id"}
    )
)
public class FinancialAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "credential_id", nullable = false)
    private IntegrationCredential credential;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false, length = 50)
    private ProviderType providerType;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 30)
    private AccountType accountType;

    @Column(name = "external_id", nullable = false, length = 255)
    private String externalId;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters

    public Long getId() { return id; }
    public User getUser() { return user; }
    public IntegrationCredential getCredential() { return credential; }
    public ProviderType getProviderType() { return providerType; }
    public AccountType getAccountType() { return accountType; }
    public String getExternalId() { return externalId; }
    public String getDisplayName() { return displayName; }
    public String getCurrency() { return currency; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // Setters

    public void setUser(User user) { this.user = user; }
    public void setCredential(IntegrationCredential credential) { this.credential = credential; }
    public void setProviderType(ProviderType providerType) { this.providerType = providerType; }
    public void setAccountType(AccountType accountType) { this.accountType = accountType; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setCurrency(String currency) { this.currency = currency; }
    public void setActive(boolean active) { this.active = active; }
}
