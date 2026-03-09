# Financial API Research — Phase 3

**Date:** 2026-03-09

---

## 1. Coinbase

- **API:** Advanced Trade API v3 (REST)
- **Auth:** API Key + HMAC-SHA256 signed requests (simpler than OAuth for personal use)
- **Data:** Account balances, transaction history, prices, portfolio value, order history
- **Rate limits:** 10,000 req/hr (v2), 10 req/sec (Advanced Trade)
- **Cost:** $0 for read-only access
- **Java SDK:** None official — use Spring RestClient with manual HMAC signing
- **Gotchas:**
  - Aggressive deprecation cycle (Pro API → Advanced Trade)
  - Accounts are per-currency wallets, not a single portfolio
  - Staking/earn products may not appear in standard listings
  - No cost basis / gain-loss data via API

## 2. M1 Finance

- **API:** None public. Internal GraphQL API exists but is undocumented and TOS-violating.
- **Alternatives:**
  - **Plaid Investments** (recommended for future) — M1 is Plaid-supported
  - Manual entry (recommended for Phase 3)
  - CSV export (not automatable)

## 3. Marcus by Goldman Sachs

- **API:** None public. GS has institutional APIs but not for consumer savings.
- **Alternatives:**
  - **Plaid** (recommended for future) — Marcus is Plaid-supported
  - Manual entry (recommended for Phase 3)

## 4. Bitcoin Wallet (Public Blockchain)

- **APIs (all free, no auth):**

| Provider | Rate Limits | Notes |
|----------|-------------|-------|
| Blockstream Esplora | ~5 req/sec | Recommended, open source |
| Mempool.space | Generous | Self-hostable |
| Blockchain.com | ~5 req/sec | Legacy |
| BlockCypher | 3 req/sec free | Paid plans from ~$75/mo |

- **Data:** Balance, full transaction history, UTXO set
- **USD price:** CoinGecko free tier (10-30 calls/min, no key)
- **Java:** bitcoinj exists but heavyweight; Spring RestClient is simpler
- **Gotchas:**
  - Single address ≠ full wallet; need xpub for HD wallets
  - Multiple address formats (Legacy/SegWit/Bech32)
  - Privacy: querying third-party API reveals holdings to provider

## 5. Plaid (Unified Alternative)

- **Coverage:** ~12,000 institutions (M1 ✓, Marcus ✓, Coinbase ✗ for crypto)
- **Auth:** client_id + secret (server), Plaid Link widget (client)
- **Products:** Transactions, Balance, Investments, Liabilities
- **Java SDK:** Official `plaid-java` on Maven Central
- **Cost:**
  - Sandbox: Free (100 live Items)
  - Production: ~$0.30/item/mo (Transactions), ~$1+/item/mo (Investments)
  - Personal project ~3-5 accounts: **$1-5/month**
  - Requires production approval (may reject hobby projects)
- **Gotchas:**
  - Connections break periodically (re-auth via Plaid Link)
  - Transaction descriptions are messy
  - Investments: no historical performance, only point-in-time snapshots

## 6. Plaid Alternatives

| Provider | Free Tier | Coverage | Notes |
|----------|-----------|----------|-------|
| **Teller** | 1 user unlimited accounts | ~5,000 institutions | Best for personal dashboard |
| **Akoya** | Unknown | Growing | FDX standard, institution-funded |
| **MX** | Enterprise pricing | Large | Better categorization than Plaid |
| **SimpleFIN** | Free | Very small | Community-driven, not reliable |

**Recommendation:** Investigate Teller first (free single-user tier). Fall back to Plaid if coverage gaps.

## Cost Summary

| Scenario | Monthly Cost |
|----------|-------------|
| Crypto only (Coinbase + BTC) | $0 |
| Full dashboard via Plaid | $1-5/mo |
| Full dashboard via Teller | $0 (if institutions supported) |
