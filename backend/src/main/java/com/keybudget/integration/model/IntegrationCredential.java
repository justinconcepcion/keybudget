package com.keybudget.integration.model;

import com.keybudget.integration.ProviderType;
import com.keybudget.integration.SyncStatus;
import com.keybudget.user.User;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * Stores encrypted credentials for a user's external financial provider integration.
 * The {@code credentialData} field is always an AES-GCM encrypted JSON blob — it must
 * never be persisted or logged in plaintext.
 */
@Entity
@Table(
    name = "integration_credentials",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_integration_credential_user_provider",
        columnNames = {"user_id", "provider_type"}
    )
)
public class IntegrationCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false, length = 50)
    private ProviderType providerType;

    /**
     * AES-GCM encrypted JSON blob containing provider-specific credential fields
     * (e.g., API key, API secret, wallet address). Never store or log in plaintext.
     */
    @Column(name = "credential_data", nullable = false, columnDefinition = "TEXT")
    private String credentialData;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SyncStatus status;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

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
    public ProviderType getProviderType() { return providerType; }
    public String getCredentialData() { return credentialData; }
    public SyncStatus getStatus() { return status; }
    public Instant getLastSyncedAt() { return lastSyncedAt; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // Setters

    public void setUser(User user) { this.user = user; }
    public void setProviderType(ProviderType providerType) { this.providerType = providerType; }
    public void setCredentialData(String credentialData) { this.credentialData = credentialData; }
    public void setStatus(SyncStatus status) { this.status = status; }
    public void setLastSyncedAt(Instant lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
