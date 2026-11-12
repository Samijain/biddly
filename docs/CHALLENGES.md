# Challenges

You write the business logic; the boilerplate is ready around it. Each challenge has tests that define "done".
Ask for a hint whenever you are stuck, and ask for a code review when you are finished.

| # | Challenge | Area | Status |
|---|---|---|---|
| 1 | Console auction simulator | JavaScript | ✅ Done (2026-10-27): 11/11 tests, edge cases verified |
| 2 | Create an auction (`AuctionService.createAuction`) | Spring Boot | ✅ Done (2026-10-30): 5/5 unit tests, verified end to end |
| 3 | Search, filter and sort auctions (`filterAuctions`) | TypeScript / React | ✅ Done (2026-11-04): 7/7 tests, exhaustive `never` check |
| 4 | TypeScript domain model (Milestone 2) | TypeScript | Done (2026-10-27) |
| 5 | Publish and cancel an auction (`Auction.publish`, `Auction.cancel`) | Spring Boot / JPA | ✅ Done (2026-11-10): 22/22 backend tests, verified end to end |
| 6 | Edit a draft auction (`AuctionService.updateAuction`, `Auction.updateDetails`) | Spring Boot / JPA | ✅ Done (2026-11-12): 35/35 backend tests, shared validation refactor |

---

## Challenge 1 - Console auction simulator (JavaScript)

**Folder:** `exercises/01-js-auction-simulator/`
**Run tests:** `cd exercises/01-js-auction-simulator; node --test`

Implement `createAuction`, `placeBid`, `getHighestBid`, `closeAuction` and `getWinner` in `simulator.js`.
These are the same rules the real backend will enforce, so this is a dry run of the bidding engine.

**You will practise:** objects and arrays, immutability (return new objects instead of changing the old ones), `reduce`, throwing errors, and closures (for generating IDs).

Hint 1

`placeBid` should not modify `auction`. Return `{ ...auction, bids: [...auction.bids, newBid], currentPrice: amount }`.

Hint 2

The minimum valid bid is `auction.bids.length === 0 ? auction.startingPrice : auction.currentPrice + auction.minimumIncrement`. Do you agree with that rule? It's one of the open business questions.

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

Hint 1

`userRepository.findById(id)` returns an `Optional<User>`. Look at `.orElseThrow(...)`.

Hint 2

Validate the cheap things (times) **before** hitting the database. The tests check that `save` is never called for invalid input.

Hint 3

`Auction` already has a constructor that sets `status = DRAFT` and `currentPrice = startingPrice`. Use `auctionMapper.toResponse(saved, clock.instant())` to build the response.

---

## Challenge 3 - Search, filter and sort (TypeScript)

**File:** `frontend/src/features/auctions/filterAuctions.ts`
**Tests:** `frontend/src/features/auctions/filterAuctions.test.ts` (change `describe.skip` to `describe`)
**Run:** `cd frontend; npm run test:watch`

The UI controls on the **Live auctions** page already exist but do nothing until you implement this pure function.

**You will practise:** `filter`, `toSorted`, non-mutation, `readonly` arrays, string normalisation, and union types (`AuctionSort`). Can you use `switch` so TypeScript errors if a new sort option is added but not handled? (Look up the `never` exhaustiveness check.)

**Think about it:** this filters on the client, which only works because we load at most 50 auctions. When should filtering move to the server, and what would the API look like?

---

## Challenge 4 - TypeScript domain model (Milestone 2)

**Folder:** `exercises/02-ts-auction-simulator/`
**Run:** `npm install` once, then `npm run check` (type check + tests). Aim for green on both.

In Challenge 1, the *tests* caught your mistakes after you ran the code. Here the *compiler* catches them as you type. The goal isn't "JavaScript with type annotations". It's **making invalid states unrepresentable**: an auction that is closed *and* receiving bids should be impossible to write, not just rejected at runtime.

### Steps

1. **Types first.** Complete the 8 TODOs in `types.ts`. Then `npm run typecheck` will complain that `simulator.ts` is missing, which is expected.
2. **Port the logic.** Copy your `simulator.js` to `simulator.ts`, and add parameter and return types until `npm run typecheck` is happy.
3. **New behaviour the tests require:**
   - `placeBid` accepts only an `OpenAuction`; `closeAuction` turns an `OpenAuction` into a `ClosedAuction`; `getWinner` accepts only a `ClosedAuction`.
   - `closeAuction` stores `winningBid` on the closed auction, so the winner is decided once, at closing time.
   - `isOpen(auction)`: a **type guard** (`auction is OpenAuction`).
   - `tryPlaceBid(...)` returns `Result<OpenAuction, BidRejectedError>` instead of throwing.
   - `parseCreateAuctionInput(value: unknown)` checks untrusted data (like JSON from a request) and returns a `CreateAuctionInput`, or throws an error naming the bad field.
4. Keep the runtime checks (`status !== 'OPEN'` and so on) even though the types forbid those cases. The `untrusted(...)` tests show why.

### You will practise

