# Roadmap

Biddly is built one milestone at a time. A milestone is done only when its code, tests and docs are done.

| # | Milestone | Status |
|---|---|---|
| 0 | Product & repository setup: monorepo, docs, Spring Boot + React scaffolds, PostgreSQL in Docker, first read-only vertical slice | ✅ Done (2026-10-13) |
| 1 | JavaScript fundamentals: console auction simulator (Challenge 1) | 🟡 In progress (you) |
| 2 | TypeScript domain model: convert the simulator to TypeScript | ⏳ |
| 3 | React UI with mock data: cards, grid, filters, details, countdown | 🟡 UI built; Challenge 3 (filtering) open |
| 4 | Spring Boot foundation: create auction end to end (Challenge 2), OpenAPI docs | 🟡 Read path done; create is Challenge 2 |
| 5 | Authentication & authorization: register, sign-in, JWT + refresh cookie, roles, ownership | ⏳ |
| 6 | **Bidding engine**: validation, transactions, locking, idempotency, bid history, winner | ⏳ |
| 7 | Real-time & Redis: WebSocket/STOMP updates, cache, rate limiting, auto-close scheduler | ⏳ |
| 8 | Testing & observability: concurrency tests, Playwright, structured logs, metrics | ⏳ |
| 9 | Docker: multi-stage images, full `docker compose up --build` | ⏳ |
| 10 | GitHub Actions: CI on pull requests, image publishing | ⏳ |
| 11 | Kubernetes: Deployments, Services, Ingress, ConfigMap/Secret, probes, HPA | ⏳ |
| 12 | Cloud deployment: managed PostgreSQL, HTTPS, custom domain | ⏳ |

Source: *Bidding War Project Learning Roadmap* (OneDrive). The order follows its advice: get one correct bidding workflow first, then add security, real-time, containers and cloud.
