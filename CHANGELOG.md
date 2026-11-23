# Changelog

All notable changes to Biddly are recorded here.
Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Versions follow [Semantic Versioning](https://semver.org/).
The *why* and *how* behind each change is in [docs/DEVELOPMENT_LOG.md](docs/DEVELOPMENT_LOG.md).

## [Unreleased]

### Added - Authentication reference (2026-11-24)

- `docs/authentication.md`: how sign-in and access work end to end, with diagrams. It covers the algorithms (BCrypt with salt, HS256 JWT, SHA-256 refresh-token hashes, `SecureRandom`), every flow (register, sign in, request checks, refresh with reuse detection, page load, sign out), threats and defences, known trade-offs, and what's protected today. Linked from `architecture.md`, the primer and `AGENTS.md`, and updated with each auth challenge.

### Added - Challenge 11b: sign in, tokens and sign out (2026-11-24)

- `AuthService.login`:
  - an unknown email, a wrong password and an account without a password all get one `401 INVALID_CREDENTIALS`, "Wrong email or password.";
  - a suspended account gets `403 ACCOUNT_SUSPENDED`, checked only after the password matched;
  - otherwise a session starts.
- `SessionService.refresh` rotates the refresh token. A missing, unknown or expired token gets `401 INVALID_REFRESH_TOKEN`. A reused (already revoked) token ends every session of that user, and `noRollbackFor` keeps those revocations despite the 401.
- `SessionService.logout` revokes the token and never fails.
- All Challenge 11b tests enabled: 142/142 backend tests. Rule R28 is done; US-30 is done.
- Known trade-offs recorded in `business-rules.md`: sign-in timing reveals registered emails, and two tabs refreshing at once sign the user out.

### Added - Challenge 11b starter: sign in, tokens and sign out (2026-11-23)

- Migration `V5__create_refresh_tokens.sql`: refresh tokens stored only as SHA-256 hashes, with expiry and revocation. `RefreshToken` entity and `RefreshTokenRepository`: a locking lookup by hash, and "revoke all of a user's sessions".
- JWT access tokens: HS256, 15 minutes, signed with `JWT_SECRET`. There's no default outside dev and test, and the app refuses a key shorter than 32 bytes. `JwtService`, `JwtConfig`, and Spring Security's OAuth 2 resource server checking the token on `GET /api/v1/auth/me`.
- `SessionService.issue` (ready) and the stubs `SessionService.refresh` and `logout`.
- The stub `AuthService.login`, with `LoginRequest` (its `toString` hides the password), `TokenResponse`, and `UserRepository.findByEmailIgnoreCase`.
- `UnauthorizedException` (401) and `ForbiddenException` (403) in `GlobalExceptionHandler`.
- `POST /api/v1/auth/login`, `/refresh` and `/logout`, using the `biddly_refresh` cookie: `HttpOnly`, `SameSite=Strict`, `Path=/api/v1/auth`, 14 days, `Secure` outside dev.
- Dev seed: the demo users have the password `biddly-demo` (dev only).
- Tests:
  - Disabled: `AuthServiceTest.Login` (7), `SessionServiceTest.Refresh` (5) and `.Logout` (2), and 7 in `AuthSessionIntegrationTest`.
  - Enabled: 2 for `issue`, and 3 for `/auth/me` (no token, an expired token, an edited token).
- Frontend:
  - a sign-in page at `/sign-in`;
  - the header shows the signed-in user and **Sign out**;
  - the access token lives in memory only, and is sent as `Authorization: Bearer`, but never to the auth endpoints;
  - the session is restored on page load, and the token refreshes a minute before it expires;
  - only one refresh runs at a time (`session.ts`, 6 tests);
  - after registering, the next step is **Sign in**.
- Backend 140 tests pass with 21 skipped; frontend 29/29.

### Added - Challenge 11a: register with email and password (2026-11-23)

- `AuthService.register`:
  - stores the email in lower case and the name trimmed;
  - refuses passwords over 72 bytes (`422 PASSWORD_TOO_LONG`) before hashing;
  - refuses a used email (`409 EMAIL_TAKEN`);
  - stores only the BCrypt hash.
- A sign-up that loses a race for the same email gets the same 409. Only a violation of `uq_users_email_lower` is turned into `EMAIL_TAKEN`; any other database error is still a logged 500.
- All Challenge 11a tests enabled: 114/114 backend tests. Rule R27 is done, and R28 is done for registration.
- Known trade-off recorded in `business-rules.md`: sign-up reveals which emails have accounts until email verification exists.

### Added - Challenge 11a starter: register with email and password (2026-11-23)

- Spring Security, with `SecurityConfig` still allowing every request (11c locks it down) and a BCrypt `PasswordEncoder`. Spring Boot's default generated-password user is switched off.
- Migration `V4__add_user_role.sql`: `users.role` (`USER` or `ADMIN`). `User` stores the password hash; new `Role` enum and `UserRepository.existsByEmailIgnoreCase`.
- `ConflictException`, mapped to **409** by `GlobalExceptionHandler`.
- `POST /api/v1/auth/register` with `RegisterRequest` (its `toString` hides the password) and `AccountResponse`. `AuthService.register` is the challenge and answers 501 until solved.
- Tests:
  - Disabled: `AuthServiceTest` (8), `AuthApiIntegrationTest` (3) and `RegistrationRaceIntegrationTest` (1).
  - Enabled: one Bean Validation test.
- Frontend: a **Create account** link in the header, and a register page at `/register` with a form schema (`registerForm.ts`, 4 tests).
- Backend 102 tests pass with 12 skipped; frontend 23/23.

### Changed - Sign-in plan adds Google (2026-11-16)

- Milestone 5 now includes **Sign in with Google** (new story US-32), next to email and password. Both end in the same Biddly session (access JWT and refresh cookie).
- Challenge 11 is planned in five parts: 11a register, 11b sign in and tokens, 11c protect the API, 11d Google sign-in, 11e seller approval.
- New planned rules R27-R32 (unique email, password storage, signed-in writes, Google token checks, no takeover of password accounts, admin-approved sellers). R1 now covers publish and cancel too. New stories US-33 (request seller access) and US-43 (approve seller requests).
- Product decisions 7 and 8:
  - Google sign-in with an email that already has a password account is refused (`409 ACCOUNT_EXISTS`).
  - Roles are `USER` and `ADMIN`; creating auctions needs admin approval.
- New `docs/learning/authentication-primer.md`: what to learn first, in order.

### Added - Auction length limits and whole rupees (2026-11-16)

- **R24:** creating or editing an auction shorter than 10 minutes gives 422 `AUCTION_TOO_SHORT`; longer than 120 days gives 422 `AUCTION_TOO_LONG`. The auction form shows the same errors before submitting.
- **R25:** a starting price, increment or bid with paise gives 400 with the field error "must be in whole rupees". `100` and `100.00` are both fine. New constraint `common.WholeRupees`.
  - The auction form rejects paise; the bid box shows "Whole rupees only", steps by ₹1 and disables **Place bid**.
- Tests: 5 new `AuctionServiceTest` cases, `AuctionApiIntegrationTest.rejectsPricesWithPaise`, `BidApiIntegrationTest.rejectsABidWithPaise`, and `auctionForm.test.ts` (4). Backend 101/101, frontend 19/19.

### Changed - Product decisions (2026-11-16)

- The six open product questions are decided and recorded in `business-rules.md` under "Product decisions":
  - first bid at least the starting price;
  - no anti-sniping;
  - the leader may raise their own bid (R26);
  - no reserve price;
  - 10 minutes to 4 months (R24);
  - whole rupees only (R25).
- New test `AuctionTest.letsTheLeaderRaiseTheirOwnBid`.
- US-24 "My bids" moves from Milestone 6 to Milestone 5, because it needs the signed-in user.

### Added - Challenge 10: idempotent bids (2026-11-13)

- `BidService.placeBid`: after locking the auction, a request with a key looks up this bidder's earlier bid with that key.
  - **Same auction and amount:** returns that bid with 201 and stores nothing.
  - **Anything else:** 422 `IDEMPOTENCY_KEY_REUSED`.
  - **New key:** stored with the new bid.
  - **No key:** no lookup.
- All Challenge 10 tests enabled: 93/93 backend tests, including six simultaneous copies of one request giving one bid.
- Rule R23 and the US-20 idempotency criterion marked as done; Milestone 6 (bidding engine) complete.

### Added - Challenge 10 starter: idempotent bids (2026-11-13)

- Migration `V3__add_bid_idempotency_key.sql`: `bids.idempotency_key` (UUID) with a partial unique index on `(bidder_id, idempotency_key)`.
- `Bid` stores the key; `BidRepository.findByBidderIdAndIdempotencyKey`.
- `POST /api/v1/auctions/{id}/bids` reads an optional `Idempotency-Key` header. `BidService.placeBid` takes it as a third parameter; the behaviour is the challenge.
- `GlobalExceptionHandler`: a path variable, query parameter or header that can't be converted (for example, a malformed key) is now **400 `INVALID_PARAMETER`** instead of a 500.
- Tests:
  - Disabled: `BidServiceTest.Idempotency` (4) and `BidIdempotencyIntegrationTest` (3, including six simultaneous copies of one request).
  - Enabled: 4 guard tests that must keep passing.
- Frontend: every bid attempt sends a UUID key, and pressing **Place bid** again with the same bidder and amount reuses it (`nextAttempt`, with 3 tests).
- Rule R23 and the Challenge 10 brief.

### Added - Challenge 9: auction scheduler (2026-11-13)

- `Auction.start` and `Auction.complete` with guard clauses (`AUCTION_NOT_SCHEDULED`, `START_TIME_NOT_REACHED`, `AUCTION_NOT_ACTIVE`, `END_TIME_NOT_REACHED`). Completing keeps the leading bidder as the winner, or none if nobody bid.
- `AuctionLifecycleService.advance` starts every due auction, then completes every ended one, in one transaction with one `now`.
- All Challenge 9 tests enabled: 81/81 backend tests.
- Rules R7, R8 and R22 and story US-14 marked as done.

### Added - Challenge 9 starter: auction scheduler (2026-11-12)

- `AuctionLifecycleJob` runs every 5 seconds (`@Scheduled`, fixed delay) and calls `AuctionLifecycleService.advance()`. Until the challenge is done it logs a single "not implemented" warning.
- `SchedulingConfig` enables scheduling. `biddly.scheduler.enabled` and `biddly.scheduler.interval-ms` (env `SCHEDULER_ENABLED`, `SCHEDULER_INTERVAL_MS`) control it, and the `test` profile turns it off.
- `AuctionRepository`: derived queries `findByStatusAndStartTimeLessThanEqual` and `findByStatusAndEndTimeLessThanEqual`.
- Stubs for the challenge: `Auction.start`, `Auction.complete`, `AuctionLifecycleService.advance`.
- Disabled tests: `AuctionTest` `Start` and `Complete`, `AuctionLifecycleServiceTest`, and `AuctionLifecycleIntegrationTest` (one run over seven auctions in different states). Enabled: the job bean is absent in tests.
- Frontend: the bid panel explains why bidding is closed ("Sold to X for ₹Y", "closed without any bids", "Bidding opens …", "cancelled"), and the price label reads "Final price" once an auction is completed.
- Rule R22 and the Challenge 9 brief; R7 and R8 mapped to `Auction.complete`.

### Added - Rule R10: no cancelling once bids exist (2026-11-12)

- `Auction.cancel` rejects an auction that has a leading bidder with 422 `AUCTION_HAS_BIDS`, after the R17 check. The rule lives on the entity, not in `AuctionService`, so every caller of `cancel()` gets it.
- Tests: `AuctionTest.rejectsCancellingAnAuctionThatHasBids` (gets a real leader through `acceptBid`) and `BidApiIntegrationTest.refusesToCancelAnAuctionOnceItHasABid` (bid, then cancel over HTTP; the auction stays `ACTIVE`).
- Frontend: **Cancel auction** is hidden once an auction has a leader.
- R10 and the US-12 criterion marked as done.

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
