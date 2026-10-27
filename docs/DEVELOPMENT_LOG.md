# Development log

A chronological record of **what** was added to Biddly, **when**, **how** (the exact commands), and **why**.
Add a new entry at the bottom for every working session. Short summaries go in [CHANGELOG.md](../CHANGELOG.md).

---

## Session 1 - 2026-10-13 - Milestone 0: project foundation

### Goal

Create a monorepo that a new developer can clone and run, with one working read-only slice:
**PostgreSQL → Spring Boot → REST → React → auction card on screen.**

### Decisions made

| Decision | Choice | Why |
|---|---|---|
| Architecture | Modular monolith | One deployable and one database transaction for bids. Modules are split by business capability so they can be extracted later. |
| Java package | `com.biddly` | Short and product-named. |
| Currency | INR (₹), `en-IN` formatting | Target market. Money is `NUMERIC(12,2)` / `BigDecimal`, never floating point. |
| IDs | `BIGINT GENERATED ALWAYS AS IDENTITY` | The SQL-standard way to auto-number in PostgreSQL (better than the older `SERIAL`). |
| Timestamps | `TIMESTAMPTZ` + `java.time.Instant`, stored in UTC | The browser converts to local time for display only. |
| Schema changes | Flyway migrations, `ddl-auto: validate` | Every schema change is a versioned, reviewed SQL file. Hibernate only *checks* the schema and never changes it. |
| Seed data | Repeatable migration in `db/seed`, `dev` profile only | Production never gets fake users. |
| Auth | Deferred to Milestone 5 | Following the roadmap: build the core flow first. `sellerId` is temporarily in the request body. |
| Real-time | Polling (5-10 s) for now | Simplest correct version; WebSocket arrives in Milestone 7. |
| Cloud provider | Undecided | Not needed until Milestone 12. |
| Ports | Backend 8081, frontend 5180 | 8080 and 5173 were already used by another app on the dev machine. |

### Step-by-step

#### 1. Checked the toolchain

Found Java 21 (Corretto), Maven 3.9, Node 24, Docker Desktop with kubectl, and Minikube. `psql` is not installed and isn't needed, because PostgreSQL runs in Docker.

#### 2. Generated the backend with Spring Initializr

```powershell
Invoke-WebRequest "https://start.spring.io/starter.zip?type=maven-project&language=java&bootVersion=4.1.1&groupId=com.biddly&artifactId=backend&name=biddly-backend&packageName=com.biddly&javaVersion=21&dependencies=web,data-jpa,postgresql,validation,flyway,actuator,testcontainers,devtools" -OutFile backend.zip
```

> **Spring Boot 4 note:** starters are now split per technology, for example `spring-boot-starter-webmvc`, `spring-boot-starter-flyway` and matching `*-test` starters. Flyway auto-configuration only activates when `spring-boot-starter-flyway` is present.

Security and Redis were deliberately **not** added yet. Adding Spring Security now would lock every endpoint, and you will learn more by adding it yourself in Milestone 5 and seeing what changes.

#### 3. Designed the first schema (`V1__create_users_categories_auctions.sql`)

- Tables `users`, `categories` and `auctions`, with foreign keys.
- `CHECK` constraints make bad data impossible even if application code has a bug: positive prices, `current_price >= starting_price`, `end_time > start_time`, and a valid status.
- A unique index on `lower(email)` makes emails case-insensitively unique.
- Indexes: `(status, end_time)` for "active auctions ending soon", `(category_id, status)` and `seller_id`.
- A `version` column for optimistic locking, used in Milestone 6.
- Deviation from the roadmap: one `cover_image_url` column instead of an `auction_images` table, for now. A gallery comes with image upload.

#### 4. Built the backend read path

- `Auction` entity, `AuctionRepository` with `@EntityGraph` (avoids N+1 queries), `AuctionService`, `AuctionMapper`, `AuctionController`.
- DTOs are Java `record`s. Entities are never returned from controllers.
- `PageResponse<T>` gives a stable pagination contract.
- `GlobalExceptionHandler` maps exceptions to one `ApiError` shape: 400 validation, 404 not found, 422 business rule, 501 not implemented, 500 unexpected.
- A `Clock` bean, so business rules involving "now" can be tested with frozen time.
- `POST /api/v1/auctions` validates the request with Bean Validation, then calls the service stub, which is **Challenge 2**.

#### 5. Backend tests

- `AuctionApiIntegrationTest`: boots the whole app against a real PostgreSQL started by **Testcontainers**, then checks list, details and the 404 error body.
- `AuctionServiceTest`: Mockito unit tests that describe Challenge 2. They are `@Disabled` until you implement it.
- Result: `Tests run: 8, Failures: 0, Errors: 0, Skipped: 4`.

#### 6. Generated the frontend

```powershell
npm create vite@latest frontend -- --template react-ts --no-interactive
npm install @mui/material @mui/icons-material @emotion/react @emotion/styled @fontsource-variable/inter @tanstack/react-query react-router react-hook-form zod @hookform/resolvers
npm install -D vitest jsdom @testing-library/react @testing-library/jest-dom @testing-library/user-event
```

