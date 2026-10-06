package com.example.rental_management.billing.entity;

/**
 * The type of movement recorded in a lease's sub-ledger.
 * DEBIT increases what the tenant owes; CREDIT reduces it.
 */
public enum LedgerEntryType {
    /** A rent charge raised for a billing period (debit). */
    RENT_CHARGE,
    /** A payment received against a charge (credit). */
    PAYMENT,
    /** A late fee levied on an overdue charge (debit). */
    LATE_FEE,
    /** An informational notice recorded when a lease is about to expire or has expired. */
    LEASE_EXPIRY_NOTICE
}
