# KeyBudget Sprint Plan

> Generated: 2026-03-09
> Repo: justinconcepcion/keybudget

---

## Issue Inventory

### Open Issues

| # | Title | Milestone | Labels | State |
|---|-------|-----------|--------|-------|
| 27 | BLOCKER: Merge PR #1 and fix local dev setup | Phase 1: Local Dev Blockers | blocked, high-priority | open |
| 9 | Add transaction edit/delete UI | Phase 2: Core Usability | frontend | open |
| 10 | Add category management UI | Phase 2: Core Usability | frontend | open |
| 12 | Coinbase API integration | Phase 3: Financial Integrations | backend, integration, high-priority | open |
| 13 | M1 Finance API integration | Phase 3: Financial Integrations | backend, integration, high-priority | open |
| 14 | Marcus by Goldman Sachs integration | Phase 3: Financial Integrations | backend, integration, high-priority | open |
| 15 | Bitcoin wallet integration | Phase 3: Financial Integrations | backend, integration, high-priority | open |
| 16 | Unified net worth dashboard | Phase 3: Financial Integrations | frontend, integration, high-priority | open |
| 17 | Financial accounts overview page | Phase 3: Financial Integrations | frontend, integration | open |
| 18 | Add Flyway database migrations | Phase 4: Production Readiness | backend, devops | open |
| 19 | Create Docker Compose setup | Phase 4: Production Readiness | devops | open |
| 20 | Add frontend unit tests with Vitest | Phase 4: Production Readiness | frontend | open |
| 21 | Add frontend E2E tests | Phase 4: Production Readiness | frontend | open |
| 22 | Dark mode support | Phase 5: Future Enhancements | frontend | open |
| 23 | Multi-currency support | Phase 5: Future Enhancements | backend, frontend | open |
| 24 | Budget alerts and notifications | Phase 5: Future Enhancements | backend, frontend | open |
| 25 | Bank CSV import | Phase 5: Future Enhancements | backend, frontend | open |

### Closed Issues (for reference)

| # | Title | Milestone |
|---|-------|-----------|
| 2 | Fix secure cookie flag for local development | Phase 1 |
| 3 | Add RSA key generation script for JWT signing | Phase 1 |
| 4 | Set default Spring profile to dev | Phase 1 |
| 5 | Add transaction edit endpoint | Phase 2 |
| 6 | Add transaction delete endpoint | Phase 2 |
| 7 | Add category edit endpoint | Phase 2 |
| 8 | Add category delete endpoint | Phase 2 |
| 11 | Extract shared utility functions | Phase 2 |

### Open PRs

| # | Title | State |
|---|-------|-------|
| 1 | feat: scaffold Vue 3 frontend with auth and layout | open |

---

## 1. Milestone Assignment Update

**Issue #27** was missing a milestone. It has been assigned to **Phase 1: Local Dev Blockers** (milestone #1), since it is a local development blocker that must be resolved before any other work can proceed.

---

## 2. Sub-Task Breakdown (Issues That Need Splitting)

The following issues are too large for a single PR and should be broken into smaller, reviewable pieces.

### #12 — Coinbase API Integration (recommend 3-4 PRs)

| Sub-task | Scope | Est. |
|----------|-------|------|
| 12a: Coinbase OAuth2 client setup | Add Coinbase OAuth2 flow, store tokens, handle refresh. Config properties for API key/secret. | S |
| 12b: Coinbase account balances | Fetch all crypto account balances via Coinbase API. Map to KeyBudget account model. Cache responses. | M |
| 12c: Coinbase transaction history | Fetch recent transactions per account. Map to KeyBudget transaction model. Pagination support. | M |
| 12d: Coinbase sync scheduler + error handling | Scheduled background sync. Rate limit handling. Error logging and retry logic. | S |

**Cost flag:** Coinbase API is free for read-only portfolio access, but requires a Coinbase developer account.

### #13 — M1 Finance API Integration (recommend 3 PRs)

| Sub-task | Scope | Est. |
|----------|-------|------|
| 13a: M1 Finance API research + auth | Research API availability (public vs. unofficial vs. Plaid). Implement auth flow. | S |
| 13b: M1 portfolio holdings | Fetch holdings and current values. Map to KeyBudget account model. | M |
| 13c: M1 performance metrics + caching | Fetch gain/loss data. Background sync with caching. | S |

**Cost flag:** M1 Finance has no public API. May require Plaid ($0-$500/mo depending on tier) or unofficial scraping approach. Research needed before implementation.

### #14 — Marcus by Goldman Sachs Integration (recommend 2-3 PRs)

