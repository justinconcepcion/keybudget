-- V2__add_preferred_currency.sql
-- Add user currency preference (defaults to USD)

ALTER TABLE users ADD COLUMN preferred_currency VARCHAR(3) NOT NULL DEFAULT 'USD';
