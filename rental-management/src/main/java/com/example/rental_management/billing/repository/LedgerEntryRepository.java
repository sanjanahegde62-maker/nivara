package com.example.rental_management.billing.repository;

import com.example.rental_management.billing.entity.LedgerEntry;
import com.example.rental_management.billing.entity.LedgerEntryType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    /** Full chronological ledger for a lease. */
    List<LedgerEntry> findByLeaseIdOrderByCreatedAtAsc(Long leaseId);

    /**
     * Guard for LateFeeJob idempotency: has a late-fee entry already been
     * recorded for this charge on this calendar date?
     */
    boolean existsByRentChargeIdAndEntryTypeAndFeeDate(
            Long rentChargeId, LedgerEntryType entryType, LocalDate feeDate);

    /**
     * Guard for LeaseExpiryJob idempotency: has an expiry-notice entry already
     * been recorded for this lease on this calendar date?
     */
    boolean existsByLeaseIdAndEntryTypeAndFeeDate(
            Long leaseId, LedgerEntryType entryType, LocalDate feeDate);
}
