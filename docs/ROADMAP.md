# Roadmap

Biddly is built one milestone at a time. A milestone is done only when its code, tests and docs are done.

| # | Milestone | Status |
|---|---|---|
| 0 | Product & repository setup: monorepo, docs, Spring Boot + React scaffolds, PostgreSQL in Docker, first read-only vertical slice | ✅ Done (2026-10-13) |
| 1 | JavaScript fundamentals: console auction simulator (Challenge 1) | ✅ Done (2026-10-27) |
| 2 | TypeScript domain model: convert the simulator to TypeScript (Challenge 4) | ✅ Done |
| 3 | React UI with mock data: cards, grid, filters, details, countdown | ✅ Done (2026-11-04): UI built, search/filter/sort working (Challenge 3) |
| 4 | Spring Boot foundation: create auction end to end (Challenge 2), OpenAPI docs | 🟡 Read path, create (Challenge 2), publish and cancel (Challenge 5) and edit (Challenge 6) done; OpenAPI docs open |
| 5 | Authentication & authorization: register, sign-in with email and password **or Google**, JWT + refresh cookie, `USER` and `ADMIN` roles, admin-approved sellers, ownership, "My bids" | 🟡 Registration done (Challenge 11a, 2026-11-23); sign-in, tokens and sign-out done (Challenge 11b, 2026-11-24). Challenge 11 has five parts: 11a register, 11b sign in and tokens, 11c protect the API and ownership, 11d Google sign-in, 11e seller approval. Learning path: [authentication primer](learning/authentication-primer.md) |
| 6 | **Bidding engine**: validation, transactions, locking, idempotency, bid history, winner | ✅ Done (2026-11-13), built before M5 with a demo bidder picker: placing a bid with R3-R5 and bid history (Challenge 7), concurrent bids with a pessimistic lock and a 409 safety net (Challenge 8), winner on close (Challenge 9), idempotency keys (Challenge 10). The bidder becomes the signed-in user in M5, and US-24 "My bids" moves there because it needs to know who "me" is |
| 7 | Real-time & Redis: WebSocket/STOMP updates, cache, rate limiting, auto-close scheduler | 🟡 Auto-close scheduler done early (Challenge 9) |
| 8 | Testing & observability: concurrency tests, Playwright, structured logs, metrics | ⏳ |
| 9 | Docker: multi-stage images, full `docker compose up --build` | ⏳ |
| 10 | GitHub Actions: CI on pull requests, image publishing | ⏳ |
| 11 | Kubernetes: Deployments, Services, Ingress, ConfigMap/Secret, probes, HPA | ⏳ |
| 12 | Cloud deployment: managed PostgreSQL, HTTPS, custom domain | ⏳ |

Ideas parked for later: [multiple winners per auction](ideas/multi-winner-auctions.md).

Source: *Bidding War Project Learning Roadmap* (OneDrive). The order follows its advice: get one correct bidding workflow first, then add security, real-time, containers and cloud.
