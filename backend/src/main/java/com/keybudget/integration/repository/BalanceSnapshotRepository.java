package com.keybudget.integration.repository;

import com.keybudget.integration.model.BalanceSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

/** Repository for {@link BalanceSnapshot} persistence operations. */
public interface BalanceSnapshotRepository extends JpaRepository<BalanceSnapshot, Long> {

    /**
     * Returns all balance snapshots for the given account, newest first.
     * Used to display balance history charts.
     *
     * @param accountId the financial account's primary key
     * @return snapshots ordered by recorded_at descending
     */
    List<BalanceSnapshot> findByAccountIdOrderByRecordedAtDesc(Long accountId);

    /**
     * Returns all snapshots for the given account recorded on or after the cutoff time.
     * Used to build net-worth history over a rolling time window.
     *
     * @param accountId the financial account's primary key
     * @param after     the inclusive lower bound for recorded_at
     * @return snapshots in descending recorded_at order
     */
    List<BalanceSnapshot> findByAccountIdAndRecordedAtAfterOrderByRecordedAtDesc(
            Long accountId, Instant after);
}
