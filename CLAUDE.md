# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

KeyBudget — personal finance management app. Monorepo with a Spring Boot backend and Vue 3 frontend.

## Build & Dev Commands

### Backend (from `backend/`)
```bash
./mvnw test                          # run all tests
./mvnw test -Dtest=AuthControllerTest # run a single test class
./mvnw test -Dtest="AuthControllerTest#testMethodName" # single test method
./mvnw package                       # build JAR (includes tests)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev  # run locally
```

### Frontend (from `frontend/`)
```bash
npm run dev       # Vite dev server on :5173
npm run build     # TypeScript check + Vite production build
npm run lint      # ESLint with auto-fix
npm run format    # Prettier formatting
```

## Architecture

### Backend — Spring Boot 3.3.5 / Java 21
- **Package structure**: `com.keybudget.{auth, user, shared, config}`
- **Auth flow**: Google OAuth2 → backend issues RSA-signed JWT access token (15min) + refresh token (7-day HttpOnly cookie). Access token passed to frontend via URL fragment on redirect.
- **Refresh tokens**: Persisted as `RefreshToken` entity, tracked by JTI for revocation. `RefreshTokenService` handles creation/validation/revocation.
- **Security**: Stateless sessions, CORS locked to frontend origin, `SecurityConfig` defines public/protected paths.
- **Profiles**: `dev` (H2 in-memory, embedded secrets), `prod` (PostgreSQL, env vars for secrets).
- **API prefix**: `/api/v1`

### Frontend — Vue 3 + TypeScript + Vite
- **State**: Pinia — `useAuthStore` keeps access token in-memory only (XSS prevention).
- **HTTP**: Axios with 401 interceptor that queues requests during token refresh. Separate raw axios instance for auth endpoints to avoid refresh loops.
- **Routing**: Vue Router with auth guard. Public: `/login`, `/auth/callback`. Protected routes use `AppShell` layout.
- **Styling**: Tailwind CSS with custom green primary palette.

### Key Auth Endpoints
- `POST /api/v1/auth/refresh` — cookie-based refresh, returns new access token
- `POST /api/v1/auth/logout` — revokes refresh token
- `GET /api/v1/users/me` — current user profile

## Feature Roadmap (Priority Order)

1. **Fix local dev blockers** — secure cookie flag, RSA key script, default profile
2. **Core usability** — transaction edit/delete, category edit/delete, shared utilities
3. **Financial integrations (top priority feature)**
   - Coinbase API — crypto portfolio balances & transaction history
   - M1 Finance API — brokerage holdings & performance
   - Marcus by Goldman Sachs — high-yield savings account balance
   - Bitcoin wallet — private key or watch-only address for BTC holdings
   - Goal: unified dashboard showing holistic net worth across all accounts
4. **Production readiness** — Flyway, Docker, frontend tests
5. **Future** — dark mode, multi-currency, budget alerts, bank CSV import

## Conventions

- **Frontend formatting**: Prettier (printWidth: 100, trailingComma: all, vueIndentScriptAndStyle: true)
- **Frontend linting**: ESLint flat config with TypeScript + Vue rules
- **Backend**: Standard Spring Boot conventions, Java records for DTOs
- **Error handling**: `GlobalExceptionHandler` with `ErrorResponse` DTO
- **CI**: GitHub Actions runs Maven tests on backend changes in PRs
