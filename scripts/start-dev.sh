#!/usr/bin/env bash
# Start KeyBudget backend in dev mode with auto-generated crypto keys.
# Google OAuth credentials must be set in backend/.env or as env vars.
#
# Usage: bash scripts/start-dev.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
BACKEND_DIR="$PROJECT_DIR/backend"
ENV_FILE="$BACKEND_DIR/.env"

export JAVA_HOME="${JAVA_HOME:-T:/Java/jdk21.0.10_7}"

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
    TMPDIR_KEYS=$(mktemp -d)
    trap 'rm -rf "$TMPDIR_KEYS"' EXIT

    openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$TMPDIR_KEYS/private.pem" 2>/dev/null
    openssl pkey -in "$TMPDIR_KEYS/private.pem" -pubout -out "$TMPDIR_KEYS/public.pem" 2>/dev/null

    export JWT_PRIVATE_KEY=$(openssl pkey -in "$TMPDIR_KEYS/private.pem" -outform DER 2>/dev/null | base64 -w0 2>/dev/null || openssl pkey -in "$TMPDIR_KEYS/private.pem" -outform DER 2>/dev/null | base64)
    export JWT_PUBLIC_KEY=$(openssl pkey -in "$TMPDIR_KEYS/public.pem" -pubin -outform DER 2>/dev/null | base64 -w0 2>/dev/null || openssl pkey -in "$TMPDIR_KEYS/public.pem" -pubin -outform DER 2>/dev/null | base64)
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
    echo "    export GOOGLE_CLIENT_ID=your-client-id"
    echo "    export GOOGLE_CLIENT_SECRET=your-client-secret"
    echo ""
    # Set dummy values so Spring Boot doesn't fail on missing placeholders
    export GOOGLE_CLIENT_ID="${GOOGLE_CLIENT_ID:-not-configured}"
    export GOOGLE_CLIENT_SECRET="${GOOGLE_CLIENT_SECRET:-not-configured}"
fi

echo ""
echo "Starting KeyBudget backend (dev profile)..."
echo "  Backend:  http://localhost:8080"
echo "  Frontend: http://localhost:5173 (run 'npm run dev' in frontend/)"
echo "  Health:   http://localhost:8080/actuator/health"
echo ""

cd "$BACKEND_DIR"
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
