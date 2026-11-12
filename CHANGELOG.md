# Changelog

All notable changes to Biddly are recorded here.
Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Versions follow [Semantic Versioning](https://semver.org/).
The *why* and *how* behind each change is in [docs/DEVELOPMENT_LOG.md](docs/DEVELOPMENT_LOG.md).

## [Unreleased]

### Added - Challenge 8: concurrent bids (2026-11-12)

- `GlobalExceptionHandler` maps `OptimisticLockingFailureException` to **409 `CONCURRENT_UPDATE`** with a fixed, friendly message. Hibernate's internal text is no longer sent to the client.
- `AuctionRepository.findByIdForUpdate`: `@Lock(PESSIMISTIC_WRITE)` with an explicit JPQL `@Query`, so Hibernate sends `SELECT ... FOR UPDATE`. `BidService.placeBid` uses it, so bids on one auction queue up instead of colliding.
- `BidServiceTest` stubs moved to the new repository method.
- Race tests enabled: 8 simultaneous bids give one winner, seven 422 `BID_TOO_LOW`, no 409s and no 500s (59/59 backend tests).
- Rule R21 marked as enforced; the concurrency decision is recorded in `business-rules.md`.

### Added - Challenge 8 starter: concurrent bids (2026-11-12)

- `BidConcurrencyIntegrationTest`: 8 real HTTP bids released together against PostgreSQL. A test clock pauses for 500 ms so the race happens every time.
  - Enabled: `keepsThePriceAndTheHistoryInAgreement`, proving `@Version` already keeps the data consistent.
  - Disabled for part 1: losers get 409 `CONCURRENT_UPDATE` or 422, never 500.
  - Disabled for part 2: with `SELECT ... FOR UPDATE`, bidders queue up and get 422, with no 409s.
- Frontend: the bid panel shows a warning for `CONCURRENT_UPDATE` and refreshes the auction after any rejected bid, not only after a successful one.
- Rule R21 and the Challenge 8 brief.

### Added - Challenge 7: place a bid (2026-11-12)

- `Auction.acceptBid` enforces R3, R4, R5 and R19 as guard clauses in a fixed order:
  - It compares user ids, not objects.
  - A bid at exactly `endTime` is too late.
  - The first-bid minimum depends on whether there is a leader.
  - `BID_TOO_LOW` names the minimum.
- `BidService.placeBid` loads the auction and bidder (404s) and reads the server clock once. It applies the rules, then saves the bid in the same transaction as the new price and leader (R20).
- `POST /api/v1/auctions/{id}/bids` now returns 201. The details page's **Place bid** panel and **Bid history** work end to end.
- All Challenge 7 tests enabled: 56/56 backend tests pass.
- Rules R3-R5 marked as enforced; R19 and R20 added; R6 partly enforced (no setters on `Bid`, no update or delete API).

### Added - Challenge 7 starter: place a bid (2026-11-12)

- Flyway migration `V2__create_bids.sql`: an insert-only `bids` table (`auction_id`, `bidder_id`, `amount`, `placed_at`) with named constraints and indexes, and `auctions.leading_bidder_id`.
- New `bid` module: the immutable `Bid` entity, `BidRepository`, `BidMapper`, `BidService` and `BidController`.
  - `GET /api/v1/auctions/{id}/bids`: bid history, newest first (US-04).
  - `POST /api/v1/auctions/{id}/bids` with `PlaceBidRequest`, which returns 501 until Challenge 7 is solved.
- `AuctionResponse.leadingBidder`, loaded through the existing entity graphs.
- Stubs: `Auction.acceptBid(bidder, amount, now)` and `BidService.placeBid`.
- Tests:
  - Disabled: `AuctionTest.AcceptBid` (10 cases), `BidServiceTest.PlaceBid` (5) and one `BidApiIntegrationTest` case.
  - Enabled: tests for the bid history and request validation.
- Frontend:
  - New `features/bids` module, with a **Place bid** panel that has a demo "Bid as" picker until sign-in exists.
  - A **Bid history** list on the details page, replacing the disabled bid field.
  - `minimumNextBid` with tests.
- Docs: Challenge 7 brief, rules R3-R5 mapped to `Auction.acceptBid`, bids in the database design, README API table, roadmap (M6 started).

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
- `exercises/02-ts-auction-simulator`: domain types and simulator implemented (Challenge 4). `tsc` reports 0 errors and 20/20 tests pass.
- Root `.gitignore` ignores `node_modules/` in every folder.

### Added - Product Requirements Document (2026-10-29)

- `docs/product-requirements.md` expanded into a full PRD: purpose, success metrics, personas, user stories, key features (auction lifecycle, bidding, persistence, real-time, auto-close, auth, search), scope, non-functional requirements, tech stack, risks and release plan.
- `docs/user-stories.md`: new stories US-04, US-13, US-14, US-24, US-25 and US-42.

### Added - AGENTS.md (2026-10-30)

- `AGENTS.md` at the repository root: guidance for AI coding agents (tutor mode, layout, commands, backend and frontend conventions, business rules, documentation rules).

### Added - Challenge 6: edit a draft auction (2026-11-12)

- `Auction.updateDetails(...)`: rejects anything but a `DRAFT` (`AUCTION_NOT_EDITABLE`), replaces every editable field (a `null` clears an optional field) and resets `currentPrice` to the new `startingPrice`.
- `AuctionService.updateAuction`: load or 404, the same time-window and category checks as create, then delegate to the entity. Saved by dirty checking, without `save`. `PUT /api/v1/auctions/{id}` now works instead of returning 501.
- Refactor: `createAuction` and `updateAuction` share `validateTimeWindow(start, end, now)` and `findCategory(id)`, and each reads the clock once.
- `AuctionTest.UpdateDetails` and `AuctionServiceTest.UpdateAuction` enabled (35/35 backend tests).
- Rules R2 and R18 marked as enforced; US-13 done.

### Added - Challenge 6 starter: edit a draft auction (2026-11-11)

- Endpoint `PUT /api/v1/auctions/{id}` with an `UpdateAuctionRequest` body (returns 501 until Challenge 6 is solved).
- Stubs: `Auction.updateDetails(...)` and `AuctionService.updateAuction(...)`.
- Disabled tests: `AuctionTest.UpdateDetails` (4 methods, 7 cases) and `AuctionServiceTest.UpdateAuction` (6).
- Frontend: **Edit draft** page (`/auctions/:id/edit`) and an **Edit** button for drafts. The "Sell an item" form moved into a shared `AuctionForm` component (same look), with its schema and conversions in `auctionForm.ts`. Create and update calls moved into `api.ts` and the `useCreateAuction` / `useUpdateAuction` hooks, built on one generic `useAuctionMutation`.
- Business rule R18 and the Challenge 6 brief in `docs/CHALLENGES.md`.

### Added - Challenge 5: publish and cancel (2026-11-10)

- `Auction.publish(now)`: only a `DRAFT` whose `endTime` is still in the future can be published (`AUCTION_NOT_DRAFT`, `AUCTION_ALREADY_ENDED`); it becomes `ACTIVE` if `startTime` has arrived, otherwise `SCHEDULED`.
- `Auction.cancel()`: rejects `COMPLETED` and `CANCELLED` auctions (`AUCTION_NOT_CANCELLABLE`); otherwise the auction becomes `CANCELLED`.
- `AuctionService.publishAuction` / `cancelAuction`: load or 404, delegate to the entity, and return the updated auction. Changes are stored by JPA dirty checking, without `save`. The publish and cancel endpoints now work instead of returning 501.
- `AuctionTest` and `AuctionServiceTest.PublishAndCancel` enabled (22/22 backend tests).
- Rules R15-R17 marked as enforced; US-11 and US-12 done.

### Added - Challenge 5 starter: publish and cancel (2026-11-04)

- Endpoints `POST /api/v1/auctions/{id}/publish` and `POST /api/v1/auctions/{id}/cancel` (return 501 until Challenge 5 is solved).
- Stubs: `Auction.publish(Instant now)`, `Auction.cancel()`, `AuctionService.publishAuction`, `AuctionService.cancelAuction`.
- Disabled tests: `AuctionTest` (entity lifecycle rules, including parameterized tests) and the nested `PublishAndCancel` group in `AuctionServiceTest`.
- Frontend: `AuctionActions` card on the details page with **Publish** and **Cancel auction** (with a confirmation dialog), `usePublishAuction` / `useCancelAuction` hooks, and a status chip coloured by status.
- Business rules R15-R17 and the Challenge 5 brief in `docs/CHALLENGES.md`.

### Added - Challenge 3: search, filter and sort (2026-11-04)

- `filterAuctions`: case-insensitive search on title and description, category filter, and four sort orders (`ENDING_SOON`, `NEWEST`, `PRICE_LOW`, `PRICE_HIGH`) with an exhaustive `never` check. The controls on the **Live auctions** page now work.
- `filterAuctions.test.ts` enabled, with a new `sorts by newest first` test (7/7; 10/10 frontend tests).
- Milestone 3 complete.

### Added - Challenge 2: create an auction (2026-10-30)

- `AuctionService.createAuction`: validates the time window (`START_TIME_IN_PAST`, `INVALID_TIME_WINDOW`), checks that the seller and the optional category exist, and saves a `DRAFT` auction. `POST /api/v1/auctions` now returns 201 instead of an error.
- `AuctionServiceTest` enabled, with a new `rejectsUnknownCategory` test (5/5 pass; 9/9 backend tests).
- R14 marked as enforced in `business-rules.md`.

### Changed - First-bid rule decided (2026-10-30)

- R5 clarified: the first bid may equal `startingPrice`; later bids must be at least `currentPrice + minimumIncrement`. Open question 1 marked as decided in `business-rules.md` and the PRD; `AGENTS.md` updated.

### Changed

- Default ports moved to 8081 (backend) and 5180 (frontend) to avoid clashing with other local apps on 8080/5173.