Literal unions · interfaces vs type aliases · `readonly` / `ReadonlyArray` · discriminated unions and narrowing · utility types (`Pick`) · generics (`Result<T, E>`) · type guards (`x is T`) · `unknown` vs `any` · strict mode · classes under `erasableSyntaxOnly`.

### Questions to answer when you're done

- `parseCreateAuctionInput` takes `unknown`, not `any`. What would `any` let you get away with that `unknown` doesn't?
- `untrusted<T>(value)` uses `as T`. What does `as` actually do at runtime? Why is it dangerous?
- Throwing (`placeBid`) or returning a `Result` (`tryPlaceBid`): which would you use in the React UI, and why?

Hint: TODO 3 (readonly arrays)

`readonly bids: Bid[]` stops `auction.bids = []`, but still allows `auction.bids.push(x)`. You need the array itself to be read-only: `readonly Bid[]` or `ReadonlyArray<Bid>`.

Hint: sharing fields between OpenAuction and ClosedAuction

Put the shared fields in one interface, then `interface OpenAuction extends Shared { readonly status: 'OPEN' }`. An intersection type (`Shared & { ... }`) works too. Look up the difference.

Hint: BidRejectedError won't compile

In JavaScript you wrote `this.code = code` in the constructor. TypeScript needs the field declared on the class first (`readonly code: BidRejectionCode`). The shortcut `constructor(readonly code: ...)` is forbidden by `erasableSyntaxOnly`. Why might it count as "non-erasable"?

Hint: reduce returns the wrong type

`bids.reduce((best, bid) => ..., null)` makes TypeScript think the result is always `null`. Tell it the accumulator type: `bids.reduce<Bid | null>(...)`.

Hint: parseCreateAuctionInput

Narrow step by step: `typeof value === 'object'`, then `value !== null`, then `!Array.isArray(value)`. After that, check each field's `typeof`. Remember `Number.isFinite` from Challenge 1.

---

## Challenge 5 - Publish and cancel an auction (Spring Boot / JPA)

**Files:**

- `backend/src/main/java/com/biddly/auction/Auction.java`: `publish(Instant now)` and `cancel()`
- `backend/src/main/java/com/biddly/auction/AuctionService.java`: `publishAuction(id)` and `cancelAuction(id)`

**Tests:** remove `@Disabled` from `AuctionTest` (entity rules) and from the nested `PublishAndCancel` class in `AuctionServiceTest`.
**Run:** `cd backend; .\mvnw.cmd test -Dtest="AuctionTest,AuctionServiceTest*"`

The endpoints (`POST /api/v1/auctions/{id}/publish` and `/cancel`) and the **Publish** / **Cancel auction** buttons on the details page are ready. Until you finish, the buttons show "This is your backend Challenge 5!".

This time the rules live **on the entity**, not in the service. The service only loads the auction, asks it to change, and returns the result. An `Auction` then protects its own lifecycle: no other code can put it into an invalid status, because there is no `setStatus`.

### Acceptance criteria

`Auction.publish(now)` (rules R15, R16):

1. Not a `DRAFT` -> `BusinessRuleException("AUCTION_NOT_DRAFT", ...)`
2. `endTime` not after `now` -> `BusinessRuleException("AUCTION_ALREADY_ENDED", ...)`
3. `startTime` not after `now` -> `ACTIVE`; otherwise -> `SCHEDULED`

`Auction.cancel()` (rule R17):

1. `COMPLETED` or `CANCELLED` -> `BusinessRuleException("AUCTION_NOT_CANCELLABLE", ...)`
2. Otherwise -> `CANCELLED`

`AuctionService.publishAuction(id)` and `cancelAuction(id)`:

1. Load with `auctionRepository.findById(id)`; unknown id -> `ResourceNotFoundException("Auction", id)`
2. Call the entity method, then return `auctionMapper.toResponse(...)`

When it works, open your draft (auction 8) and click **Publish**. Its end time has already passed, so you should get `AUCTION_ALREADY_ENDED`. Create a new auction, publish it, and it appears on **Live auctions**.

### Learn first

- **Rich domain model vs anemic model:** why `auction.publish(now)` is better than `auction.setStatus(ACTIVE)` called from a service.
- **Encapsulation:** private fields, no setters, methods that keep the object valid.
- **Why** `now` **is a parameter:** entities are created by `new` and by Hibernate, not by Spring, so they can't have the `Clock` injected.
- **JPA dirty checking:** inside a `@Transactional` method, a loaded entity is *managed*. Hibernate notices that you changed a field and writes an `UPDATE` at commit. Do you need to call `save` at all?
- `@Version` **(optimistic locking):** look at the `version` field on `Auction`. What happens if two people publish and cancel the same auction at the same moment?
- **JUnit 5 parameterized tests:** `@ParameterizedTest` with `@EnumSource`, used in `AuctionTest`.
- `@Nested` **test classes** in JUnit 5, used in `AuctionServiceTest`.

### Questions to answer in your PR description

