# Changelog

All notable changes to Biddly are recorded here.
Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Versions follow [Semantic Versioning](https://semver.org/).
The *why* and *how* behind each change is in [docs/DEVELOPMENT_LOG.md](docs/DEVELOPMENT_LOG.md).

## [Unreleased]

### Added - Milestone 0: project foundation (2026-10-13)

- Monorepo with `backend/`, `frontend/`, `infrastructure/`, `exercises/` and `docs/`.
- **Backend** generated from Spring Initializr: Spring Boot 4.1.1, Java 21, group `com.biddly`.
  - Modules: `auction`, `category`, `user`, `common`, `configuration`.
  - Flyway migration `V1__create_users_categories_auctions.sql` with constraints and indexes.
  - Development-only seed data (`db/seed/R__dev_seed_data.sql`), loaded only with the `dev` profile.
  - Endpoints: `GET /api/v1/auctions`, `GET /api/v1/auctions/{id}`, `GET /api/v1/categories`, `POST /api/v1/auctions` (stub for Challenge 2).
  - Consistent error body (`ApiError`) through `GlobalExceptionHandler`.
  - Injectable `Clock` for testable time rules; CORS restricted to the frontend origin.
  - Actuator health endpoint with liveness/readiness probes (ready for Kubernetes).
  - Tests: Testcontainers integration tests (`AuctionApiIntegrationTest`) and disabled unit tests for Challenge 2 (`AuctionServiceTest`).
- **Frontend** generated with `create-vite` (React 19 + TypeScript 6 + Vite 8).
  - MUI 9 design system with light and dark colour schemes, Inter font and Biddly branding.
  - Pages: Home, Live auctions (search, category chips, sort), Auction details, Sell an item, Not found.
  - TanStack Query data layer with 5-10 s polling (real-time level 1).
  - Typed API client with a single `ApiError` type; mock mode (`npm run dev:mock`).
  - Vitest + Testing Library set up; `format` tests pass; Challenge 3 tests are skipped.
- **Infrastructure:** `docker-compose.yml` with PostgreSQL 17 (health check, named volume) and optional pgAdmin.
- **Exercises:** `exercises/01-js-auction-simulator` (Challenge 1).
- **Docs:** README, roadmap, development log, challenges, product requirements, business rules, user stories, architecture, database design and a PostgreSQL primer.

- Published to <https://github.com/Samijain/biddly>.

### Added - Milestone 1 and 2 exercises (2026-10-24 to 2026-10-27)

- `exercises/01-js-auction-simulator`: simulator implemented (Challenge 1).
- `exercises/02-ts-auction-simulator`: TypeScript exercise with runtime tests (`simulator.test.ts`), compile-time tests (`simulator.typecheck.ts`), strict `tsconfig.json`, a guided `types.ts` starter and a README (Challenge 4).
- Root `.gitignore` ignores `node_modules/` in every folder.

### Changed

- Default ports moved to 8081 (backend) and 5180 (frontend) to avoid clashing with other local apps on 8080/5173.
