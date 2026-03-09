package com.keybudget.integration;

/** Represents the current state of an integration credential. */
public enum SyncStatus {
    ACTIVE,
    EXPIRED,
    REVOKED,
    ERROR
}
