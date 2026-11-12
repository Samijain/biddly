# User stories

Format: *As a [role], I want [goal] so that [benefit].* Each story lists acceptance criteria (AC) and the milestone that delivers it.

## Browsing

**US-01 Browse live auctions** (M0 ✅)
As a visitor, I want to see all active auctions so that I can find something to bid on.
- AC: Shows image, title, category, current price, minimum increment and a live countdown.
- AC: Sorted by "ending soon" by default.
- AC: Shows skeleton cards while loading and a friendly message if the server is unreachable.

**US-02 Search and filter** (M3 ✅, Challenge 3)
As a visitor, I want to search by keyword, filter by category and change the sort order so that I quickly find relevant items.

**US-03 View auction details** (M0 ✅)
As a visitor, I want to see the full description, the price rules and the closing time so that I can decide whether to bid.

**US-04 View bid history** (M6)
As a visitor, I want to see every accepted bid in order so that I can trust the auction is fair.
- AC: Newest bid first, showing amount, bidder display name and time in my local time zone.

## Selling

**US-10 Create a draft auction** (M4, Challenge 2)
As a seller, I want to create an auction with a title, description, image, starting price, minimum increment and time window so that I can sell an item.
- AC: Saved as DRAFT; invalid input shows a message next to the right field.

**US-11 Publish an auction** (M4 ✅, Challenge 5)
As a seller, I want to publish my draft so that bidders can see it.

**US-12 Cancel an auction** (M4 ✅, Challenge 5)
As a seller, I want to cancel my auction before anyone has bid.
- AC: Blocked once a valid bid exists (R10, added in M6 when bids exist).

**US-13 Edit a draft auction** (M4 ✅, Challenge 6)
As a seller, I want to change my draft before publishing so that I can fix mistakes.
- AC: Only the owner can edit (R1, added in M5), and only while the auction is `DRAFT` (R18). Price rules are locked after publishing (R2).

**US-14 See the result** (M7)
As a seller, I want to see when my auction has closed and who won so that I can complete the sale.
- AC: Moves to `COMPLETED` automatically at `endTime`, with one winner (R7) or none if nobody bid (R8).

## Bidding

**US-20 Place a bid** (M6)
As a bidder, I want to place a bid and get an immediate yes/no answer with the reason so that I know where I stand.
- AC: Rejection codes: `BID_TOO_LOW`, `AUCTION_CLOSED`, `SELF_BIDDING_NOT_ALLOWED`.
- AC: Double-clicking "Place bid" never creates two bids (idempotency key).

**US-21 See live price changes** (M7)
As a bidder, I want the price to update without refreshing so that I can react to competing bids.

**US-22 Outbid notification** (M7)
As a bidder, I want to be told when I've been outbid.

**US-23 Win an auction** (M7)
As a bidder, I want to know when I've won and see it in "My wins".

**US-24 My bids** (M6)
As a bidder, I want a list of auctions I've bid on, showing whether I'm currently winning, so that I can follow them in one place.

**US-25 Watch an auction** (M7)
As a bidder, I want to save auctions to a watchlist so that I can come back before they end.

## Accounts

**US-30 Register / sign in / sign out** (M5)
**US-31 Protected pages redirect to sign-in** (M5)

## Administration

**US-40 Suspend a user** (post-MVP)
**US-41 View audit history** (post-MVP)

**US-42 Review reported auctions** (post-MVP)
As an administrator, I want to review auctions that users have reported so that I can remove fraudulent listings.
