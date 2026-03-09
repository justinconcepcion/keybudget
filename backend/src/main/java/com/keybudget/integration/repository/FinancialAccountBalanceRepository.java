package com.keybudget.integration.repository;

import com.keybudget.integration.model.FinancialAccountBalance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Repository for {@link FinancialAccountBalance} persistence operations. */
public interface FinancialAccountBalanceRepository extends JpaRepository<FinancialAccountBalance, Long> {

    /**
     * Retrieves the current balance record for a given account.
     *
     * @param accountId the financial account's primary key
     * @return the balance record, if one exists
     */
    Optional<FinancialAccountBalance> findByAccountId(Long accountId);
}
