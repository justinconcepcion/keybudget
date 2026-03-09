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

### 2. Configure secrets

Create `backend/.env` with your credentials:

```bash
export GOOGLE_CLIENT_ID=your-google-client-id
export GOOGLE_CLIENT_SECRET=your-google-client-secret
export JWT_PRIVATE_KEY=base64-encoded-rsa-private-key
export JWT_PUBLIC_KEY=base64-encoded-rsa-public-key
export ENCRYPTION_KEY=base64-encoded-32-byte-aes-key
```

Generate RSA and AES keys using the helper script:

```bash
bash scripts/generate-keys.sh
```

### 3. Start the backend

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

The API starts on **http://localhost:8080**.

### 4. Start the frontend

```bash
cd frontend
npm install
npm run dev
```

The dev server starts on **http://localhost:5173**.

> **Windows users:** You can also use `start-backend.bat` and `start-frontend.bat` from the project root.

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
└── scripts/                # Dev utility scripts (key generation)
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
