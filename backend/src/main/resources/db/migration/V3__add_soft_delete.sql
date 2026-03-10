-- V3__add_soft_delete.sql
-- Adds soft-delete support to transactions and budgets.
-- @SQLRestriction("deleted_at IS NULL") on the entities ensures all JPA queries
-- automatically exclude soft-deleted rows without any repository changes.

ALTER TABLE transactions ADD COLUMN deleted_at TIMESTAMP;
ALTER TABLE budgets ADD COLUMN deleted_at TIMESTAMP;

CREATE INDEX idx_transactions_deleted_at ON transactions(deleted_at);
CREATE INDEX idx_budgets_deleted_at ON budgets(deleted_at);
