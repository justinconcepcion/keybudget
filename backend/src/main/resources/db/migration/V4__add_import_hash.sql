-- V4__add_import_hash.sql
-- Add SHA-256 import hash to transactions for CSV duplicate detection.
-- Only CSV-imported transactions carry a hash; manually created rows remain NULL.
-- Both PostgreSQL and H2 treat NULLs as distinct in unique indexes,
-- so multiple NULL rows are allowed without a partial index.

ALTER TABLE transactions ADD COLUMN import_hash VARCHAR(64);

CREATE UNIQUE INDEX idx_transactions_import_hash
    ON transactions (import_hash);