1. You didn't call `auctionRepository.save(...)`, yet the new status is stored. Why? (If you did call it, try removing it.)
2. Why does `publish` take `Instant now` instead of calling `Instant.now()` inside the entity?
3. Why are the publish and cancel endpoints `POST /{id}/publish` rather than `PATCH /{id}` with `{"status": "ACTIVE"}`?
4. A draft whose `startTime` has already passed becomes `ACTIVE` straight away, keeping its old start time. Should publishing move `startTime` to `now` instead? What would bidders see in each case?
5. There's no sign-in yet, so anyone can publish or cancel any auction. Which rule from `business-rules.md` is missing, and which milestone adds it?

Hint 1

The tests reach other statuses with `ReflectionTestUtils.setField(auction, "status", ...)` because there's no setter. In your entity, assign the `status` field directly. You are inside the class.

Hint 2

Check the rules in the order listed. Each test checks that the status is **unchanged** after a rejected call, so throw before you assign anything.

Hint 3

`AuctionService.getById` already shows the "load or 404" pattern with `orElseThrow`. Read "now" once from the `clock` and use the same value for `publish(now)` and `toResponse(auction, now)`.

---

## Challenge 6 - Edit a draft auction (Spring Boot / JPA)

**Files:**

- `backend/src/main/java/com/biddly/auction/Auction.java`: `updateDetails(...)`
- `backend/src/main/java/com/biddly/auction/AuctionService.java`: `updateAuction(id, request)`, plus a small refactor of `createAuction`

**Tests:** remove `@Disabled` from the nested `UpdateDetails` class in `AuctionTest` and the nested `UpdateAuction` class in `AuctionServiceTest`.
**Run:** `cd backend; .\mvnw.cmd test -Dtest="AuctionTest,AuctionServiceTest*"` (stop the running backend first, or restart it afterwards).

Ready for you: `PUT /api/v1/auctions/{id}` with an `UpdateAuctionRequest` body, an **Edit** button on a draft's details page, and an **Edit draft** page that reuses the "Sell an item" form. Until you finish, saving shows "The server can't do this yet".

Sellers make typos. While an auction is a `DRAFT`, nobody can see it or bid on it, so every detail may change. Once it's published, bidders rely on the price rules (R2), so the details lock.

### Acceptance criteria

`Auction.updateDetails(category, title, description, coverImageUrl, startingPrice, minimumIncrement, startTime, endTime)` (rules R2, R18):

1. Not a `DRAFT` -> `BusinessRuleException("AUCTION_NOT_EDITABLE", ...)`, and nothing changes
2. Otherwise copy **every** argument into its field, including `null`s, and set `currentPrice` to the new `startingPrice`

`AuctionService.updateAuction(id, request)`:

1. Load with `auctionRepository.findById(id)`; unknown id -> `ResourceNotFoundException("Auction", id)`
2. The same time rules as create (R14): `START_TIME_IN_PAST`, `INVALID_TIME_WINDOW`
3. If `categoryId` is given, the category must exist -> `ResourceNotFoundException("Category", categoryId)`
4. Call `auction.updateDetails(...)` and return `auctionMapper.toResponse(...)`. No `save`: a test checks that.

**Don't copy steps 2 and 3 from `createAuction`.** Move them into private methods, and make both `createAuction` and `updateAuction` call them. The Challenge 2 tests keep passing if the refactor is right.

When it works, open your draft auction 8 ("Shanti Dhara Chandrapbhu Bhagwan"). Its end time passed on 2 November, so publishing it fails with `AUCTION_ALREADY_ENDED`. Click **Edit**, give it new start and end times, save, then publish it. It finally reaches **Live auctions**.

### Learn first

- **PUT vs PATCH:** PUT replaces the whole resource, so a missing optional field means "clear it". PATCH changes only the fields that are sent. Which one does `clearsOptionalFieldsThatAreSentAsNull` test?
- **Extract Method refactoring and DRY:** one rule, one place. What happens later if the time rule changes and it lives in two places?
- **Where each check belongs:** Bean Validation on the request record (shape of one field), the service (needs the clock or the database), the entity (its own state). Sort this challenge's rules into those three.
- **Lost updates and `@Version`:** two tabs open the same draft and both save. The second silently overwrites the first. Read how optimistic locking would turn that into an error.
- **Order of checks:** which error is most useful to the user when a request breaks two rules at once?

### Questions to answer in your PR description

1. Which checks did you put in the service and which in the entity, and why?
2. Why would `if (description != null) this.description = description;` be wrong here? For which HTTP method would it be right?
3. Two tabs edit the same draft and both click **Save**. What happens today? What would the client need to send for `@Version` to catch it?
4. Why does `UpdateAuctionRequest` have no `sellerId` and no `status`?
5. Editing resets `currentPrice` to the new `startingPrice`. Why is that safe for a draft but dangerous for an active auction?

Hint 1

Two private methods are enough, for example `validateTimeWindow(Instant start, Instant end, Instant now)` and `findCategory(Long categoryId)`, which returns `null` when the id is `null`. Refactor `createAuction` first and run the Challenge 2 tests before you write `updateAuction`.

Hint 2

The entity check works like `publish`: throw before assigning anything. Inside the class you can assign fields directly (`this.title = title;`).

Hint 3

The service tests freeze time at `NOW`. Read the clock once in `updateAuction` and pass that same instant to the time check and to `toResponse`.
