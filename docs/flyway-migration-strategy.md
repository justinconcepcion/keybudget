# Flyway Migration Strategy

**Issue:** #18
**Date:** 2026-03-09

---

## Overview

Migrate from JPA auto-DDL (`hibernate.ddl-auto=create-drop` in dev, `validate` in prod) to Flyway-managed migrations. This gives us version-controlled, repeatable schema changes and eliminates the risk of JPA silently altering production tables.

## Current Schema (from Entity Analysis)

Five tables derived from the `@Entity` classes:

### `users`
| Column | Type | Constraints |
|--------|------|-------------|
| `id` | `BIGINT` / `BIGSERIAL` | PK, auto-generated (IDENTITY) |
| `google_sub` | `VARCHAR(255)` | NOT NULL, UNIQUE |
| `email` | `VARCHAR(255)` | NOT NULL |
| `name` | `VARCHAR(255)` | NOT NULL |
| `picture_url` | `VARCHAR(255)` | nullable |
| `created_at` | `TIMESTAMP` | NOT NULL, set by `@PrePersist` |

### `integration_credentials`
| Column | Type | Constraints |
|--------|------|-------------|
| `id` | `BIGINT` / `BIGSERIAL` | PK, IDENTITY |
| `user_id` | `BIGINT` | NOT NULL, FK → `users(id)` |
| `provider_type` | `VARCHAR(50)` | NOT NULL (enum: COINBASE, BITCOIN_WALLET, M1_FINANCE, MARCUS) |
| `credential_data` | `TEXT` | NOT NULL (AES-GCM encrypted JSON) |
| `status` | `VARCHAR(20)` | NOT NULL (enum: ACTIVE, EXPIRED, REVOKED, ERROR) |
| `last_synced_at` | `TIMESTAMP` | nullable |
| `error_message` | `VARCHAR(500)` | nullable |
| `created_at` | `TIMESTAMP` | NOT NULL |
| `updated_at` | `TIMESTAMP` | NOT NULL |
| **Unique** | `uq_integration_credential_user_provider` | `(user_id, provider_type)` |

### `financial_accounts`
| Column | Type | Constraints |
|--------|------|-------------|
| `id` | `BIGINT` / `BIGSERIAL` | PK, IDENTITY |
| `user_id` | `BIGINT` | NOT NULL, FK → `users(id)` |
| `credential_id` | `BIGINT` | NOT NULL, FK → `integration_credentials(id)` |
| `provider_type` | `VARCHAR(50)` | NOT NULL |
| `account_type` | `VARCHAR(30)` | NOT NULL (enum: CRYPTO_WALLET, BROKERAGE, SAVINGS, CHECKING) |
| `external_id` | `VARCHAR(255)` | NOT NULL |
| `display_name` | `VARCHAR(100)` | NOT NULL |
| `currency` | `VARCHAR(10)` | NOT NULL |
| `active` | `BOOLEAN` | NOT NULL, default `true` |
| `created_at` | `TIMESTAMP` | NOT NULL |
| `updated_at` | `TIMESTAMP` | NOT NULL |
| **Unique** | `uq_financial_account_credential_external_id` | `(credential_id, external_id)` |

### `financial_account_balances`
| Column | Type | Constraints |
|--------|------|-------------|
| `id` | `BIGINT` / `BIGSERIAL` | PK, IDENTITY |
| `account_id` | `BIGINT` | NOT NULL, FK → `financial_accounts(id)`, UNIQUE |
| `balance` | `NUMERIC(18,8)` | NOT NULL |
| `balance_usd` | `NUMERIC(14,2)` | NOT NULL |
| `as_of` | `TIMESTAMP` | NOT NULL |
| `updated_at` | `TIMESTAMP` | NOT NULL |

### `balance_snapshots`
| Column | Type | Constraints |
|--------|------|-------------|
| `id` | `BIGINT` / `BIGSERIAL` | PK, IDENTITY |
| `account_id` | `BIGINT` | NOT NULL, FK → `financial_accounts(id)` |
| `balance` | `NUMERIC(18,8)` | NOT NULL |
| `balance_usd` | `NUMERIC(14,2)` | NOT NULL |
| `recorded_at` | `TIMESTAMP` | NOT NULL |
| **Index** | `idx_balance_snapshot_account_recorded_at` | `(account_id, recorded_at DESC)` |

