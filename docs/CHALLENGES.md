# Challenges

You write the business logic; the boilerplate is ready around it. Each challenge has tests that define "done".
Ask for a hint whenever you are stuck, and ask for a code review when you are finished.

| # | Challenge | Area | Status |
|---|---|---|---|
| 1 | Console auction simulator | JavaScript | Open |
| 2 | Create an auction (`AuctionService.createAuction`) | Spring Boot | Open |
| 3 | Search, filter and sort auctions (`filterAuctions`) | TypeScript / React | Open |

---

## Challenge 1 - Console auction simulator (JavaScript)

**Folder:** `exercises/01-js-auction-simulator/`
**Run tests:** `cd exercises/01-js-auction-simulator; node --test`

Implement `createAuction`, `placeBid`, `getHighestBid`, `closeAuction` and `getWinner` in `simulator.js`.
These are the same rules the real backend will enforce, so this is a dry run of the bidding engine.

**You will practise:** objects and arrays, immutability (return new objects instead of changing the old ones), `reduce`, throwing errors, and closures (for generating IDs).

<details><summary>Hint 1</summary>

`placeBid` should not modify `auction`. Return `{ ...auction, bids: [...auction.bids, newBid], currentPrice: amount }`.
</details>

<details><summary>Hint 2</summary>

The minimum valid bid is `auction.bids.length === 0 ? auction.startingPrice : auction.currentPrice + auction.minimumIncrement`. Do you agree with that rule? It's one of the open business questions.
</details>

**Stretch:** convert it to TypeScript (Milestone 2) and use a *discriminated union* for the auction status.

---

## Challenge 2 - Create an auction (Spring Boot)

**File:** `backend/src/main/java/com/biddly/auction/AuctionService.java` → `createAuction`
**Tests:** `backend/src/test/java/com/biddly/auction/AuctionServiceTest.java` (remove `@Disabled`)
**Run:** `cd backend; .\mvnw.cmd test -Dtest=AuctionServiceTest`

Acceptance criteria:

1. `startTime` in the past → `BusinessRuleException("START_TIME_IN_PAST", ...)`
2. `endTime` not after `startTime` → `BusinessRuleException("INVALID_TIME_WINDOW", ...)`
3. Unknown seller → `ResourceNotFoundException("User", id)`
4. Unknown category, if one is given → `ResourceNotFoundException("Category", id)`
5. Save a **DRAFT** auction whose `currentPrice == startingPrice` and return an `AuctionResponse`.

When it works, the **Sell an item** page in the UI creates real auctions and redirects you to them.

**Questions to answer in your PR description:**

- Why do we validate `endTime > startTime` in the service when the database has a `CHECK` constraint too?
- Why is "now" read from the injected `Clock` instead of `Instant.now()`?
- What does `@Transactional` on `createAuction` give us that the class-level `@Transactional(readOnly = true)` doesn't?
- Should there be a minimum auction duration (for example 1 hour) or a maximum (for example 30 days)? If yes, add a rule and a test.

<details><summary>Hint 1</summary>

`userRepository.findById(id)` returns an `Optional<User>`. Look at `.orElseThrow(...)`.
</details>

<details><summary>Hint 2</summary>

Validate the cheap things (times) **before** hitting the database. The tests check that `save` is never called for invalid input.
</details>

<details><summary>Hint 3</summary>

`Auction` already has a constructor that sets `status = DRAFT` and `currentPrice = startingPrice`. Use `auctionMapper.toResponse(saved, clock.instant())` to build the response.
</details>

---

## Challenge 3 - Search, filter and sort (TypeScript)

**File:** `frontend/src/features/auctions/filterAuctions.ts`
**Tests:** `frontend/src/features/auctions/filterAuctions.test.ts` (change `describe.skip` to `describe`)
**Run:** `cd frontend; npm run test:watch`

The UI controls on the **Live auctions** page already exist but do nothing until you implement this pure function.

**You will practise:** `filter`, `toSorted`, non-mutation, `readonly` arrays, string normalisation, and union types (`AuctionSort`). Can you use `switch` so TypeScript errors if a new sort option is added but not handled? (Look up the `never` exhaustiveness check.)

**Think about it:** this filters on the client, which only works because we load at most 50 auctions. When should filtering move to the server, and what would the API look like?
