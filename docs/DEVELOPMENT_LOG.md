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

---

## Session 6 - 2026-10-29 - Full Product Requirements Document

### Goal

Turn the short product summary into a complete PRD that a reviewer, recruiter or new teammate can read on its own.

### What changed

- `docs/product-requirements.md` was rewritten with 10 sections: document info, purpose and success metrics, personas, user stories, key features, scope, non-functional requirements, tech stack, risks and open questions, and a release plan.
- The to-do style actions (add, edit, mark done) were translated into auction actions: create a draft, edit a draft, and publish, close or cancel.
- `docs/user-stories.md` gained six stories so both docs use the same IDs: US-04 (bid history), US-13 (edit draft), US-14 (see the result), US-24 (my bids), US-25 (watchlist) and US-42 (review reported auctions).

### Why

- Each feature in the PRD is tagged with its milestone and the business rules (R1-R14) it depends on, so the PRD, roadmap and business rules can't drift apart silently.
- The six open questions in `business-rules.md` are linked from the PRD's risks section. They still need your decisions before Milestone 6.

---

## Session 7 - 2026-10-30 - AGENTS.md

### What changed

Added `AGENTS.md` at the repository root. AI coding agents (Cursor, Copilot, Codex and others) read it automatically before working in the repo.

### Why

- **Tutor mode:** agents must not solve open challenges unless asked. They give hints and reviews instead, so the learning stays with you.
- **Shared conventions:** the layering, `Clock`, `BigDecimal`, Flyway and `ApiError` rules are written down once instead of being re-explained in every chat.
- **Documentation rule:** every change must update this log and `CHANGELOG.md`.
- **No unrequested visual changes:** prompted by the earlier unrequested change to the DataGrid header style.

### Decision: minimum first bid (open question 1)

| Case | Minimum valid bid |
|---|---|
| No bids yet | `startingPrice` (a bid equal to it is accepted) |
| At least one bid | `currentPrice + minimumIncrement` |

**Why:** the starting price is the price the seller has already agreed to, so asking for more on the first bid would turn away a valid buyer. The increment exists to beat an *existing* bid. Both simulators (Challenges 1 and 4) already follow this rule and have a test for it. The Milestone 6 bidding engine must follow it too.

Updated: R5 in `business-rules.md`, the PRD bidding section and its open questions, and `AGENTS.md`.

---

## Session 8 - 2026-10-30 - Challenge 2: create an auction

### What you built

`AuctionService.createAuction`, in this order:

