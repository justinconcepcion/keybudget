# Docker Compose Design — Local Development

**Issue:** #19
**Date:** 2026-03-09

---

## Overview

A single `docker-compose.yml` at the project root that spins up three services for local development: PostgreSQL, the Spring Boot backend, and the Vue 3 frontend (Vite dev server). All secrets are injected via environment variables sourced from a `.env` file (never committed).

## Services

### 1. `postgres`

| Setting | Value |
|---------|-------|
| Image | `postgres:16-alpine` |
| Port | `5432:5432` |
| Volume | `pgdata:/var/lib/postgresql/data` (named volume for persistence across restarts) |
| Init script mount | `./docker/init-db.sql:/docker-entrypoint-initdb.d/init-db.sql` (optional — Flyway handles schema) |

Environment variables:
```
POSTGRES_DB=keybudget
POSTGRES_USER=${DB_USERNAME}
POSTGRES_PASSWORD=${DB_PASSWORD}
```

Health check:
```yaml
healthcheck:
  test: ["CMD-SHELL", "pg_isready -U ${DB_USERNAME} -d keybudget"]
  interval: 5s
  timeout: 3s
  retries: 5
```

### 2. `backend`

| Setting | Value |
|---------|-------|
| Build context | `./backend` |
| Dockerfile | `./backend/Dockerfile.dev` |
| Port | `8080:8080` |
| Depends on | `postgres` (condition: `service_healthy`) |
| Profile | `dev` |

**Dockerfile.dev** (multi-stage not needed for dev):
```dockerfile
FROM amazoncorretto:21-alpine

WORKDIR /app

# Copy Maven wrapper and POM first for layer caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy source
COPY src/ src/

# Run with spring-boot:run for DevTools live reload
EXPOSE 8080
CMD ["./mvnw", "spring-boot:run", "-Dspring-boot.run.jvmArguments=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"]
```

Volume mounts for live code changes:
```yaml
volumes:
  - ./backend/src:/app/src:ro
```

> **Note:** `spring-boot:run` with the source mount lets you rebuild on change. For true hot-reload, Spring Boot DevTools (already on the classpath via starter) detects changes when the IDE compiles. The volume mount ensures container sees the latest `.class` files if using an external build trigger.

Environment variables (all injected from `.env`):
```yaml
environment:
  SPRING_PROFILES_ACTIVE: dev
  # Override dev profile to point at Postgres instead of H2
  SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/keybudget
  SPRING_DATASOURCE_USERNAME: ${DB_USERNAME}
  SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
  SPRING_JPA_DATABASE_PLATFORM: org.hibernate.dialect.PostgreSQLDialect
  SPRING_JPA_HIBERNATE_DDL_AUTO: validate
  # App secrets
  APP_FRONTEND_URL: http://localhost:5173
  APP_JWT_PRIVATE_KEY: ${JWT_PRIVATE_KEY}
  APP_JWT_PUBLIC_KEY: ${JWT_PUBLIC_KEY}
  APP_ENCRYPTION_KEY: ${ENCRYPTION_KEY}
  # OAuth2
  SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_ID: ${GOOGLE_CLIENT_ID}
  SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET: ${GOOGLE_CLIENT_SECRET}
  # Integration APIs (public, no keys needed)
  INTEGRATION_BITCOIN_BLOCKSTREAM_URL: https://blockstream.info/api
  INTEGRATION_COINGECKO_PRICE_URL: https://api.coingecko.com/api/v3/simple/price
  INTEGRATION_COINBASE_API_URL: https://api.coinbase.com
  INTEGRATION_COINBASE_API_VERSION: 2024-01-01
```

Health check:
```yaml
healthcheck:
  test: ["CMD-SHELL", "curl -sf http://localhost:8080/actuator/health || exit 1"]
  interval: 10s
  timeout: 5s
  retries: 5
  start_period: 30s
```

Debug port (optional):
```yaml
ports:
  - "8080:8080"
  - "5005:5005"  # JVM remote debug
```

### 3. `frontend`

| Setting | Value |
|---------|-------|
| Build context | `./frontend` |
| Dockerfile | `./frontend/Dockerfile.dev` |
| Port | `5173:5173` |
| Depends on | `backend` (condition: `service_healthy`) |

**Dockerfile.dev:**
```dockerfile
FROM node:20-alpine

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .

EXPOSE 5173
CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0"]
```

Volume mounts for hot module replacement:
```yaml
volumes:
  - ./frontend:/app
  - /app/node_modules  # anonymous volume — prevent host node_modules from overriding container's
```

Environment variables:
```yaml
environment:
  VITE_API_BASE_URL: http://localhost:8080
```

Health check:
```yaml
healthcheck:
  test: ["CMD-SHELL", "wget -qO- http://localhost:5173 || exit 1"]
  interval: 10s
  timeout: 3s
  retries: 5
  start_period: 10s
```

## Complete docker-compose.yml

