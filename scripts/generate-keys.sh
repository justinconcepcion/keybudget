#!/usr/bin/env bash
# Generate RSA key pair for JWT signing (KeyBudget)
# Usage: bash scripts/generate-keys.sh
# Output: base64-encoded values ready for JWT_PRIVATE_KEY and JWT_PUBLIC_KEY env vars

set -euo pipefail

TMPDIR=$(mktemp -d)
trap 'rm -rf "$TMPDIR"' EXIT

echo "Generating 2048-bit RSA key pair..."

# Generate PKCS#8 private key (required by Java's PKCS8EncodedKeySpec)
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$TMPDIR/private.pem" 2>/dev/null

# Extract public key in DER format
openssl pkey -in "$TMPDIR/private.pem" -pubout -out "$TMPDIR/public.pem" 2>/dev/null

# Convert to DER then base64 (no line wraps) for env vars
PRIVATE_B64=$(openssl pkey -in "$TMPDIR/private.pem" -outform DER 2>/dev/null | base64 -w0 2>/dev/null || openssl pkey -in "$TMPDIR/private.pem" -outform DER 2>/dev/null | base64)
PUBLIC_B64=$(openssl pkey -in "$TMPDIR/public.pem" -pubin -outform DER 2>/dev/null | base64 -w0 2>/dev/null || openssl pkey -in "$TMPDIR/public.pem" -pubin -outform DER 2>/dev/null | base64)

echo ""
echo "=== Add these to your .env or start-backend.bat ==="
echo ""
echo "JWT_PRIVATE_KEY=$PRIVATE_B64"
echo ""
echo "JWT_PUBLIC_KEY=$PUBLIC_B64"
echo ""
echo "Done. Keys are ephemeral — the PEM files have been deleted."