- Added `strict` and `noUncheckedIndexedAccess` to `tsconfig.app.json`. Type unions are used instead of `enum`, because the template enables `erasableSyntaxOnly`.
- The Vite dev server proxies `/api` to Spring Boot, so the browser sees one origin.

#### 7. Built the UI

- Theme (`src/app/theme.ts`): violet/coral brand colours, Inter font, rounded shapes, and light/dark colour schemes using MUI CSS variables. The toggle remembers your choice.
- Layout: sticky translucent app bar, nav, a "Sell an item" call to action, and a footer.
- Pages: Home (hero, value props, "Ending soon"), Live auctions (search, category chips, sort, skeleton loading, empty and error states), Auction details (live countdown, price panel, disabled bid box until Milestone 6), Sell an item (React Hook Form + Zod, server field errors mapped back onto inputs), and 404.
- Accessibility: semantic headings, labelled inputs, readable contrast in both themes.

#### 8. Docker Compose for PostgreSQL

`docker compose up -d` starts `postgres:17-alpine` with a health check and a named volume, so data survives restarts. pgAdmin is behind the `tools` profile.

#### 9. Verified end to end

- Flyway applied `V1` and the dev seed on first start.
- `GET /api/v1/auctions` returned 5 active auctions, sorted by soonest end time.
- Error bodies checked: `VALIDATION_FAILED` (400), `NOT_IMPLEMENTED` (501), `NOT_FOUND` (404).
- UI checked in light and dark mode.

### Problems hit and what we learned

1. **Port 8080 already in use** by another Spring Boot app. Fix: Biddly defaults to 8081 (`SERVER_PORT` overrides it).
2. **Port 5173 already in use**, so Vite silently moved to 5174. Fix: port 5180 with `strictPort: true`, so Vite fails loudly instead of moving.
3. **POST failed with `403 Invalid CORS request` while GET worked.** Browsers send an `Origin` header on POST requests even through the proxy. The running backend still allowed only the old origin. Lesson: CORS config must match the real frontend origin, and config changes need a restart.
4. **TypeScript: `theme.vars` possibly undefined.** MUI needs `import type {} from '@mui/material/themeCssVarsAugmentation'` when CSS variables are enabled.
5. **oxlint `react(purity)` warnings**: `Date.now()` during render. Fixed with a module constant and a lazy `useState` initializer.

#### 10. Published to GitHub

Repository: <https://github.com/Samijain/biddly> (public).

```powershell
gh auth login                     # added the personal account next to the work account
gh auth switch -u Samijain
git config user.name  "Samyak Jain"                                   # repo-local identity
git config user.email "60084970+Samijain@users.noreply.github.com"    # GitHub noreply address, keeps the real email private
gh repo create Samijain/biddly --public --source . --remote origin --push
git config credential.https://github.com.username Samijain           # Git Credential Manager always uses the personal account for this repo
gh auth switch -u kj185108_DFH    # CLI back to the work account
```

> Why repo-local config? The work and personal accounts live on the same machine. Global settings stay on the work identity; only this repository uses the personal one.

### Next session (as planned on 2026-10-13)

