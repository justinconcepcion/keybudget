# KeyBudget

A personal finance dashboard for tracking budgets, transactions, and net worth across financial accounts. Built with Spring Boot and Vue 3.

## Features

- **Google OAuth2 login** with RSA-signed JWT access and refresh tokens
- **Budget management** — create and track monthly budgets by category
- **Transaction tracking** — log, categorize, and review spending
- **Category management** — custom income and expense categories
- **Dashboard** — overview of financial activity
- **Responsive SPA** with Tailwind CSS styling

## Tech Stack

| Layer    | Technology                                                    |
|----------|---------------------------------------------------------------|
| Backend  | Java 21, Spring Boot 3.3.5, Spring Security, Spring Data JPA |
| Frontend | Vue 3, TypeScript, Vite, Pinia, Vue Router, Tailwind CSS     |
| Auth     | Google OAuth2 + JWT (JJWT 0.12.6, RSA-signed)                |
| Database | PostgreSQL (prod), H2 in-memory (dev)                         |
| Build    | Maven (backend), Vite (frontend)                              |
| CI       | GitHub Actions                                                |

## Prerequisites

- **Java 21** (Amazon Corretto recommended)
- **Node.js 18+** and npm
- **Git**
- **Google OAuth2 credentials** — create a project in [Google Cloud Console](https://console.cloud.google.com/) and configure an OAuth 2.0 client with `http://localhost:8080/login/oauth2/code/google` as an authorized redirect URI

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/justinconcepcion/keybudget.git
cd keybudget
```

### 2. Quick start (Unix / Git Bash)

The dev startup script auto-generates ephemeral RSA and AES keys, so you can start without any manual configuration. Google OAuth login will be disabled until you add credentials (see step 3).

```bash
# Terminal 1 — starts the backend with auto-generated keys
bash scripts/start-dev.sh

# Terminal 2 — starts the frontend dev server
cd frontend && npm install && npm run dev
```

### 2b. Quick start (Windows CMD)

Requires `backend\.env` with secrets configured (see step 3).

```cmd
REM Terminal 1
start-backend.bat

REM Terminal 2
start-frontend.bat
```

### 3. Configure secrets (for Google OAuth login)

Create `backend/.env` with your credentials:

```bash
export GOOGLE_CLIENT_ID=your-google-client-id
export GOOGLE_CLIENT_SECRET=your-google-client-secret
export JWT_PRIVATE_KEY=base64-encoded-rsa-private-key
export JWT_PUBLIC_KEY=base64-encoded-rsa-public-key
export ENCRYPTION_KEY=base64-encoded-32-byte-aes-key
```

Generate RSA and AES keys using the helper scripts:

```bash
bash scripts/generate-keys.sh         # outputs JWT_PRIVATE_KEY and JWT_PUBLIC_KEY values
openssl rand -base64 32               # generates ENCRYPTION_KEY value
```

To set up Google OAuth credentials, create a project in [Google Cloud Console](https://console.cloud.google.com/) → APIs & Services → Credentials → OAuth 2.0 Client ID, and add `http://localhost:8080/login/oauth2/code/google` as an authorized redirect URI.

> **Note:** If you use `scripts/start-dev.sh`, JWT and encryption keys are auto-generated ephemerally when not set in `.env`. You only need to manually generate keys if you want them to persist across restarts.

### Local dev URLs

| Service      | URL                                      |
|--------------|------------------------------------------|
| Frontend     | http://localhost:5173                     |
| Backend API  | http://localhost:8080                     |
| Health check | http://localhost:8080/actuator/health     |
| H2 Console   | http://localhost:8080/h2-console (dev)    |

The dev profile uses an **H2 in-memory database** — no database setup required. Data resets on each restart. The Vite dev server automatically proxies `/api/**` and `/oauth2/**` requests to the backend.

## Project Structure

```
keybudget/
├── backend/                # Spring Boot API
│   └── src/main/java/com/keybudget/
│       ├── auth/           # OAuth2 + JWT authentication
│       ├── user/           # User profile management
│       ├── budget/         # Budget tracking
│       ├── category/       # Income/expense categories
│       ├── transaction/    # Transaction management
│       ├── config/         # Security, CORS configuration
│       └── shared/         # Global error handling, DTOs
├── frontend/               # Vue 3 SPA
│   └── src/
│       ├── views/          # Dashboard, Budgets, Transactions, Settings
│       ├── components/     # Reusable UI components
│       ├── stores/         # Pinia stores (auth, budgets, categories, transactions)
│       ├── composables/    # Vue composables
│       ├── router/         # Vue Router with auth guard
│       ├── api/            # Axios HTTP layer with token refresh interceptor
│       └── types/          # TypeScript type definitions
├── scripts/                # Dev utility scripts
│   ├── start-dev.sh        # One-command backend startup (auto-generates keys)
│   └── generate-keys.sh    # RSA key pair generator for JWT signing
├── start-backend.bat       # Windows backend launcher (reads backend/.env)
└── start-frontend.bat      # Windows frontend launcher
```

## API Endpoints

All endpoints are prefixed with `/api/v1`.

### Auth

| Method | Endpoint        | Description          | Auth Required |
|--------|-----------------|----------------------|---------------|
| POST   | `/auth/refresh` | Refresh access token | Cookie        |
| POST   | `/auth/logout`  | Revoke refresh token | Cookie        |

### User

| Method | Endpoint    | Description          | Auth Required |
|--------|-------------|----------------------|---------------|
| GET    | `/users/me` | Current user profile | Yes           |

### Budgets, Categories, Transactions

Standard CRUD endpoints for managing budgets, categories, and transactions. All require authentication.

## Running Tests

### Backend

```bash
cd backend
./mvnw test                                    # all tests
./mvnw test -Dtest=AuthControllerTest          # single class
./mvnw test -Dtest="AuthControllerTest#method" # single method
```

### Frontend

```bash
cd frontend
npm run lint      # ESLint with auto-fix
npm run format    # Prettier formatting
npm run build     # TypeScript check + production build
```

## Development Profiles

| Profile | Database     | Secrets Source   | Use Case          |
|---------|--------------|------------------|-------------------|
| `dev`   | H2 in-memory | `backend/.env`   | Local development |
| `prod`  | PostgreSQL   | Environment vars | Production deploy |

## Roadmap

- [x] Google OAuth2 + JWT authentication
- [x] User profile management
- [x] Budget and category management
- [x] Transaction tracking
- [x] Vue 3 frontend with dashboard
- [ ] Financial account integrations (Coinbase, Bitcoin, M1 Finance, Marcus)
- [ ] Net-worth aggregation and history
- [ ] Scheduled balance syncing
- [ ] Docker Compose local dev stack
- [ ] Flyway database migrations
- [ ] Dark mode

## License

Private project. All rights reserved.
