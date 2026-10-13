# Biddly

**Live auctions where every second counts.** Biddly is a real-time auction marketplace: sellers create timed auctions, and registered bidders compete until the auction closes. The server guarantees that only valid bids are accepted and that each auction has exactly one winner.

> Learning project, built milestone by milestone. See [docs/DEVELOPMENT_LOG.md](docs/DEVELOPMENT_LOG.md) for what was built, when and why.

## Tech stack

| Layer | Technology |
|---|---|
| Frontend | React 19, TypeScript 6, Vite 8, MUI 9, TanStack Query, React Router, React Hook Form + Zod |
| Backend | Java 21, Spring Boot 4.1 (Web MVC, Data JPA, Validation, Actuator), Flyway |
| Database | PostgreSQL 17 |
| Testing | JUnit 5, Mockito, Testcontainers, Vitest, Testing Library |
| Infrastructure | Docker Compose (now), Kubernetes (Milestone 11), GitHub Actions (Milestone 10) |

## Repository layout

```
biddly/
├── backend/                 Spring Boot API (com.biddly)
├── frontend/                React + TypeScript single-page app
├── infrastructure/
│   ├── docker/              Supporting files for Docker Compose
│   └── kubernetes/          Kubernetes manifests (Milestone 11)
├── exercises/               Stand-alone learning exercises (JS / TS)
├── docs/                    Product, architecture, database and learning docs
├── docker-compose.yml       Local PostgreSQL (+ optional pgAdmin)
└── CHANGELOG.md
```

## Prerequisites

- Java 21, Node.js 22+ (tested on 24), Docker Desktop (running)
- No local PostgreSQL install is needed: it runs in Docker.

## Run it locally

```powershell
# 1. Start PostgreSQL (first time downloads the image)
docker compose up -d

# 2. Start the backend (http://localhost:8081). Flyway creates the tables and loads sample data.
cd backend
.\mvnw.cmd spring-boot:run

# 3. In a second terminal, start the frontend (http://localhost:5180)
cd frontend
npm install
npm run dev
```

Open <http://localhost:5180>.

**Frontend only, no backend:** `npm run dev:mock` serves the UI from in-memory sample data.

**Explore the database visually:** `docker compose --profile tools up -d`, then open <http://localhost:5050> (pgAdmin). The server "Biddly (local)" is pre-registered; the password is `biddly`.

### Ports

| Service | Port | Why this port |
|---|---|---|
| Frontend (Vite) | 5180 | 5173 is often taken by other Vite apps |
| Backend (Spring Boot) | 8081 | 8080 is often taken by other Spring apps |
| PostgreSQL | 5432 | Default |
| pgAdmin | 5050 | Optional |

Override the backend port with `SERVER_PORT`; all backend settings can be overridden with environment variables (see `backend/src/main/resources/application.yml`).

## Tests

```powershell
cd backend;  .\mvnw.cmd test      # needs Docker running (Testcontainers starts a throwaway PostgreSQL)
cd frontend; npm test             # Vitest
cd frontend; npm run lint; npm run typecheck
```

## API (current)

| Method | Path | Status |
|---|---|---|
| GET | `/api/v1/auctions?status=ACTIVE&page=0&size=20` | Done |
| GET | `/api/v1/auctions/{id}` | Done |
| POST | `/api/v1/auctions` | **Challenge 2** (returns 501 until implemented) |
| GET | `/api/v1/categories` | Done |
| GET | `/actuator/health` | Done |

Errors always use the same JSON shape:

```json
{ "code": "NOT_FOUND", "message": "Auction 9999 was not found.", "timestamp": "2026-10-13T15:09:31Z", "fieldErrors": {} }
```

## Documentation

- [Roadmap and milestone status](docs/ROADMAP.md)
- [Development log](docs/DEVELOPMENT_LOG.md): step-by-step history of how the app was built
- [Challenges](docs/CHALLENGES.md): the business logic you implement yourself
- [Product requirements](docs/product-requirements.md) · [Business rules](docs/business-rules.md) · [User stories](docs/user-stories.md)
- [Architecture](docs/architecture.md) · [Database design](docs/database-design.md)
- [PostgreSQL primer](docs/learning/postgresql-primer.md)