## Migration Plan

### Step 1: Add Flyway Dependency

Add to `backend/pom.xml`:
```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

Spring Boot 3.3.x auto-configures Flyway when it's on the classpath and a datasource is present.

### Step 2: Baseline Migration

File: `backend/src/main/resources/db/migration/V1__baseline.sql`

```sql
-- =============================================
-- V1__baseline.sql
-- Captures the full schema as of Phase 3.
-- =============================================

CREATE TABLE users (
    id          BIGSERIAL       PRIMARY KEY,
    google_sub  VARCHAR(255)    NOT NULL UNIQUE,
    email       VARCHAR(255)    NOT NULL,
    name        VARCHAR(255)    NOT NULL,
    picture_url VARCHAR(255),
    created_at  TIMESTAMP       NOT NULL DEFAULT now()
);

CREATE TABLE integration_credentials (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL REFERENCES users(id),
    provider_type   VARCHAR(50)     NOT NULL,
    credential_data TEXT            NOT NULL,
    status          VARCHAR(20)     NOT NULL,
    last_synced_at  TIMESTAMP,
    error_message   VARCHAR(500),
    created_at      TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT now(),
    CONSTRAINT uq_integration_credential_user_provider
        UNIQUE (user_id, provider_type)
);

CREATE TABLE financial_accounts (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL REFERENCES users(id),
    credential_id   BIGINT          NOT NULL REFERENCES integration_credentials(id),
    provider_type   VARCHAR(50)     NOT NULL,
    account_type    VARCHAR(30)     NOT NULL,
    external_id     VARCHAR(255)    NOT NULL,
    display_name    VARCHAR(100)    NOT NULL,
    currency        VARCHAR(10)     NOT NULL,
    active          BOOLEAN         NOT NULL DEFAULT true,
    created_at      TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT now(),
    CONSTRAINT uq_financial_account_credential_external_id
        UNIQUE (credential_id, external_id)
);

CREATE TABLE financial_account_balances (
    id          BIGSERIAL       PRIMARY KEY,
    account_id  BIGINT          NOT NULL UNIQUE REFERENCES financial_accounts(id),
    balance     NUMERIC(18, 8)  NOT NULL,
    balance_usd NUMERIC(14, 2)  NOT NULL,
    as_of       TIMESTAMP       NOT NULL,
    updated_at  TIMESTAMP       NOT NULL DEFAULT now()
);

CREATE TABLE balance_snapshots (
    id          BIGSERIAL       PRIMARY KEY,
    account_id  BIGINT          NOT NULL REFERENCES financial_accounts(id),
    balance     NUMERIC(18, 8)  NOT NULL,
    balance_usd NUMERIC(14, 2)  NOT NULL,
    recorded_at TIMESTAMP       NOT NULL DEFAULT now()
);

CREATE INDEX idx_balance_snapshot_account_recorded_at
    ON balance_snapshots (account_id, recorded_at DESC);
