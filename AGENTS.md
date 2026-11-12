# AGENTS.md

Guidance for AI coding agents working on Biddly. Read this before making changes.

## What Biddly is

A real-time auction marketplace: sellers create timed auctions, bidders compete until they close, and each auction ends with at most one winner. The core guarantee is **correct bidding under concurrency**. See [docs/product-requirements.md](docs/product-requirements.md).

This is also a **learning project**. The owner writes the business logic; the agent acts as a tutor, prepares the surrounding code, and reviews.

## Tutor mode (most important)

- **Don't solve open challenges unless asked.** Open challenges are listed in [docs/CHALLENGES.md](docs/CHALLENGES.md). Their stubs (marked `CHALLENGE n` in the code) and their skipped or `@Disabled` tests belong to the owner. Give hints, ask guiding questions, and review their attempts instead.
- If the owner explicitly asks you to fix or finish something, do it, then explain what you changed and why, so they can learn from it.
- When explaining a concept, tie it to the code in this repo rather than a generic example.
- Prepare boilerplate (wiring, configuration, UI scaffolding, test setup) so challenges can focus on the business rules.

## Repository layout

```
backend/         Spring Boot API, package com.biddly (modules: auction, category, user, common, configuration)
frontend/        React + TypeScript SPA (app/, components/, features/, hooks/, lib/, pages/)
exercises/       Stand-alone JS/TS learning exercises (run with node --test)
infrastructure/  docker/ support files, kubernetes/ manifests (Milestone 11)
docs/            PRD, roadmap, business rules, user stories, architecture, database design, dev log
```

## Commands (Windows PowerShell)

```powershell
docker compose up -d                          # PostgreSQL 17 on :5432 (pgAdmin: --profile tools, :5050)
cd backend;  .\mvnw.cmd spring-boot:run       # API on :8081
cd backend;  .\mvnw.cmd test                  # needs Docker running (Testcontainers)
cd frontend; npm run dev                      # UI on :5180, proxies /api to :8081
cd frontend; npm run dev:mock                 # UI with in-memory data, no backend
cd frontend; npm test; npm run lint; npm run typecheck
cd exercises/02-ts-auction-simulator; npm run check
```

Use `;` to chain commands, not `&&`. Ports 8080 and 5173 are taken on the dev machine, so don't change the ports back.

## Backend conventions

- **Layering:** Controller (HTTP and `@Valid` only), Service (business rules and transactions), Repository (Spring Data JPA), Entity, DTO (Java `record`), Mapper. Business rules never go in controllers, and entities are never returned from the API.
- **Errors:** throw `BusinessRuleException(code, message)` or `ResourceNotFoundException(type, id)`. `GlobalExceptionHandler` turns every exception into the `ApiError` JSON shape. Error codes are `UPPER_SNAKE_CASE`, for example `BID_TOO_LOW`.
- **Time:** read "now" from the injected `Clock`, never `Instant.now()`. Store `Instant` / `TIMESTAMPTZ` in UTC.
- **Money:** use `BigDecimal` and `NUMERIC(12,2)`. Never use `double` or `float`.
- **Schema:** change it only with a new Flyway migration in `src/main/resources/db/migration` (`V<n>__description.sql`). Never edit a migration that has already run. Hibernate runs with `ddl-auto: validate`. Development seed data lives in `db/seed` and loads only with the `dev` profile.
- **Transactions:** services are `@Transactional(readOnly = true)` at class level; write methods add `@Transactional`.
- **Configuration:** environment variables with local defaults in `application.yml`. No secrets in Git.
- **Tests:** unit tests use Mockito; integration tests use Testcontainers against real PostgreSQL.

## Frontend conventions

- Server state goes in TanStack Query hooks (`features/<feature>/hooks.ts`), and API calls go through `lib/http.ts`. Form and UI state stay in components or React Hook Form with Zod.
- Code is organized by feature: `features/<feature>/` holds `types.ts`, `api.ts`, `hooks.ts`, `components/` and pure logic with its tests next to it.
- Use MUI components and the theme in `app/theme.ts`. **Don't change the existing look** (colours, header styles, spacing) unless asked; the owner has noticed unrequested visual changes before.
- Keep the UI mobile-first, keyboard accessible, and working in light and dark themes. Show skeletons while loading and clear messages on errors.
- Format money with `en-IN` / INR, and show times in the viewer's local time zone (see `lib/format.ts`).
- TypeScript is strict: prefer `unknown` over `any`, use `readonly` data, and use discriminated unions with exhaustive `switch` statements.

## Business rules

The server enforces every rule in [docs/business-rules.md](docs/business-rules.md) (R1-R18); the UI only mirrors them. Key rules:

- Sellers can't bid on their own auctions.
- The **first** bid must be at least `startingPrice`, so a bid equal to the starting price is valid. Every **later** bid must be at least `currentPrice + minimumIncrement`. Every bid must arrive before `endTime`, by the server clock.
- Accepted bids are immutable, and an auction has at most one winner.

Don't decide the open questions in that file on the owner's behalf. Ask instead.

## Documentation is part of "done"

For every meaningful change:

- Add a dated session entry at the bottom of [docs/DEVELOPMENT_LOG.md](docs/DEVELOPMENT_LOG.md): what, when, how (commands) and why.
- Add a short entry under `[Unreleased]` in [CHANGELOG.md](CHANGELOG.md) (Keep a Changelog format).
- Update [docs/ROADMAP.md](docs/ROADMAP.md) and [docs/CHALLENGES.md](docs/CHALLENGES.md) status when a milestone or challenge changes.
- Keep user-story IDs (US-xx) and rule IDs (Rxx) consistent across the PRD, user stories and business rules.

## Before finishing a change

- Run the relevant tests, lint and type checks, and report what passed or failed.
- Commit only when asked. Use Conventional Commit messages, for example `feat(auction): ...`, `fix(exercises): ...` or `docs: ...`.