| Sub-task | Scope | Est. |
|----------|-------|------|
| 14a: Marcus API research + Plaid setup | Research access options. Set up Plaid if needed (Link flow, token exchange). | M |
| 14b: Marcus balance + interest fetching | Fetch current balance and recent interest earned. Map to account model. Cache data. | S |
| 14c: Plaid sync management | Webhook handling for balance updates. Error handling for disconnected accounts. | S |

**Cost flag:** Almost certainly requires Plaid. Plaid pricing: free tier available (100 connections in sandbox), Production pricing is per-connection. Flag to user before implementing.

### #15 — Bitcoin Wallet Integration (recommend 2 PRs)

| Sub-task | Scope | Est. |
|----------|-------|------|
| 15a: BTC address balance via public API | Fetch BTC balance from a watch-only address using a public blockchain API (Blockchain.info, Blockstream). Map to account model. | S |
| 15b: BTC price conversion + history | Fetch BTC/USD price. Calculate USD value. Historical balance tracking. Cache/schedule. | S |

**Note:** No cost — uses public blockchain APIs. No private keys needed (watch-only).

### #16 — Unified Net Worth Dashboard (recommend 3 PRs)

| Sub-task | Scope | Est. |
|----------|-------|------|
| 16a: Net worth aggregation API endpoint | Backend endpoint that aggregates all connected account balances into a single response. | M |
| 16b: Dashboard UI — totals and breakdown | Vue page showing total net worth, breakdown by account type (crypto, brokerage, savings). | M |
| 16c: Dashboard charts + history | Pie/bar chart for asset allocation (Chart.js or similar). Net worth over time line chart. | M |

### #17 — Financial Accounts Overview Page (recommend 2 PRs)

| Sub-task | Scope | Est. |
|----------|-------|------|
| 17a: Account list + status display | Vue page listing connected accounts with type, name, sync status, last sync time. | M |
| 17b: Add/remove account flow + manual sync | Add-account wizard (select type, enter credentials/address). Remove with confirmation. Manual sync button. | M |

### #19 — Docker Compose Setup (recommend 2 PRs)

| Sub-task | Scope | Est. |
|----------|-------|------|
| 19a: Dockerfiles for backend + frontend | Dockerfile for Spring Boot (multi-stage). Dockerfile for Vue (nginx). | S |
| 19b: Docker Compose orchestration | Compose file with backend, frontend, PostgreSQL. Env config. Health checks. Volumes. | M |

### Issues That Are Fine As-Is (single PR each)

- **#9** — Transaction edit/delete UI: Well-scoped, single page change.
- **#10** — Category management UI: Well-scoped, single page change.
- **#18** — Flyway migrations: Focused DB migration task.
- **#20** — Vitest setup: Test infrastructure + initial tests.
- **#21** — E2E tests: Test infrastructure + initial tests.
- **#22** — Dark mode: CSS/theming change.
- **#23** — Multi-currency: Could be split later, but scope is unclear until Phase 3 is done.
- **#24** — Budget alerts: Could be split later, Phase 5 item.
- **#25** — Bank CSV import: Could be split later, Phase 5 item.

---

## 3. Dependency Map

```
PR #1 (frontend scaffold)
  |
  v
#27 (BLOCKER: merge PR #1, fix dev setup)
  |
  +---> #9  (transaction edit/delete UI)        [Phase 2]
  +---> #10 (category management UI)            [Phase 2]
  |
  +---> #12 (Coinbase integration)              [Phase 3 backend]
  |       |
  +---> #13 (M1 Finance integration)            [Phase 3 backend]
  |       |
  +---> #14 (Marcus integration)                [Phase 3 backend]
  |       |
  +---> #15 (Bitcoin wallet integration)        [Phase 3 backend]
  |       |
  |       +----> #17 (accounts overview page)   [Phase 3 frontend]
  |       |        \-- needs at least 1 backend integration done
  |       |
  |       +----> #16 (net worth dashboard)      [Phase 3 frontend]
  |                \-- needs all backend integrations done
  |
  +---> #18 (Flyway migrations)                 [Phase 4, independent]
  +---> #19 (Docker Compose)                    [Phase 4, after #18]
  +---> #20 (Vitest)                            [Phase 4, after #9/#10]
  +---> #21 (E2E tests)                         [Phase 4, after #20]
  |
  +---> #22 (dark mode)                         [Phase 5, independent]
  +---> #23 (multi-currency)                    [Phase 5, after Phase 3]
  +---> #24 (budget alerts)                     [Phase 5, after Phase 2]
  +---> #25 (bank CSV import)                   [Phase 5, independent]
```

### Explicit Blocking Relationships