```yaml
version: "3.9"

services:
  postgres:
    image: postgres:16-alpine
    container_name: keybudget-db
    restart: unless-stopped
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: keybudget
      POSTGRES_USER: ${DB_USERNAME}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U $${DB_USERNAME} -d keybudget"]
      interval: 5s
      timeout: 3s
      retries: 5

  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile.dev
    container_name: keybudget-api
    restart: unless-stopped
    ports:
      - "8080:8080"
      - "5005:5005"
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      SPRING_PROFILES_ACTIVE: dev
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/keybudget
      SPRING_DATASOURCE_USERNAME: ${DB_USERNAME}
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      SPRING_JPA_DATABASE_PLATFORM: org.hibernate.dialect.PostgreSQLDialect
      SPRING_JPA_HIBERNATE_DDL_AUTO: validate
      APP_FRONTEND_URL: http://localhost:5173
      APP_JWT_PRIVATE_KEY: ${JWT_PRIVATE_KEY}
      APP_JWT_PUBLIC_KEY: ${JWT_PUBLIC_KEY}
      APP_ENCRYPTION_KEY: ${ENCRYPTION_KEY}
      SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_ID: ${GOOGLE_CLIENT_ID}
      SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET: ${GOOGLE_CLIENT_SECRET}
      INTEGRATION_BITCOIN_BLOCKSTREAM_URL: https://blockstream.info/api
      INTEGRATION_COINGECKO_PRICE_URL: https://api.coingecko.com/api/v3/simple/price
      INTEGRATION_COINBASE_API_URL: https://api.coinbase.com
      INTEGRATION_COINBASE_API_VERSION: 2024-01-01
    volumes:
      - ./backend/src:/app/src:ro
    healthcheck:
      test: ["CMD-SHELL", "curl -sf http://localhost:8080/actuator/health || exit 1"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s

  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile.dev
    container_name: keybudget-ui
    restart: unless-stopped
    ports:
      - "5173:5173"
    depends_on:
      backend:
        condition: service_healthy
    environment:
      VITE_API_BASE_URL: http://localhost:8080
    volumes:
      - ./frontend:/app
      - /app/node_modules
    healthcheck:
      test: ["CMD-SHELL", "wget -qO- http://localhost:5173 || exit 1"]
      interval: 10s
      timeout: 3s
      retries: 5
      start_period: 10s

volumes:
  pgdata:
```

## `.env.example`

Create at project root — developers copy to `.env` and fill in real values.

```bash
# ===========================================
# KeyBudget — Local Development Environment
# Copy this file to .env and fill in values.
# NEVER commit .env to version control.
# ===========================================

# --- PostgreSQL ---
DB_USERNAME=keybudget
DB_PASSWORD=changeme

# --- Google OAuth2 ---
# Create at: https://console.cloud.google.com → APIs & Services → Credentials
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=

# --- JWT RSA Keys (base64-encoded) ---
# Generate with: ./scripts/generate-keys.sh
JWT_PRIVATE_KEY=
JWT_PUBLIC_KEY=

# --- AES-256 Encryption Key ---
# Generate with: openssl rand -base64 32
ENCRYPTION_KEY=
```

## .gitignore additions

```
.env
!.env.example
```

## Application Profile Strategy

When running under Docker Compose, the backend uses the `dev` profile but with **environment variable overrides** that swap H2 for PostgreSQL. This means:

- `application-dev.properties` remains untouched (H2 config for non-Docker local runs)
- Docker Compose env vars (`SPRING_DATASOURCE_URL`, etc.) take precedence over properties files per Spring Boot's externalized configuration order
- `ddl-auto` is set to `validate` inside Docker (Flyway manages schema — see migration strategy doc)

Alternatively, a dedicated `application-docker.properties` profile could be introduced if the override list grows unwieldy. For now, env-var overrides keep it simple.

## Startup

```bash
# First time — build images and start
docker compose up --build

# Subsequent runs
docker compose up

# Tear down (preserves pgdata volume)
docker compose down

# Tear down and destroy data
docker compose down -v
```

## Files to Create

| File | Purpose |
|------|---------|
| `docker-compose.yml` | Orchestration (project root) |
| `backend/Dockerfile.dev` | Backend dev image |
| `frontend/Dockerfile.dev` | Frontend dev image |
| `.env.example` | Template for secrets |
| `.dockerignore` (backend) | Exclude `target/`, `.idea/`, etc. |
| `.dockerignore` (frontend) | Exclude `node_modules/`, `dist/` |

## Open Questions

1. **Spring Boot DevTools vs. source mount:** The source volume mount gives the container latest source, but `spring-boot:run` needs recompilation. Consider adding `spring-boot-devtools` dependency (with `optional=true`) for automatic restart on classpath changes, or use `./mvnw compile` on the host with a class-files volume mount instead.
2. **Frontend prerequisite:** The frontend scaffold (PR #1) must be merged before the frontend container is viable. Until then, the frontend service can be commented out or marked with a Compose profile (`profiles: [full]`).
3. **Nginx reverse proxy:** Not included for local dev, but for production a `nginx` service should sit in front of both backend and frontend. That is out of scope for this local-dev design.
