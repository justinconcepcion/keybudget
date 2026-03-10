-- V4__add_import_hash.sql
-- Add SHA-256 import hash to transactions for CSV duplicate detection.
-- Only CSV-imported transactions carry a hash; manually created rows remain NULL.
-- Both PostgreSQL and H2 honour the partial index, so multiple NULLs are allowed.

ALTER TABLE transactions ADD COLUMN import_hash VARCHAR(64);

CREATE UNIQUE INDEX idx_transactions_import_hash
    ON transactions (import_hash)
    WHERE import_hash IS NOT NULL;
