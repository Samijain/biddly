# Milestone 2 - TypeScript auction simulator

Convert the Challenge 1 simulator to TypeScript, then use the type system to make invalid states impossible.

```powershell
cd exercises/02-ts-auction-simulator
npm install           # once
npm run typecheck     # TypeScript: are the types right?
npm test              # Node: does the code behave right? (Node 24 runs .ts files directly)
npm run check         # both - this must be green to finish the milestone
```

## Files

| File | Who writes it | Purpose |
|---|---|---|
| `types.ts` | **You** (starter with TODOs) | The domain model as types |
| `simulator.ts` | **You** (start by copying `../01-js-auction-simulator/simulator.js`) | The logic, now typed |
| `simulator.test.ts` | Given | Runtime behaviour |
| `simulator.typecheck.ts` | Given | Compile-time guarantees: each `@ts-expect-error` line must be a type error |

The full brief and hints are in [docs/CHALLENGES.md](../../docs/CHALLENGES.md#challenge-4---typescript-domain-model-milestone-2).
