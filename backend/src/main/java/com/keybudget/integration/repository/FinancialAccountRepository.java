package com.keybudget.integration.repository;

import com.keybudget.integration.model.FinancialAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Repository for {@link FinancialAccount} persistence operations. */
public interface FinancialAccountRepository extends JpaRepository<FinancialAccount, Long> {

    /**
     * Returns all accounts belonging to the given user (active and inactive).
     *
     * @param userId the user's primary key
     * @return list of financial accounts
     */
    List<FinancialAccount> findByUserId(Long userId);

    /**
     * Returns all accounts associated with the given credential.
     * Used when disconnecting a provider to locate accounts to deactivate.
     *
     * @param credentialId the credential's primary key
     * @return list of financial accounts
     */
    List<FinancialAccount> findByCredentialId(Long credentialId);

    /**
     * Returns all active accounts belonging to the given user.
     * Used when computing net worth and other aggregations.
     *
     * @param userId the user's primary key
     * @return list of active financial accounts
     */
    List<FinancialAccount> findByUserIdAndActiveTrue(Long userId);
}
