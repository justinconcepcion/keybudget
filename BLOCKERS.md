# BLOCKERS — Read Before Continuing Work

**Filed:** 2026-03-09
**Issue:** #27 — https://github.com/justinconcepcion/keybudget/issues/27

---

## STOP: PR #1 must be merged before Phase 3 can continue

The `feat/phase3-financial-integrations` branch diverged from `master` WITHOUT merging PR #1 (frontend scaffold). The frontend directory is incomplete — no `package.json`, no components, no build config.

### Required Actions (in order)

1. **Merge PR #1** (`feat/frontend-scaffolding`) into `master`
2. **Rebase this branch** onto `master` to pick up frontend code
3. **Create `scripts/generate-keys.sh`** — RSA keypair + AES key for dev
4. **Fix `application-dev.properties`** — replace all `REPLACE_ME` placeholders
5. **Add README.md** with local setup instructions
6. **Verify local run** — backend starts, frontend builds, OAuth flow works

### Architecture Docs Ready

Phase 3 architecture and API research are already prepared:
- `docs/phase3-architecture.md` — full blueprint (patterns, data models, API contracts)
- `docs/api-research.md` — Coinbase, M1, Marcus, Bitcoin API availability and costs

### Key Findings from Research
- M1 Finance and Marcus have NO public APIs — use manual entry for Phase 3, Plaid later
- Coinbase: direct API with API key + HMAC signing ($0)
- Bitcoin: Blockstream Esplora API, no auth needed ($0)
- All Phase 3 integrations can be $0/month
