# Biddly frontend

React 19 + TypeScript + Vite + MUI. See the [root README](../README.md) for the full setup.

| Script | Does |
|---|---|
| `npm run dev` | Dev server on <http://localhost:5180>, proxies `/api` to the backend on 8081 |
| `npm run dev:mock` | Dev server with in-memory sample data (no backend needed) |
| `npm test` / `npm run test:watch` | Vitest |
| `npm run typecheck` | TypeScript project build check |
| `npm run lint` | oxlint |
| `npm run build` | Production build into `dist/` |

Folder conventions are described in [docs/architecture.md](../docs/architecture.md#frontend-structure).
