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

### Next session

1. You: Challenges 1, 2 and 3 (see [CHALLENGES.md](CHALLENGES.md)).
2. Answer the open questions in [business-rules.md](business-rules.md#open-questions-for-you).
3. Then: Milestone 4 completion (publish/cancel auction, OpenAPI docs) and the start of the bids table.