```

### Step 3: Migration Naming Convention

```
V{version}__{description}.sql     — versioned (run once, in order)
R__{description}.sql              — repeatable (re-run when checksum changes)
```

Examples of future migrations:
```
V1__baseline.sql
V2__add_transaction_table.sql
V3__add_plaid_provider_type.sql
R__refresh_net_worth_view.sql
```

Rules:
- Double underscore between version and description
- Use snake_case for descriptions
- Never edit a migration that has already been applied — create a new one

### Step 4: Spring Boot Configuration

#### `application.properties` (shared)
```properties
# Flyway
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
```

#### `application-dev.properties` (H2 — non-Docker local dev)
```properties
# Keep H2 for fast local iteration without Docker
spring.datasource.url=jdbc:h2:mem:keybudget;DB_CLOSE_DELAY=-1;MODE=PostgreSQL
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.hibernate.ddl-auto=none
spring.flyway.enabled=true
```

Key change: `ddl-auto` moves from `create-drop` to `none`. Flyway owns the schema now.

The `MODE=PostgreSQL` H2 flag provides compatibility for PostgreSQL-specific syntax (e.g., `BIGSERIAL`, `TIMESTAMP` defaults). Anything that H2 cannot handle in PostgreSQL mode should be split into a separate location.

#### `application-prod.properties`
```properties
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
```

`validate` remains — Hibernate still checks that entities match the Flyway-created schema on startup.

#### Test profile (`test/resources/application.properties`)
```properties
spring.jpa.hibernate.ddl-auto=none
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
```

Tests run Flyway against H2, same as dev. If a migration uses PostgreSQL-only syntax that H2 cannot handle, add a test-specific location:
```properties
spring.flyway.locations=classpath:db/migration,classpath:db/migration-h2
```

### Step 5: Handling H2 vs PostgreSQL Differences

| Concern | PostgreSQL | H2 (with MODE=PostgreSQL) |
|---------|------------|---------------------------|
| `BIGSERIAL` | Native | Supported in PostgreSQL mode |
| `TIMESTAMP` defaults (`now()`) | Native | Supported |
| `TEXT` type | Native | Supported |
| `NUMERIC(p,s)` | Native | Supported |
| Index with `DESC` | Native | Supported |
| Extensions (e.g., `pgcrypto`) | Native | Not supported — use separate location |

**Strategy:** Write migrations in PostgreSQL syntax. Use `H2 MODE=PostgreSQL` for local/test. If a future migration needs a PostgreSQL extension or feature not supported by H2, place an H2-compatible alternative in `db/migration-h2/` with the same version number and configure the test/dev profile to use both locations.

### Step 6: Generating the Baseline from a Running H2 Schema

If you want to verify the baseline against what JPA currently generates:

1. Start the app with the current `create-drop` dev profile
2. Connect to H2 console at `http://localhost:8080/h2-console`
   - JDBC URL: `jdbc:h2:mem:keybudget`
   - User: `sa`, no password
3. Run: `SCRIPT NODATA` — this dumps the full DDL
4. Compare with `V1__baseline.sql` and adjust any differences

Alternatively, enable H2 console temporarily:
```properties
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

### Step 7: Applying to an Existing Production Database

If a PostgreSQL database already exists with tables created by `ddl-auto=update`:

1. **Baseline the existing schema:**
   ```properties
   spring.flyway.baseline-on-migrate=true
   spring.flyway.baseline-version=1
   ```
   This tells Flyway "version 1 is already applied — start tracking from V2 onward."

2. Run the app once. Flyway creates its `flyway_schema_history` table and records V1 as already applied.

3. Remove `baseline-on-migrate` after the first run — it should not remain enabled permanently.

If the database is empty (fresh deployment), Flyway runs `V1__baseline.sql` normally.

## Rollback Strategy

Flyway Community Edition does not support automatic rollbacks. Our approach:

1. **Forward-only migrations:** Every change is a new `V{n}__*.sql` file. To "undo" a change, write a new migration that reverses it (e.g., `V4__drop_deprecated_column.sql`).

2. **Pre-deploy backup:** Before applying migrations in production, take a `pg_dump` snapshot. If a migration fails or causes issues, restore from the dump.

3. **Testing gate:** All migrations must pass against H2 in CI (`./mvnw test`) before reaching production. The `validate` mode in prod catches entity-schema mismatches at startup.

4. **Manual repair:** If a migration partially applies and leaves Flyway in a broken state, use:
   ```bash
   ./mvnw flyway:repair
   ```
   This removes failed entries from `flyway_schema_history` so the fixed migration can be re-applied.

## Migration File Location

```
backend/
  src/
    main/
      resources/
        db/
          migration/
            V1__baseline.sql
            V2__next_change.sql
            ...
          migration-h2/          (only if needed for H2 workarounds)
            V{n}__h2_compat.sql
```

## Checklist for Implementation

- [ ] Add `flyway-core` and `flyway-database-postgresql` to `pom.xml`
- [ ] Create `db/migration/V1__baseline.sql` with the schema above
- [ ] Change `application-dev.properties`: `ddl-auto=none`, add `MODE=PostgreSQL` to H2 URL
- [ ] Change test `application.properties`: `ddl-auto=none`
- [ ] Keep `application-prod.properties` at `ddl-auto=validate`
- [ ] Add `spring.flyway.enabled=true` and `spring.flyway.locations` to shared properties
- [ ] Verify: `./mvnw test` passes (Flyway creates schema in H2, Hibernate validates)
- [ ] Verify: Docker Compose backend starts (Flyway creates schema in PostgreSQL, Hibernate validates)
- [ ] Add `spring.flyway.baseline-on-migrate=true` only if migrating an existing prod DB
