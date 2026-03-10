#!/usr/bin/env bash
# Generate all cryptographic keys needed for KeyBudget deployment.
# Usage: bash scripts/generate-keys.sh
# Output: values ready to paste into .env.prod

set -euo pipefail

TMPDIR=$(mktemp -d)
trap 'rm -rf "$TMPDIR"' EXIT

echo "=== KeyBudget Key Generator ==="
echo ""
echo "Generating 2048-bit RSA key pair for JWT signing..."

# Generate PKCS#8 private key (required by Java's PKCS8EncodedKeySpec)
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$TMPDIR/private.pem" 2>/dev/null

# Extract public key in DER format
openssl pkey -in "$TMPDIR/private.pem" -pubout -out "$TMPDIR/public.pem" 2>/dev/null

# Convert to DER then base64 (no line wraps) for env vars
PRIVATE_B64=$(openssl pkey -in "$TMPDIR/private.pem" -outform DER 2>/dev/null | base64 -w0 2>/dev/null || openssl pkey -in "$TMPDIR/private.pem" -outform DER 2>/dev/null | base64)
PUBLIC_B64=$(openssl pkey -in "$TMPDIR/public.pem" -pubin -outform DER 2>/dev/null | base64 -w0 2>/dev/null || openssl pkey -in "$TMPDIR/public.pem" -pubin -outform DER 2>/dev/null | base64)

# AES-256 key for credential encryption
ENCRYPTION_KEY=$(openssl rand -base64 32)

# Strong database password (32 chars, URL-safe)
DB_PASSWORD=$(openssl rand -base64 32 | tr -d '/+=\n' | head -c 32)

echo ""
echo "# --- Paste these into your .env.prod ---"
echo ""
echo "JWT_PRIVATE_KEY=$PRIVATE_B64"
echo ""
echo "JWT_PUBLIC_KEY=$PUBLIC_B64"
echo ""
echo "ENCRYPTION_KEY=$ENCRYPTION_KEY"
echo ""
echo "DB_PASSWORD=$DB_PASSWORD"
echo ""
echo "=== Done. Store these securely — they cannot be recovered. ==="