| Blocked Issue | Blocked By |
|---------------|------------|
| Everything | #27 (must resolve first) |
| #27 | PR #1 (must merge first) |
| #9, #10 | #27 (need frontend scaffold on master) |
| #17 (accounts page) | #12 or #13 or #14 or #15 (needs at least one backend integration) |
| #16 (net worth dashboard) | #12, #13, #14, #15 (needs all integrations to be meaningful) |
| #19 (Docker Compose) | #18 (Flyway — want stable DB schema before containerizing) |
| #20 (Vitest) | #9, #10 (need UI components to test) |
| #21 (E2E tests) | #20 (unit tests first), #9, #10 |
| #23 (multi-currency) | Phase 3 integrations (need account model finalized) |
| #24 (budget alerts) | #9, #10 (need working transaction/category UI) |

---

## 4. Sprint Groupings

### Sprint 0: Unblock (IMMEDIATE — do this now)

**Goal:** Get local dev working end-to-end.

| Task | Type | Effort |
|------|------|--------|
| Merge PR #1 | PR review + merge | XS |
| Resolve #27 (rebase Phase 3 branch, fix dev setup) | DevOps | S |

**Outcome:** Master has frontend scaffold. Dev environment works. All subsequent work is unblocked.

---

### Sprint 1: Core Usability (Phase 2 completion)

**Goal:** Complete remaining Phase 2 frontend work. Can run in parallel with Sprint 2 backend items.

| Task | Issue | Effort | Notes |
|------|-------|--------|-------|
| Transaction edit/delete UI | #9 | M | Single PR |
| Category management UI | #10 | M | Single PR |

**Parallel track — can start simultaneously:**

| Task | Issue | Effort | Notes |
|------|-------|--------|-------|
| Flyway database migrations | #18 | M | Independent of frontend work |

---

### Sprint 2: Financial Integrations — Backend (Phase 3, part 1)

**Goal:** Build all four backend integrations. These can be worked on in parallel since they are independent of each other.

**Parallel lanes:**

| Lane | Sub-tasks | Effort |
|------|-----------|--------|
| Lane A: Bitcoin wallet | #15a, #15b | S+S |
| Lane B: Coinbase | #12a, #12b, #12c, #12d | S+M+M+S |
| Lane C: M1 Finance | #13a, #13b, #13c | S+M+S |
| Lane D: Marcus/Plaid | #14a, #14b, #14c | M+S+S |

**Recommended order within a single-developer workflow:**

1. **#15 (Bitcoin wallet)** — simplest, no auth complexity, public APIs, quick win
2. **#12 (Coinbase)** — well-documented API, OAuth2 flow, highest value
3. **#13 (M1 Finance)** — research-dependent, may pivot based on API availability
4. **#14 (Marcus)** — likely requires Plaid, cost implications to evaluate

---

### Sprint 3: Financial Integrations — Frontend (Phase 3, part 2)

**Goal:** Build the dashboard and accounts management UI.

| Task | Issue | Effort | Depends On |
|------|-------|--------|------------|
| Accounts overview page | #17a, #17b | M+M | At least #15 done |
| Net worth dashboard | #16a, #16b, #16c | M+M+M | All of #12-#15 done |

**Note:** #17 can start as soon as one backend integration lands. #16 should wait until all integrations are done for the full picture.

---

### Sprint 4: Production Readiness (Phase 4)

**Goal:** Harden for deployment.

| Task | Issue | Effort | Depends On |
|------|-------|--------|------------|
| Docker Compose | #19a, #19b | S+M | #18 |
| Vitest setup | #20 | M | #9, #10 |
| E2E tests | #21 | M | #20 |

---

### Sprint 5: Polish (Phase 5 — pick as desired)

These are independent "nice-to-haves" that can be prioritized based on interest:

| Task | Issue | Effort | Notes |
|------|-------|--------|-------|
| Dark mode | #22 | S | Independent, fun quick win |
| Bank CSV import | #25 | M | Independent |
| Budget alerts | #24 | M | Needs Phase 2 UI done |
| Multi-currency | #23 | L | Needs Phase 3 account model finalized |

---

## Summary: What Can Run In Parallel vs. Sequential

```
SEQUENTIAL (critical path):
  PR #1 merge --> #27 resolve --> Sprint 1 (#9, #10) --> Sprint 3 (#16, #17)

PARALLEL OPPORTUNITIES:
  - Sprint 1: #9 + #10 + #18 (all independent)
  - Sprint 2: #15 + #12 + #13 + #14 (all independent backend integrations)
  - Sprint 4: #19 + #20 (independent tracks: DevOps vs. testing)
  - Sprint 5: #22 + #25 (independent features)

COST WATCH:
  - #14 (Marcus): Likely requires Plaid — verify pricing before implementing
  - #13 (M1 Finance): May require Plaid or unofficial approach — research first
  - #12 (Coinbase): Free API, but requires developer account registration
```
