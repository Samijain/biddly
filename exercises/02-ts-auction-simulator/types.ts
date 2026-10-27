/**
 * Milestone 2 - the auction domain, described in types.
 *
 * Node runs this file by deleting the types ("type stripping"), so types cost nothing at runtime
 * and protect nothing at runtime either. Their job is to make mistakes impossible to *write*.
 * `npm run typecheck` is the judge: simulator.typecheck.ts lists what your types must allow and forbid.
 *
 * Rule from tsconfig ("erasableSyntaxOnly"): no `enum`, no `namespace`, no constructor parameter
 * properties - only syntax that can be deleted without changing behaviour.
 */

// ---------------------------------------------------------------------------------------------
// WORKED EXAMPLE - read it, then write the rest in the same style.
// `readonly` makes `bid.amount = 5` a compile error. Accepted bids must never change (rule R6).
// ---------------------------------------------------------------------------------------------
export interface Bid {
  readonly id: string
  readonly bidderId: string
  readonly amount: number
  readonly placedAt: Date
}

// TODO 1 - AuctionStatus: exactly 'OPEN' or 'CLOSED'. Use a union of string literals.
//          (Why not `string`? Look at the `'OPENED'` line in simulator.typecheck.ts.)

// TODO 2 - BidRejectionCode: the three codes placeBid can reject with.

// TODO 3 - The fields every auction has, whatever its status:
//          id, title, sellerId, startingPrice, minimumIncrement, currentPrice, bids.
//          Everything readonly. Hint: a readonly property holding an ordinary array still allows
//          `.push()`. Which array type forbids it?

// TODO 4 - OpenAuction: the shared fields + status 'OPEN'.

// TODO 5 - ClosedAuction: the shared fields + status 'CLOSED' + winningBid (a Bid, or null when nobody bid).

// TODO 6 - Auction: either an OpenAuction or a ClosedAuction.
//          `status` is the "discriminant": checking it tells TypeScript which one you have.

// TODO 7 - CreateAuctionInput: the 4 fields createAuction receives.
//          Derive it from your shared fields with the utility type Pick<...> instead of retyping them.
//          When a field's type changes later, the input type follows automatically.

// TODO 8 - Result<T, E>: a generic type for "it worked, here is a T" OR "it failed, here is an E".
//          The tests read `result.ok`, `result.value` and `result.error`.
