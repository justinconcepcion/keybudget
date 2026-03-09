package com.keybudget.integration.repository;

import com.keybudget.integration.ProviderType;
import com.keybudget.integration.SyncStatus;
import com.keybudget.integration.model.IntegrationCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Repository for {@link IntegrationCredential} persistence operations. */
public interface IntegrationCredentialRepository extends JpaRepository<IntegrationCredential, Long> {

    /**
     * Returns all credentials belonging to the given user, regardless of status.
     *
     * @param userId the user's primary key
     * @return list of credentials ordered by creation time descending
     */
    List<IntegrationCredential> findByUserId(Long userId);

    /**
     * Looks up a specific provider credential for a user.
     * Returns {@link Optional#empty()} if the user has not connected that provider.
     *
     * @param userId       the user's primary key
     * @param providerType the provider to look up
     * @return the matching credential, if present
     */
    Optional<IntegrationCredential> findByUserIdAndProviderType(Long userId, ProviderType providerType);

    /**
     * Returns all credentials with the given sync status across all users.
     * Intended for background sync jobs.
     *
     * @param status the status to filter by
     * @return list of matching credentials
     */
    List<IntegrationCredential> findByStatus(SyncStatus status);
}
