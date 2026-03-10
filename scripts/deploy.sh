#!/usr/bin/env bash
# Deploy KeyBudget to production using Docker Compose.
#
# Prerequisites:
#   - Docker & Docker Compose installed
#   - .env.prod filled in (copy from .env.prod.example)
#   - TLS certs at nginx/certs/fullchain.pem + privkey.pem
#
# Usage:
#   bash scripts/deploy.sh          # Build and start
#   bash scripts/deploy.sh stop     # Stop all services
#   bash scripts/deploy.sh logs     # Tail logs
#   bash scripts/deploy.sh restart  # Rebuild and restart

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
ENV_FILE="$PROJECT_DIR/.env.prod"
COMPOSE_FILE="$PROJECT_DIR/docker-compose.prod.yml"

cd "$PROJECT_DIR"

# --- Helpers ---
red()   { echo -e "\033[0;31m$1\033[0m"; }
green() { echo -e "\033[0;32m$1\033[0m"; }

check_prereqs() {
    if ! command -v docker &>/dev/null; then
        red "ERROR: docker is not installed."
        exit 1
    fi

    if ! docker compose version &>/dev/null; then
        red "ERROR: Docker Compose is not available."
        exit 1
    fi

    if [ ! -f "$ENV_FILE" ]; then
        red "ERROR: .env.prod not found."
        echo "  Copy .env.prod.example to .env.prod and fill in values."
        echo "  Generate keys with: bash scripts/generate-keys.sh"
        exit 1
    fi

    if [ ! -f "$PROJECT_DIR/nginx/certs/fullchain.pem" ]; then
        red "ERROR: TLS certificate not found at nginx/certs/fullchain.pem"
        echo ""
        echo "  Option 1 — Self-signed cert for testing:"
        echo "    mkdir -p nginx/certs"
        echo "    openssl req -x509 -nodes -days 365 -newkey rsa:2048 \\"
        echo "      -keyout nginx/certs/privkey.pem \\"
        echo "      -out nginx/certs/fullchain.pem \\"
        echo "      -subj '/CN=localhost'"
        echo ""
        echo "  Option 2 — Let's Encrypt (requires domain + port 80 open):"
        echo "    sudo certbot certonly --standalone -d YOUR_DOMAIN"
        echo "    cp /etc/letsencrypt/live/YOUR_DOMAIN/fullchain.pem nginx/certs/"
        echo "    cp /etc/letsencrypt/live/YOUR_DOMAIN/privkey.pem nginx/certs/"
        exit 1
    fi

    # Check required env vars
    set -a
    source "$ENV_FILE"
    set +a

    MISSING=""
    [ -z "${DB_USERNAME:-}" ] && MISSING="$MISSING DB_USERNAME"
    [ -z "${DB_PASSWORD:-}" ] && MISSING="$MISSING DB_PASSWORD"
    [ -z "${GOOGLE_CLIENT_ID:-}" ] && MISSING="$MISSING GOOGLE_CLIENT_ID"
    [ -z "${GOOGLE_CLIENT_SECRET:-}" ] && MISSING="$MISSING GOOGLE_CLIENT_SECRET"
    [ -z "${JWT_PRIVATE_KEY:-}" ] && MISSING="$MISSING JWT_PRIVATE_KEY"
    [ -z "${JWT_PUBLIC_KEY:-}" ] && MISSING="$MISSING JWT_PUBLIC_KEY"
    [ -z "${ENCRYPTION_KEY:-}" ] && MISSING="$MISSING ENCRYPTION_KEY"
    [ -z "${FRONTEND_URL:-}" ] && MISSING="$MISSING FRONTEND_URL"

    if [ -n "$MISSING" ]; then
        red "ERROR: Missing required env vars in .env.prod:"
        echo "  $MISSING"
        exit 1
    fi
}

# --- Commands ---
case "${1:-start}" in
    start)
        check_prereqs
        green "Building and starting KeyBudget..."
        docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up --build -d
        echo ""
        green "KeyBudget is starting up."
        echo "  Frontend: ${FRONTEND_URL:-https://localhost}"
        echo "  Health:   curl -k https://localhost/actuator/health"
        echo "  Logs:     bash scripts/deploy.sh logs"
        echo "  Stop:     bash scripts/deploy.sh stop"
        ;;

    stop)
        green "Stopping KeyBudget..."
        docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" down
        green "Stopped."
        ;;

    restart)
        check_prereqs
        green "Rebuilding and restarting KeyBudget..."
        docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" down
        docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up --build -d
        green "Restarted."
        ;;

    logs)
        docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" logs -f --tail=100
        ;;

    status)
        docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" ps
        ;;

    *)
        echo "Usage: bash scripts/deploy.sh [start|stop|restart|logs|status]"
        exit 1
        ;;
esac
