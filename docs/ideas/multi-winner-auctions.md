# Idea: multiple winners per auction (parked)

**Status:** parked, not scheduled. Written 2026-11-16 after a question about temple Abhishek auctions with 4 winners.
**If built:** a good Challenge 11.

## The need

Some items aren't unique. A temple Abhishek can have 4 places, and the 4 highest bidders should all win and pay the **same** amount. The seller chooses this while the auction is a `DRAFT`: one winner (today's behaviour) or N winners.

The design name is a **multi-unit, uniform-price auction**. Today's single-winner auction is the special case N = 1, so there should be one set of rules, not two.

## Decisions (proposed, to confirm before building)

| Question | Proposal | Why |
|---|---|---|
| What do winners pay? | The lowest winning bid (the cutoff) | Everyone pays the same, and it's easy to explain |
| Can one person hold two spots? | No, one spot per bidder | Abhishek is per family or devotee |
| Can a spot holder bid again? | No (`ALREADY_HOLDS_SPOT`). If pushed out, they bid again to get back in | Simple rules. Raising wouldn't change the price anyway |
| Tie at the cutoff | The earlier bid keeps its spot | Fair and predictable |
| Fewer bidders than spots at close | Everyone wins and pays the starting price | There was no competition |
| When can the number of winners change? | Only in `DRAFT` | Same as the price rules (R2, R18) |

## Worked example

4 spots, starting price ₹1,000, increment ₹100.

| Step | Bid | Spots after it | Next bid must be |
|---|---|---|---|
| 1 | A ₹1,000 | A | ≥ ₹1,000 (spots free) |
| 2 | B ₹1,000 | A, B | ≥ ₹1,000 |
| 3 | C ₹1,200 | A, B, C | ≥ ₹1,000 |
| 4 | D ₹1,500 | A, B, C, D (full) | ≥ ₹1,100 |
| 5 | E ₹1,100 | E pushes out B (tied with A, but A bid first). A, C, D, E | ≥ ₹1,100 |
| 6 | B ₹1,100 | B pushes out A. B, C, D, E | ≥ ₹1,200 |
| Close | | Winners B, C, D, E, each paying ₹1,100 | |

The minimum bid (R5) becomes:
- **Spots still free:** at least the starting price.
- **All spots full:** at least the cutoff + the increment.

For N = 1, that is exactly today's rule.

## Proposed rules

- **R24:** An auction has 1 to 50 winners, set in `DRAFT` and fixed once published.
- **R25:** Each bidder holds at most one spot. A spot holder can't bid again until they're pushed out.
- **R26:** When the spots are full, a new bid must beat the cutoff by the increment. It pushes out the lowest spot, and on a tie the most recent one.
- **R27:** At close, everyone holding a spot wins and pays the cutoff, or the starting price if there were fewer bidders than spots.
- **R7 changes** from "at most one winning bid" to "at most `winnerCount` winners, one per bidder".

## Data model

```sql
ALTER TABLE auctions ADD COLUMN winner_count SMALLINT NOT NULL DEFAULT 1
    CHECK (winner_count BETWEEN 1 AND 50);

-- Who holds a spot right now. At close, these rows are the winners.
CREATE TABLE auction_spots (
    auction_id BIGINT NOT NULL REFERENCES auctions (id),
    bidder_id  BIGINT NOT NULL REFERENCES users (id),
    bid_id     BIGINT NOT NULL REFERENCES bids (id),
    amount     NUMERIC(12, 2) NOT NULL,
    placed_at  TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (auction_id, bidder_id)          -- one spot per bidder
);
```

- **`auction_spots`** replaces `auctions.leading_bidder_id`. The migration copies each current leader into a spot row. For N = 1, it holds at most one row.
- **`auctions.current_price`** stays "the number to beat", which now means the **cutoff**. For N = 1, that's the highest bid, as today.
- **`bids`** doesn't change. Bids stay immutable (R6). A pushed-out bid stays in the history.

## Code changes

**Backend, `Auction` entity:**
- New field `winnerCount`. Validated on create, editable only in `updateDetails` (DRAFT), frozen by `publish`.
- `acceptBid`:
  1. Run the same status, closed and seller checks as today.
  2. Reject a bidder who already holds a spot (`ALREADY_HOLDS_SPOT`).
  3. Work out the minimum: the starting price if spots are free, otherwise the cutoff + the increment.
  4. If spots are full, remove the lowest spot. Ties: remove the most recent.
  5. Add the new spot, then recalculate `currentPrice` as the new cutoff.

  Spots map as `@OneToMany` on `Auction`, so the logic stays on the entity and stays unit-testable.
- `cancel`: "has bids" becomes "any spots taken" (R10).
- `complete`: marks the spots as winners. The final price is the cutoff, or the starting price if spots weren't full.

**Backend, other parts:**
- **Concurrency:** nothing new. `placeBid` already locks the auction row with `SELECT ... FOR UPDATE` (Challenge 8), so spot changes are serialised.
- **Idempotency:** unchanged (Challenge 10). A replay still returns the original bid.
- **API:**
  - `CreateAuctionRequest` and the update request get `winnerCount` (default 1).
  - `AuctionResponse` gets `winnerCount` and `spots: [{ bidder, amount }]`.
  - `leadingBidder` stays for a while as "the top spot", so the frontend doesn't break at once.

**Frontend:**
- **Create and edit draft:** a "Number of winners" field (default 1), optionally behind a "Multiple winners" switch.
- **Details page:**
  - "4 spots · ₹1,100 to get in", plus the list of spot holders.
  - "You hold spot 3" or "You were outbid".
  - Once completed: "Won by B, C, D, E at ₹1,100 each".
- `minimumNextBid` follows the same rule as R5 above.

## Tests to write

- The worked example above, step by step, as one entity test.
- The tie rule: two bids at the same cutoff amount; the most recent one is pushed out.
- A spot holder bidding again gets `ALREADY_HOLDS_SPOT`.
- Fewer bidders than spots at close: all win at the starting price.
- N = 1 behaves exactly like today. The existing `AuctionTest` and bid tests must still pass unchanged.
- `winnerCount` can't change after publishing.
- A race: several bidders competing for the last spot at the same moment. Exactly one gets it.

## Alternative to consider first

If the temple really wants a **fixed price for the first 4 devotees**, that's a booking with limited places, not an auction. It would be a separate and much simpler feature.