1. Reject a start time before "now", read from the injected `Clock` (`START_TIME_IN_PAST`, HTTP 422).
2. Reject an end time that is not after the start, using `!endTime.isAfter(startTime)` so that "equal" is rejected too (`INVALID_TIME_WINDOW`, HTTP 422).
3. Load the seller with `findById(...).orElseThrow(...)` (`ResourceNotFoundException`, HTTP 404).
4. Load the category only if `categoryId` is given (HTTP 404 if it doesn't exist).
5. Build the `Auction` (its constructor sets `DRAFT` and `currentPrice = startingPrice`), save it, and map the saved entity to an `AuctionResponse`.

The cheap time checks run before any database lookup, so invalid input never reaches `save`.

### Mistakes found in review, and what they taught

| Mistake | Lesson |
|---|---|
| `new BusinessRuleException("CODE")` without the import | The only constructor is `(code, message)`; the message is shown to the user. |
| `endTime.isBefore(startTime)` | Lets `end == start` through. "Must be after" is `!isAfter`. |
| `isBeforeOrEqual(...)` | Doesn't exist on `Instant` in Java 21: only `isBefore`, `isAfter`, `equals` and `compareTo`. |
| `!isAfter(...) \|\| isBefore(...)` | The second half is already covered by the first. |

### Tests

- `AuctionServiceTest` enabled. The tutor added `rejectsUnknownCategory` on request. It stubs the seller as existing, so the exception can only come from the category check, and it checks the message (`"Category 99"`) to prove it.
- `mvnw test`: 9/9 pass (5 unit, 3 integration, 1 context).
- End-to-end checks against the running backend: valid request returns 201 with a `DRAFT` auction; past start and end equal to start return 422; unknown seller and unknown category return 404. The test auction was deleted afterwards.

### Gotcha: DevTools and `mvnw test`

Running `mvnw test` while `spring-boot:run` is running recompiles `target/classes`. DevTools then restarts the app halfway through and it can come back with no controllers, so every endpoint returns 404. Restart the backend if that happens.

Challenge 2 complete.

---

## Session 9 - 2026-11-04 - Challenge 3: search, filter and sort

### What you built

`filterAuctions(auctions, filter)` in `frontend/src/features/auctions/filterAuctions.ts`, a pure function used by the **Live auctions** page:

1. Normalize the search once (`trim().toLowerCase()`), then keep auctions whose title or description contains it. `description` can be `null`, so it uses `?.` with `?? false`.
2. Keep only the chosen category; `categorySlug === null` means all categories, and `category` can be `null` too.
3. Sort with a `switch` over `AuctionSort`. The `default` branch assigns `filter.sort` to a `never` variable, so adding a fifth sort option without a `case` is a compile error.

### Mistakes found in review, and what they taught

| Mistake | Lesson |
|---|---|
| `NEWEST` copied from `ENDING_SOON` (`endTime`, ascending) | Copy-paste bugs hide when no test covers the branch. A `sorts by newest first` test was added. |
| `NEWEST` used `a - b` | `a - b` puts the smaller value first; for "latest first" use `b - a`, as `PRICE_HIGH` does. |
| `default: return 0` | Silently ignores unhandled options. `never` turns that into a compile error. |
| `console.log` in the filter and in `AuctionsPage` | Runs on every keystroke and every 10 s poll. Remove before committing. |

### Readability pass (done by the tutor on request)

- `const` instead of `let`; `trimmedFilter` renamed to `query`.
- The single 300-character condition split into `matchesCategory` and `matchesSearch`.
- `Date.parse(...)` instead of `new Date(...).getTime()`.
- `.toSorted(...)` instead of `.sort(...)`, so it's obvious the input is never mutated. (The earlier `.filter().sort()` was already safe, because `filter` returns a new array first.)
- Removed the "CHALLENGE 3" instructions from the code comments.

### Why the function must not mutate its input

`data.content` is TanStack Query's cached array, shared with `HomePage`. Sorting it in place would silently re-order every component reading that cache.

### Verified

`npm test`: 10/10 (7 for `filterAuctions`). `npm run typecheck` and `npm run lint` are clean.

Challenge 3 and Milestone 3 complete.

---

## Session 10 - 2026-11-04 - Challenge 5 starter: publish and cancel

### Why this challenge

A new auction is always a `DRAFT`, and nothing could change that, so created auctions never reached **Live auctions** (your auction 8 was stuck as a draft). Publishing and cancelling finish the seller side of Milestone 4 (US-11, US-12).

### What the tutor prepared

| Layer | Added |
|---|---|
| Controller | `POST /api/v1/auctions/{id}/publish` and `/cancel`, returning the updated `AuctionResponse` |
| Service (stubs) | `publishAuction(id)`, `cancelAuction(id)`: `@Transactional`, throwing `UnsupportedOperationException` (HTTP 501) |
| Entity (stubs) | `Auction.publish(Instant now)`, `Auction.cancel()` with the rules in Javadoc |
| Tests (disabled) | `AuctionTest` (6 methods, 9 cases with `@ParameterizedTest` / `@EnumSource`) and `AuctionServiceTest.PublishAndCancel` (4, `@Nested`) |
| Frontend | `publishAuction` / `cancelAuction` in `api.ts`; a shared `useAuctionAction` mutation hook that writes the response into the detail cache and invalidates every auction query; `AuctionActions` card with a cancel confirmation dialog; status chip coloured by status |
| Docs | Rules R15-R17, Challenge 5 brief and hints, README API table, roadmap, user stories |

### Design decisions

- **Rules on the entity, not the service** (a "rich domain model"). `Auction` has no `setStatus`, so the only way to change status is through methods that check the rules.
- **`now` is a parameter** of `publish`, because entities aren't Spring beans and can't have the `Clock` injected. The tests pass a fixed instant.
- **Action endpoints (`POST /{id}/publish`)** rather than `PATCH {status}`: the client asks for an action, and the server decides the resulting status (`ACTIVE` or `SCHEDULED`).
- **R10 (no cancelling once bids exist) is deferred to Milestone 6**, because bids don't exist yet.
- **Ownership checks are deferred to Milestone 5**: until sign-in exists, anyone can publish or cancel.

### Verified

- A reference solution was written temporarily, all 18 Challenge 5 test cases passed, and the stubs were restored. `mvnw clean` was run afterwards so no compiled solution was left in `target/`.
- Backend: 19 tests, 9 pass and 10 are skipped (Challenge 5). Frontend: 10/10 tests, `typecheck` and `lint` clean.

---

## Session 11 - 2026-11-05 to 2026-11-10 - Challenge 5: publish and cancel completed

### What you built

- `Auction.publish(now)` checks the rules in order and throws before changing anything: not a `DRAFT` gives `AUCTION_NOT_DRAFT`; `!endTime.isAfter(now)` gives `AUCTION_ALREADY_ENDED`; then `!startTime.isAfter(now)` decides `ACTIVE` vs `SCHEDULED`.
- `Auction.cancel()` rejects `COMPLETED` and `CANCELLED`, otherwise sets `CANCELLED`.
- `publishAuction` / `cancelAuction` load with `findById(...).orElseThrow(...)`, call the entity method and map the result. `publishAuction` reads the clock once, so the rules and the response use the same instant.

### Mistakes found in review, and what they taught

| Mistake | Lesson |
|---|---|
| `status.equals("DRAFT")` | An enum never equals a `String`, so every publish was rejected. Compare enum to enum, ideally with `==` (null-safe). |
| `endTime.isBefore(now)` and `startTime.isBefore(now)` | Both mishandle the boundary where the time equals `now`. "Not after" is `!x.isAfter(now)`. |
| `isEqual(...)` on `Instant` | Doesn't compile: `isEqual` exists on `LocalDateTime` and similar types, not on `Instant`. |
| `Instant.now()` in the service | Bypasses the injected `Clock`, so tests that freeze time can't control it. |
| Empty `""` error messages | The message reaches the API response; write a sentence for the user. |

### Review questions: key points

1. The status is stored without `save` because of **dirty checking**: `findById` inside `@Transactional` returns a *managed* entity, and Hibernate compares it with its snapshot and writes the `UPDATE` at commit. New objects still need `save`.
2. `now` is a parameter because entities aren't Spring beans (no injected `Clock`), and because tests need a fixed instant for the boundary cases.
3. `POST /{id}/publish` is a command: the server decides the resulting status. `PATCH {status}` would let the client choose any status and bypass the lifecycle rules.
4. Open design question: whether publishing a late draft should move `startTime` to `now`. You prefer moving it (bidders see an accurate start). The code currently keeps the original start time.
5. The missing rule is ownership (R1, extended to publish and cancel), added with sign-in in Milestone 5.

### Observed while testing

Auctions published before their start time stay `SCHEDULED` even after it passes, because nothing re-checks the clock. The auto-start/auto-close scheduler is part of Milestone 7.

### Verified

Backend: 22/22 tests. Frontend: 10/10 tests, `typecheck` and `lint` clean. Manual checks: publishing an ended draft returns 422 `AUCTION_ALREADY_ENDED`; an unknown ID returns 404; publishing and cancelling from the details page work.

Challenge 5 complete.

---

## Session 12 - 2026-11-11 - Challenge 6 starter: edit a draft auction

### Why this challenge

A seller could create a draft but not fix it. Your auction 8 ended on 2 November while still a draft, so it can never be published, and there was no way to correct it. Editing drafts (US-13) is the last business logic in Milestone 4.

### What the tutor prepared

| Layer | Added |
|---|---|
| DTO | `UpdateAuctionRequest`: the create fields without `sellerId` (ownership never changes) and without `status` (only publish and cancel change it) |
| Controller | `PUT /api/v1/auctions/{id}` returning the updated `AuctionResponse` |
| Service (stub) | `updateAuction(id, request)`, `@Transactional`, throwing `UnsupportedOperationException` (HTTP 501) |
| Entity (stub) | `Auction.updateDetails(...)` with the rules in Javadoc |
| Tests (disabled) | `AuctionTest.UpdateDetails` (4 methods, 7 cases, including a parameterized "not a draft" test) and `AuctionServiceTest.UpdateAuction` (6, including one that checks `save` is never called). The shared `draft(...)` helper moved to the outer test class. |
| Frontend | `AuctionForm` extracted from `CreateAuctionPage` without changing its look; `auctionForm.ts` holds the Zod schema and the form-to-request conversions; new `EditAuctionPage` (non-drafts show a "details are locked" message); **Edit** button in `AuctionActions`; `createAuction` / `updateAuction` in `api.ts`; `useAuctionAction` generalized to `useAuctionMutation<TVariables>`, which also backs `useCreateAuction` and `useUpdateAuction` |
| Docs | Rule R18, Challenge 6 brief and hints, README API table, roadmap, user stories |

### Design decisions

- **PUT, not PATCH:** the form always has every field, so the client sends the whole draft. A `null` optional field means "clear it"; a test checks this.
- **Time and category checks stay in the service** (they need the clock and the database). The status check lives on the entity, like `publish`. The brief asks you to share the create checks through private methods, not copies.
- **No optimistic-locking check yet:** the request has no `version`, so two simultaneous edits overwrite each other. This is review question 3, and the same idea comes back for bids in Milestone 6.
- **The Edit page's category select gets its starting value directly,** because MUI's select keeps its own state and React Hook Form can't set it through `register`.

### Verified

- A reference solution was written temporarily, and all 35 backend test cases passed (13 Challenge 6 cases). The stubs were restored and `mvnw clean` was run.
- With the stubs: 32 backend cases, 22 pass and 10 are skipped. `PUT /api/v1/auctions/9` returns 501. Frontend: 10/10 tests, `typecheck` and `lint` clean.
- Gotcha: `mvnw clean` or `test` while the app runs from IntelliJ or `spring-boot:run` can delete classes under the running app or the test run (`NoClassDefFoundError`). Rerunning fixed it; stopping the app first avoids it.

---

## Session 13 - 2026-11-11 to 2026-11-12 - Challenge 6: edit a draft auction completed

### What you built

- `Auction.updateDetails(...)` throws `AUCTION_NOT_EDITABLE` unless the auction is a `DRAFT`, then copies every argument (including `null`s) and resets `currentPrice` to the new `startingPrice`.
- `AuctionService.updateAuction` loads the auction or returns 404, reads the clock once, calls `validateTimeWindow(start, end, now)` and `findCategory(categoryId)`, delegates to the entity and maps the result.
- `createAuction` now uses the same two private methods, so R14 and the category lookup each live in one place.

### Mistakes found in review, and what they taught

| Mistake | Lesson |
|---|---|
| `AUCTION_NOT_EIDTABLE` | Error codes are part of the API contract; clients and tests compare them exactly. |
| `currentPrice` not reset | A field derived from another (`currentPrice` from `startingPrice`) must be updated with it. |
| Status check moved from the entity to the service | A rule in the service protects one call path; on the entity it protects every caller of a `public` method. |
| First helper took a `CreateAuctionRequest` and looked up seller and category, discarding them | Update couldn't reuse it, and create ran two extra queries. Helpers should take only the values they need and return what they find. |
| `request.categoryId()` inside the extracted helper | After an Extract Method, every variable must come from the new method's parameters. |

### Review questions: key points

1. The status check lives on the entity, so no caller can edit a live auction. The time and category checks stay in the service, because they need the `Clock` and the `CategoryRepository`.
2. With PUT the body is the whole new state, so `null` means "clear it"; a null-guard would make optional fields impossible to clear. The guard is right for PATCH, where a missing field means "leave it alone".
3. Today two tabs saving the same draft is "last write wins": the second silently overwrites the first (a lost update). `@Version` adds `AND version = ?` to the `UPDATE`, but it only catches this if the client sends back the version it loaded (in `AuctionResponse` and `UpdateAuctionRequest`), and a mismatch returns 409 Conflict. HTTP's equivalent is `ETag` / `If-Match`.
4. No `sellerId`: ownership never changes, and from Milestone 5 the seller comes from sign-in, not the client. No `status`: status changes only through publish and cancel.
5. On a live auction `currentPrice` is the highest accepted bid; resetting it would allow bids below an accepted one. R2 locks price rules after publishing.

### Verified

Backend: 35/35 tests (13 for Challenge 6, plus the Challenge 2 tests guarding the refactor). Manual checks against the running app: PUT to an unknown ID returns 404; a past start time returns 422; editing an `ACTIVE` auction returns 422.

Challenge 6 complete. All of Milestone 4's business logic is done; only the OpenAPI docs remain.

---

## Session 14 - 2026-11-12 - Challenge 7 starter: place a bid

### Why this challenge

You chose to start Milestone 6, the bidding engine, before sign-in (M5). Bidding is what Biddly is about. The roadmap advises getting one correct bidding workflow first. Until M5, a "Bid as" picker stands in for the signed-in user.

### What the tutor prepared

| Layer | Added |
|---|---|
| Database | `V2__create_bids.sql`: `bids` (insert-only, `placed_at` from the server clock), `ck_bids_amount_positive`, `idx_bids_auction_placed_at`, `idx_bids_bidder`; `auctions.leading_bidder_id` with `fk_auctions_leading_bidder` |
| Entity | `Bid` (no setters, never changed after insert, R6); `Auction.leadingBidder` (lazy, included in the entity graphs) |
| Entity (stub) | `Auction.acceptBid(bidder, amount, now)` with the rules R3-R5 in Javadoc, in a fixed order |
| Service | `BidService.listBids` (404 for an unknown auction), plus the stub `placeBid` |
| API | `GET` and `POST /api/v1/auctions/{id}/bids`; `PlaceBidRequest(bidderId, amount)` with `@DecimalMin("1.00")` and `@Digits(10, 2)`; `leadingBidder` in `AuctionResponse` |
| Tests (disabled) | `AuctionTest.AcceptBid` (7 methods, 10 cases; the self-bid test uses a *different* `User` object with the seller's id), `BidServiceTest.PlaceBid` (5), `BidApiIntegrationTest.storesAnAcceptedBidAndTheNewPrice` |
| Tests (enabled) | `BidServiceTest` history tests (2), `BidApiIntegrationTest` (empty history, unknown auction, missing amount); `AuctionApiIntegrationTest` now deletes bids before auctions |
| Frontend | `features/bids`: types, API, `useBids` (5 s polling) and `usePlaceBid` (invalidates every auction query, which includes the bid list); `BidPanel` replaces the disabled bid field on the details page; `BidHistory` below the description; `minimumNextBid` with tests |
| Docs | Challenge 7 brief, questions and hints; R3-R5 mapped; database design; README; roadmap; user stories |

### Design decisions

- **`leading_bidder_id` on `auctions`:** with only `currentPrice`, "has anyone bid?" can't be answered without a query, so the first-bid rule (R5) can't be decided on the entity. Like `current_price`, it's a denormalised copy of the top bid, kept in the same transaction.
- **The rules live on `Auction`, saving the bid lives in the service.** The auction owns its price; a `Bid` is a record of an accepted change. This is the same split as `publish` and `updateDetails`.
- **No `placedAt` in the request:** R4 is judged by the server clock, so the client can't backdate a bid.
- **Rule order:** not active, then closed, then self-bid, then too low. Tell the user the most basic problem first: there's no point saying "too low" on an auction that has ended.
- **Concurrency is left out on purpose:** two simultaneous bids can both pass today. That's Challenge 8 (`@Version` or `SELECT ... FOR UPDATE`), and review question 4 sets it up.
- **The bid list's query key sits under the auction's detail key,** so a successful bid only needs one `invalidateQueries(auctionKeys.all)`.

### Verified

- A reference solution was written temporarily and all 56 backend test cases passed (16 for Challenge 7). The stubs were restored and `mvnw clean` was run.
- With the stubs: 40 backend cases pass and 13 entries are skipped. Frontend: 12/12 tests, `tsc` and `lint` clean.
- Against the running app (V2 applied):
  - `GET .../9/bids` returns `[]`, and an unknown auction returns 404.
  - `POST .../bids` returns 501, or 400 when the amount is missing.
  - The details page shows "That's Challenge 7!" after **Place bid**.
- Auctions 9 and the seed auctions still say `ACTIVE` although their end time has passed, because nothing closes auctions until the Milestone 7 scheduler. The brief uses auction 16 for the happy path, and auction 9 to see R4 reject a bid.

---

## Session 15 - 2026-11-12 - Challenge 7: place a bid completed

### What you built

- `Auction.acceptBid(bidder, amount, now)`: four guard clauses (not active, closed, self-bid, too low), then `currentPrice = amount` and `leadingBidder = bidder`. The minimum is `currentPrice` with no leader, otherwise `currentPrice + minimumIncrement`. The `BID_TOO_LOW` message names it ("Your bid must be at least ₹69750.00").
- `BidService.placeBid`: load the auction and the bidder (404s), read `now` once, call `acceptBid`, then `bidRepository.save(new Bid(...))` and map the saved entity.

### Bugs found in review, and what each one taught

| Bug | Lesson |
|---|---|
| `status.equals("ACTIVE")`, an enum compared with a `String` (also in Challenge 5) | `equals` across types compiles and is always `false`. Compare enums with `==` or `equals(AuctionStatus.ACTIVE)` |
| `now.isAfter(endTime)`, then `endTime.isBefore(now)` | Both let a bid at exactly `endTime` through. "Before the end" is `now.isBefore(endTime)`, so reject `!now.isBefore(endTime)` |
| `bidder.getId() == seller.getId()` | `==` on `Long` compares references. It passes for ids from -128 to 127 (the `Long` cache) and silently fails above that |
| Minimum compared with `currentPrice` only; a too-low later bid fell through an `if` without throwing | A rule that rejects must `throw`. A skipped `if` is a silent no-op, and the service would have saved the bid |
| `findWithDetailsById` instead of `findById` | It worked in the app but broke the mocks. Stick to the contract the brief and tests set |
| `new Bid(...)` mapped but never saved | Loaded entities are saved by dirty checking; new ones need `repository.save`. Map the object `save` returns, because it has the generated id |
| Message `"at least+ ₹+" + currentPrice` | Stray `+` inside the string, and the wrong number (current price, not the minimum). Tests check codes, so read messages by hand |

The final refactor puts the rule in one place: compute `min`, then `if (amount.compareTo(min) < 0) throw ...`.

### Review answers (model answers)

1. **Order:** the user should see the most basic problem first, one that no change to the request can fix. Example: Asha bids ₹10 on ended auction 9, which is closed, a self-bid and too low; the answer is `AUCTION_CLOSED`. A fixed order also makes responses predictable and testable. *Your answer described each rule but not why the order matters.*
2. **Has it had a bid:** `leadingBidder == null`. `leading_bidder_id` is a foreign key to `users`. Price comparison can't work: R5 allows a first bid equal to the starting price, so after one bid `currentPrice == startingPrice` is still true, and a second bid at the same price would be accepted.
3. **No `placedAt`:** trust, not time zones. A client could backdate a bid past `endTime`, and device clocks drift. The server's clock is the only authority for R4 and for bid order. (Storing UTC and displaying local time is true, but it's a different point.)
4. **Two ₹1,050 bids at once:** both transactions read version 0 and pass the rules. The first commits (`UPDATE ... WHERE version = 0` gives version 1). The second's `UPDATE` matches zero rows, Hibernate throws an optimistic-locking exception, and that transaction rolls back, bid included. Data stays consistent, but the loser gets a **500** from the catch-all handler. *You expected a lost update; `@Version` was already on `Auction`, so it's a bad response, not bad data.* This is Challenge 8.
5. **`bidderId`:** from the authenticated principal (the JWT through Spring Security), never the request body. Otherwise anyone can bid as anyone. `sellerId` on create works the same way. Both leave the request records in Milestone 5.

### Verified

- Backend: 56/56 (16 for Challenge 7, plus every earlier test).
- Manual checks against the running app (auction 16):
  - A ₹64,999 first bid gets `BID_TOO_LOW`; ₹65,000 is accepted.
  - Meera's ₹65,400 gets `BID_TOO_LOW`; ₹65,500 is accepted.
  - Asha's bid gets `SELF_BIDDING_NOT_ALLOWED`.
  - Auction 9 gets `AUCTION_CLOSED`; `SCHEDULED` auction 10 gets `AUCTION_NOT_ACTIVE`.
  - An unknown auction or bidder returns 404; ₹0.50 returns 400.
  - A UI bid updated the price, the leader, the minimum and the history.
- Rejected bids were never stored.

Challenge 7 complete.

---

## Session 16 - 2026-11-12 - Challenge 8 starter: concurrent bids

### Why this challenge

The product requirements name concurrency as Biddly's core risk: "zero inconsistent auctions" with many simultaneous bids. Challenge 7's review answer 4 showed that `@Version` already protects the data, but the losing bidders get a 500. You chose the full version: fix the response (optimistic), then compare it with a lock (pessimistic).

### What the tutor prepared

| Layer | Added |
|---|---|
| Test | `BidConcurrencyIntegrationTest` (`RANDOM_PORT`, real HTTP through `RestClient`, 8 bidders, a `CountDownLatch` start gate). An `@Primary` test `Clock` sleeps 500 ms in `instant()`, so the race is deterministic, not luck |
| Test (enabled) | `keepsThePriceAndTheHistoryInAgreement`: bids stored = 201 responses, the price is the highest stored bid, the leader is its bidder |
| Tests (disabled) | Part 1: one 201, the rest 409 `CONCURRENT_UPDATE` or 422, no 5xx, one bid row. Part 2: one 201 and seven 422 `BID_TOO_LOW`, no 409s |
| Frontend | A `CONCURRENT_UPDATE` warning in `BidPanel`; `usePlaceBid` refreshes on `onSettled`, so a rejected bid also refreshes the price |
| Docs | Challenge 8 brief, questions and hints; rule R21; changelog |

### Design decisions

- **Make the race deterministic.** A race test that passes by luck proves nothing. Pausing the clock after the auction is loaded makes every request load version 0 before any commit. The same pause shows how a pessimistic lock queues requests: part 2's test takes about 8 × 0.5 s.
- **Test through HTTP, not the service.** The optimistic failure happens at commit, outside `placeBid`, so only a full request shows what the client receives.
- **A generic code, `CONCURRENT_UPDATE`.** The handler is global, so a draft edited in two tabs could trigger it too.
- **Both strategies, measured by the same test.** Optimistic fails fast with 409s; pessimistic waits and gives 422s because each request sees the winner's price.

### Verified

- Before any solution, with all tests enabled:
  - the invariant test passes;
  - part 1 fails with seven `500 INTERNAL_ERROR`;
  - part 2 fails with no 422s.
- A temporary reference solution showed each part's test can be passed:
  - handler only: part 1 passes, part 2 still fails;
  - plus the lock: both pass;
  - with the `BidServiceTest` stubs updated: 59/59.
- The code was then restored to the Challenge 7 commit.

---

## Session 17 - 2026-11-12 - Challenge 8: concurrent bids completed

### What you built

- **Part 1:** `@ExceptionHandler(OptimisticLockingFailureException.class)` returns 409 `CONCURRENT_UPDATE` with "Someone else updated this auction at the same moment. Refresh and try again."
- **Part 2:** `@Lock(LockModeType.PESSIMISTIC_WRITE) @Query("SELECT a FROM Auction a WHERE a.id = :id") Optional<Auction> findByIdForUpdate(Long id)`, used by `placeBid`. All five `BidServiceTest` auction stubs were moved to it.

### Bugs found in review, and what each one taught

| Bug | Lesson |
|---|---|
| `findByIdForUpdate` with no `@Query`: the app failed to start (`No property 'forUpdate' found for type 'Long'`) | Without a query, Spring Data derives one from the method name. One broken repository method stops the whole application context, so 11 unrelated tests failed |
| An empty `@Query` | The annotation holds the JPQL text. Empty means "none given", and Spring falls back to name derivation |
| `ex.getMessage()` in the 409 body | It leaked Hibernate's internals (entity class and id). Clients get a fixed message; details belong in the server log |
| `BidServiceTest` still stubbed `findById` | Mocks are tied to the exact method the code calls. An unstubbed call returns an empty `Optional`, so the result is a misleading "Auction 5 was not found" |

### Review answers (model answers)

1. **409 vs 422:**
   - With optimistic locking, all 8 requests read the same state (₹1,000, no leader, version 0) before anyone committed. All of them passed `acceptBid`, and the collision showed up only at commit (`WHERE version = 0` matched zero rows), which gives 409. No 422s, because nobody saw the winner's price.
   - With the lock, request 2's `SELECT ... FOR UPDATE` waits until request 1 commits, then reads the new price and leader. `acceptBid` correctly rejects ₹1,050, which gives 422.
   - Plain reads are not blocked; only other locking reads and writes of that row wait.
   - *Your answer named the outcomes but not the mechanism, and said other users couldn't read.*
2. **One generic handler:** it lives in a global `@RestControllerAdvice` and matches on the exception type. So it fires for any `@Version` entity on any endpoint: bids, two tabs editing a draft, a cancel racing the scheduler. A bid-specific name would mislead; `CONCURRENT_UPDATE` names the cause, and each screen words it for its context.
3. **Keep `@Version`: yes.** A locked bid always reads the latest version, so its own update can't conflict. But `@Version` still protects writers that don't lock: `updateAuction`, `cancelAuction` and the future scheduler. If a cancel collides with a bid, it gets 409 instead of overwriting the new price. If the cancel commits first, the bid then reads `CANCELLED` and gets `AUCTION_NOT_ACTIVE`.
4. **Slow work inside the lock:** it's about the transaction boundary, not the layer (`placeBid` is already in the service layer).
   - While the transaction is open it holds the row lock and a database connection, so every other bidder on that auction waits. That's 8 × 0.5 s in the test.
   - About 10 slow bids exhaust the Hikari pool, and the whole app stalls.
   - Side effects belong after commit: `@TransactionalEventListener(phase = AFTER_COMMIT)`, `@Async`, or a queue. That way nobody is emailed about a bid that rolled back.
   - *Your answer said "service layer".*
5. **The last 10 seconds:**
   - Pessimistic: many bidders on one row queue fairly, and each gets a definite answer in milliseconds. Optimistic would turn most of them into 409s and retries.
   - **Server-side retry of a 409** (for example Spring Retry `@Retryable` around the whole transaction) is acceptable for bids, with a limit and backoff. The retry re-reads fresh state and re-runs every rule, so it can't save anything wrong. It's never acceptable for draft edits, where it would silently cause the lost update `@Version` exists to prevent.
   - *Your idea of stopping bids 5 seconds early would break R4; the database needs milliseconds. The real practice behind it is anti-sniping (soft close), open question 2.*

### Verified

Backend: 59/59 after a clean build. The pessimistic race test took about 5.6 s, showing the bidders queued behind the lock.

Challenge 8 complete. Milestone 6 still needs the winner and idempotency keys; auto-closing comes with the scheduler.

