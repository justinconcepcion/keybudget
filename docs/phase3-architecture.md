# Phase 3 — Financial Integrations Architecture

**Date:** 2026-03-09
**Status:** DRAFT

---

## Integration Strategy

| Service | API | Auth | Cost |
|---------|-----|------|------|
| **Coinbase** | Advanced Trade API v3 | API Key + HMAC-SHA256 | $0 (read-only) |
| **Bitcoin Wallet** | Blockstream Esplora | None | $0 |
| **M1 Finance** | Manual entry (no public API) | N/A | $0 |
| **Marcus by GS** | Manual entry (no public API) | N/A | $0 |

**Future:** Plaid for M1 + Marcus (~$3-5/mo per account). Investigate Teller free tier first.

## Implementation Priority

1. Bitcoin Wallet (simplest, no auth)
2. Coinbase (API key auth, real crypto data)
3. Marcus (manual, trivial)
4. M1 Finance (manual + stock price lookup)

## Key Decisions

### Data Strategy: Cache in DB
- External API calls add 200-2000ms latency per provider
- Cache balances in `FinancialAccountBalance`, serve from DB
- `asOf` timestamp communicates staleness to frontend
- 15-60 min staleness is acceptable for personal dashboard

### Sync Strategy: Three Tiers
1. **On-demand** — `POST /providers/{credentialId}/sync` (already implemented)
2. **Scheduled** — `@Scheduled` every 15min, API-backed providers only, virtual threads
3. **Webhooks** — not needed for Phase 3 (single user)

### Manual vs Auto-Sync Providers
Add `autoSyncSupported` boolean to `ProviderType` enum:
- `COINBASE(true)`, `BITCOIN_WALLET(true)`
- `M1_FINANCE(false)`, `MARCUS(false)`

New endpoint: `PUT /api/v1/integrations/accounts/{accountId}/balance` for manual providers.

### Resilience (No Circuit Breaker Library)
- Retry with backoff: 2 retries, exponential (1s, 2s) for 5xx only
- Do NOT retry 401 or 429
- Parse `Retry-After` header on 429
- `consecutiveFailures` counter on `IntegrationCredential` — skip after 5 failures
- Fallback: serve cached balance (already default behavior)
- Timeouts: 10s connect, 30s read

### Credential Security
- AES-256-GCM with random IV per operation (already implemented)
- Add `keyVersion` int to `IntegrationCredential` for key rotation
- Add `consecutiveFailures` int for provider health tracking
- Validate credential map keys match expected keys per `ProviderType`
- Reject Bitcoin credentials containing `privateKey` or `wif`
- Prod: migrate encryption key to AWS Secrets Manager (~$0.40/mo)

## New Shared Services

### CryptoPriceService
- Interface: `BigDecimal getUsdPrice(String cryptoTicker)`
- Implementation: CoinGecko free API (or reuse Coinbase ticker)
- Cache: Caffeine, 60s TTL
- Shared by `CoinbaseProvider` and `BitcoinWalletProvider`

### StockPriceService
- Interface: `Map<String, BigDecimal> getUsdPrices(List<String> tickers)`
- Implementation: Alpha Vantage free tier (25 req/day)
- Cache: Caffeine, 5min TTL
- Used by `M1FinanceProvider` (manual holdings)

### ScheduledSyncJob
- `@Scheduled(fixedRate)` with configurable interval
- Query active credentials, filter to auto-sync providers
- Use `Executors.newVirtualThreadPerTaskExecutor()` for parallel I/O
- Each provider sync wrapped in try-catch (one failure doesn't block others)
- `@ConditionalOnProperty(name = "app.sync.enabled")` to disable in tests

## Package Structure (New Components)

```
com.keybudget.integration/
  provider/
    coinbase/
      CoinbaseApiClient.java          (NEW — WebClient/RestClient wrapper)
    bitcoin/
      BlockstreamApiClient.java       (NEW)
  price/                              (NEW package)
    CryptoPriceService.java           (interface)
    CoinGeckoPriceService.java        (cached implementation)
    StockPriceService.java            (interface)
    AlphaVantagePriceService.java     (cached implementation)
  sync/                               (NEW package)
    ScheduledSyncJob.java
  dto/
    UpdateBalanceRequest.java         (NEW)
```

## API Contract

### Existing Endpoints (no changes)
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/integrations/connect` | Connect provider |
| GET | `/api/v1/integrations/accounts` | List accounts |
| GET | `/api/v1/integrations/providers` | Provider statuses |
| DELETE | `/api/v1/integrations/providers/{id}` | Disconnect |
| POST | `/api/v1/integrations/providers/{id}/sync` | Manual sync |
| GET | `/api/v1/integrations/net-worth` | Current net worth |
| GET | `/api/v1/integrations/net-worth/history` | History |

### New Endpoint
```
PUT /api/v1/integrations/accounts/{accountId}/balance
Body: { "balance": 25432.50, "balanceUsd": 25432.50 }
Returns: AccountResponse (200) | 400 if auto-sync provider | 404 if not found
```

### Credential Keys by Provider
| Provider | Keys | Example |
|----------|------|---------|
| COINBASE | `apiKey`, `apiSecret` | `{"apiKey":"abc","apiSecret":"xyz"}` |
| BITCOIN_WALLET | `address` | `{"address":"bc1q..."}` |
| M1_FINANCE | `accountName`, `holdings` | `{"accountName":"Brokerage","holdings":"[{\"ticker\":\"VOO\",\"shares\":10.5}]"}` |
| MARCUS | `accountName`, `balance` | `{"accountName":"Marcus HYSA","balance":"25432.50"}` |

## Entity Changes

### IntegrationCredential
- Add `consecutiveFailures` (int, default 0)
- Add `keyVersion` (int, default 1)

### ProviderType enum
- Add `autoSyncSupported` boolean property

### ProviderStatusResponse
- Add `autoSyncSupported` boolean field

## Provider Implementation Notes

### Coinbase
- No official Java SDK — use Spring `RestClient` with HMAC-SHA256 signing
- `GET /api/v3/brokerage/accounts` for balances
- Accounts are per-currency wallets; iterate all to build portfolio
- Reject API keys with trade permissions (read-only only)
- Rate limit: 10 req/sec

### Bitcoin Wallet
- Blockstream: `GET /api/address/{address}`
- Balance = `chain_stats.funded_txo_sum - chain_stats.spent_txo_sum` (satoshis / 1e8)
- Supports Legacy (1...), SegWit (3...), Native SegWit (bc1...)
- For full HD wallet, user must provide xpub
- Self-imposed rate limit: 1 req/sec

### M1 Finance (Manual)
- `connect()`: parse holdings JSON from credential map
- `syncBalances()`: fetch stock prices via `StockPriceService`, recalculate
- Upgrade path: swap to Plaid Investments product later

### Marcus (Manual)
- `connect()`: create account from user-supplied name + balance
- `syncBalances()`: return stored balance (no external call)
- Upgrade path: swap to Plaid Balance product later

## Risks

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| External API instability | Medium | Medium | Cached data + asOf timestamp |
| Coinbase API key over-scoped | Low | High | Reject keys with trade permissions |
| Encryption key compromise | Low | Critical | Secrets Manager + key versioning |
| Snapshot table growth (~140k/yr) | High | Low | Retention policy in Phase 4+ |
| Plaid cost surprise | Medium | Medium | Flagged; manual entry first |

## Configuration Properties
```properties
app.sync.interval-minutes=15
app.sync.min-age-minutes=15
app.sync.enabled=true
```
