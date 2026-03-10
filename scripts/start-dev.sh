#!/usr/bin/env bash
# Start KeyBudget backend + frontend in dev mode.
# Auto-generates ephemeral crypto keys if not configured.
# Google OAuth credentials must be set in backend/.env or as env vars.
#
# Usage: bash scripts/start-dev.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
BACKEND_DIR="$PROJECT_DIR/backend"
FRONTEND_DIR="$PROJECT_DIR/frontend"
ENV_FILE="$BACKEND_DIR/.env"

export JAVA_HOME="${JAVA_HOME:-T:/Java/jdk21.0.10_7}"

# Track child PIDs for cleanup on Ctrl+C
BACKEND_PID=""
FRONTEND_PID=""
TMPDIR_KEYS=""
cleanup() {
    echo ""
    echo "Shutting down..."
    [ -n "$FRONTEND_PID" ] && kill "$FRONTEND_PID" 2>/dev/null || true
    [ -n "$BACKEND_PID" ] && kill "$BACKEND_PID" 2>/dev/null || true
    [ -n "$TMPDIR_KEYS" ] && rm -rf "$TMPDIR_KEYS"
    wait "$FRONTEND_PID" "$BACKEND_PID" 2>/dev/null || true
    echo "Stopped."
}
trap cleanup INT TERM

# Load existing .env if present
if [ -f "$ENV_FILE" ]; then
    echo "Loading secrets from backend/.env"
    set -a
    source "$ENV_FILE"
    set +a
fi

# Auto-generate JWT keys if not set
if [ -z "${JWT_PRIVATE_KEY:-}" ] || [ -z "${JWT_PUBLIC_KEY:-}" ]; then
    echo "JWT keys not set — generating ephemeral RSA keypair..."
    TMPDIR_KEYS=$(mktemp -d) || { echo "ERROR: Failed to create temp dir"; exit 1; }

    openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$TMPDIR_KEYS/private.pem" 2>/dev/null
    openssl pkey -in "$TMPDIR_KEYS/private.pem" -pubout -out "$TMPDIR_KEYS/public.pem" 2>/dev/null

    export JWT_PRIVATE_KEY=$(openssl pkey -in "$TMPDIR_KEYS/private.pem" -outform DER 2>/dev/null | base64 | tr -d '\n')
    export JWT_PUBLIC_KEY=$(openssl pkey -in "$TMPDIR_KEYS/public.pem" -pubin -outform DER 2>/dev/null | base64 | tr -d '\n')
    echo "JWT keys generated (ephemeral — will be lost on restart)"
fi

# Auto-generate encryption key if not set
if [ -z "${ENCRYPTION_KEY:-}" ]; then
    echo "Encryption key not set — generating ephemeral AES-256 key..."
    export ENCRYPTION_KEY=$(openssl rand -base64 32)
    echo "Encryption key generated (ephemeral — will be lost on restart)"
fi

# Check Google OAuth
if [ -z "${GOOGLE_CLIENT_ID:-}" ] || [ "${GOOGLE_CLIENT_ID:-}" = "not-configured" ]; then
    echo ""
    echo "WARNING: GOOGLE_CLIENT_ID is not set."
    echo "  Google OAuth login will not work, but the app will start."
    echo "  To enable login, create credentials at:"
    echo "  https://console.cloud.google.com -> APIs & Services -> Credentials"
    echo "  Then add to backend/.env:"
    echo "    GOOGLE_CLIENT_ID=your-client-id"
    echo "    GOOGLE_CLIENT_SECRET=your-client-secret"
    echo ""
    # Set dummy values so Spring Boot doesn't fail on missing placeholders
    export GOOGLE_CLIENT_ID="${GOOGLE_CLIENT_ID:-not-configured}"
    export GOOGLE_CLIENT_SECRET="${GOOGLE_CLIENT_SECRET:-not-configured}"
fi

echo ""
echo "Starting KeyBudget..."
echo "  Backend:  http://localhost:8080"
echo "  Frontend: http://localhost:5173"
echo "  Health:   http://localhost:8080/actuator/health"
echo "  Press Ctrl+C to stop both servers"
echo ""

# Install frontend deps if needed
if [ ! -d "$FRONTEND_DIR/node_modules" ]; then
    echo "Installing frontend dependencies..."
    (cd "$FRONTEND_DIR" && npm install) || { echo "ERROR: npm install failed"; exit 1; }
fi

# Start frontend in background
cd "$FRONTEND_DIR"
npx vite --host &
FRONTEND_PID=$!

# Start backend in background
cd "$BACKEND_DIR"
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev &
BACKEND_PID=$!

echo "Frontend PID: $FRONTEND_PID | Backend PID: $BACKEND_PID"

# Wait for both — if either exits, cleanup runs on next signal or script ends
wait