1. You: Challenges 1, 2 and 3 (see [CHALLENGES.md](CHALLENGES.md)).
2. Answer the open questions in [business-rules.md](business-rules.md#open-questions-for-you).
3. Then: Milestone 4 completion (publish/cancel auction, OpenAPI docs) and the start of the bids table.

---

## Sessions 2-3 - 2026-10-21 to 2026-10-27 - Milestone 1: JavaScript simulator

### What you built

`exercises/01-js-auction-simulator/simulator.js`: `createIdGenerator`, `createAuction`, `placeBid`, `getHighestBid`, `closeAuction`, `getWinner`. Committed as `c9ec407` and `089d8a5`.

### Concepts learned, in the order they came up

| Concept | Where it appeared |
|---|---|
| **Closures** and factory functions | `createIdGenerator` keeps a private `counter` per generator |
| `counter++` vs `++counter` | First ID was `x-0` instead of `x-1` |
| References vs copies | `const b = a` shares one function and one counter |
| **Type coercion** | `"1000" > 0`, `true > 0` and `[5] > 0` are all `true` |
| `Number.isFinite` vs global `isFinite` | The correct way to validate a number |
| **Guard clauses** | Check every rule first, then build the result |
| **Immutability** | `placeBid` returns a new auction; a rejected bid changes nothing |
| Never rewrite user input | Don't silently raise a bid; accept it exactly or reject it with a reason |
| `reduce` on an empty array | Returns the initial value, here `undefined` |
| `?.` and `??` | `getHighestBid(a)?.bidderId ?? null` |
| One source of truth | `const minimumAmount` used in both the check and the error message |
| Truthiness pitfalls | `0 && ...` and `NaN && ...` short-circuit, so `x && x <= 0` never rejects 0 or NaN |

### Mistakes worth remembering

1. Using one rule for every bid. The first bid and later bids need different minimums.
2. "Fixing" a failing test by silently changing the stored price. That hides the problem instead of rejecting the bid.
3. The error message used a different formula than the check, which would tell users the wrong minimum.
4. A truthiness guard (`startingPrice && ...`) disabled validation for exactly the values it should reject.
5. Writing the *valid* condition inside `if (...) throw`, which rejected every good auction. Then using `Number.isFinite(x) && ...` as a gate, which skipped validation for `NaN` and `Infinity`. The fix joins every "something is wrong" check with `||` (De Morgan's law).

### Final validation check (2026-10-27)

| `startingPrice` / `minimumIncrement` | Result |
|---|---|
| `0`, `-1`, `NaN`, `"1000"`, `Infinity`, `true`, `null` | rejected |
| `1000` | accepted |

Milestone 1 complete: 11/11 tests.

---

## Session 4 - 2026-10-27 - Milestone 2 setup: TypeScript simulator

### What was added

- `exercises/02-ts-auction-simulator/` was already committed in `089d8a5`, with `package.json`, a strict `tsconfig.json`, `simulator.test.ts` (20 runtime tests) and `simulator.typecheck.ts` (compile-time tests using `@ts-expect-error`).
- This session added `types.ts` (worked `Bid` example + 8 TODOs), `README.md`, and the Challenge 4 brief in [CHALLENGES.md](CHALLENGES.md#challenge-4---typescript-domain-model-milestone-2).
- `npm install` in the exercise folder (TypeScript 6.0.3, `@types/node`). The root `.gitignore` now ignores `node_modules/` everywhere.

### How it works

- **No build step.** Node 24 strips TypeScript types and runs `.ts` files directly, so `npm test` is plain `node --test`. That's why the tsconfig sets `erasableSyntaxOnly` (no enums or parameter properties) and `allowImportingTsExtensions` (imports end in `.ts`).
- **Two judges.** `tsc` (`npm run typecheck`) checks the types and never runs anything. `node --test` runs the code and never checks types. `npm run check` runs both.
- **Compile-time tests.** Every `// @ts-expect-error` line in `simulator.typecheck.ts` *must* be a type error. If your types are too loose, the line compiles and `tsc` reports an "unused directive".

### Verified

A reference solution was written in a temporary folder outside the repo, then deleted: `tsc` passed with 0 errors and all 20 tests passed. The exercise is solvable as specified.

---

## Session 5 - 2026-10-27 - Milestone 2: TypeScript simulator completed

### What you built

`types.ts` (all 8 TODOs) and `simulator.ts`, ported from the JavaScript simulator. You did the types, the typed `BidRejectedError`, the `OpenAuction`/`ClosedAuction` signatures and `winningBid` in `closeAuction`. The tutor finished the last steps on request; they're listed below so you can study them.

### Finished by the tutor

| Change | Why |
|---|---|
| `BidRejectionCode` = `'AUCTION_CLOSED' \| 'SELF_BIDDING_NOT_ALLOWED' \| 'BID_TOO_LOW'` | Must list the codes `placeBid` actually throws. Typing `code` made the compiler find the mismatch. |
| `getHighestBid` callback checks `highest === null \|\| ...`, initial value `null` | With a `Bid \| null` accumulator, the first call really gets `null`. `\|\|` short-circuits, so `.amount` is never read on `null`. No empty-array special case needed. |
| `getWinner(auction: ClosedAuction)` reads `auction.winningBid` | Makes `getWinner(open)` a compile error. The runtime check uses `(auction as Auction).status` so TypeScript allows comparing against `'OPEN'`. |
| `isOpen(auction): auction is OpenAuction` | A type guard: narrows the type inside `if (isOpen(a))`. |
| `tryPlaceBid` returns `Result<OpenAuction, BidRejectedError>` | Catches only `BidRejectedError` (checked with `instanceof`, because `catch` gives `unknown`) and rethrows anything else, so real bugs aren't hidden. |
| `parseCreateAuctionInput(value: unknown)` | Narrows step by step (object, not null, not array), then checks each field and names the bad one in the error. |
| Explicit return types on `placeBid` and `getHighestBid` | Errors show up in the function, not at every call site. |

### Review questions (your answers, checked)

| Question | Answer |
|---|---|
| `unknown` vs `any` | `any` switches checking off, so mistakes only show up at runtime, and it spreads to everything it touches. `unknown` blocks every use until you narrow it. |
| What `as` does at runtime | Nothing; it's erased. It's a "trust me" to the compiler, not a check. If it's wrong, the bad value travels on and crashes somewhere else. That's why the runtime checks stay. |
| Throw or `Result` in React | `Result` for expected failures (bid too low): the compiler forces you to check `ok`, and the UI renders from it. Keep throwing for unexpected bugs, which error boundaries and TanStack Query's error state handle. |

Milestone 2 complete: `tsc` 0 errors, 20/20 tests.
