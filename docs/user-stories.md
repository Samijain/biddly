# User stories

Format: *As a [role], I want [goal] so that [benefit].* Each story lists acceptance criteria (AC) and the milestone that delivers it.

## Browsing

**US-01 Browse live auctions** (M0 ✅)
As a visitor, I want to see all active auctions so that I can find something to bid on.
- AC: Shows image, title, category, current price, minimum increment and a live countdown.
- AC: Sorted by "ending soon" by default.
- AC: Shows skeleton cards while loading and a friendly message if the server is unreachable.

**US-02 Search and filter** (M3, Challenge 3)
As a visitor, I want to search by keyword, filter by category and change the sort order so that I quickly find relevant items.

**US-03 View auction details** (M0 ✅)
As a visitor, I want to see the full description, the price rules and the closing time so that I can decide whether to bid.

## Selling

**US-10 Create a draft auction** (M4, Challenge 2)
As a seller, I want to create an auction with a title, description, image, starting price, minimum increment and time window so that I can sell an item.
- AC: Saved as DRAFT; invalid input shows a message next to the right field.

**US-11 Publish an auction** (M4)
As a seller, I want to publish my draft so that bidders can see it.

**US-12 Cancel an auction** (M4)
As a seller, I want to cancel my auction before anyone has bid.

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

## Accounts

**US-30 Register / sign in / sign out** (M5)
**US-31 Protected pages redirect to sign-in** (M5)

## Administration

**US-40 Suspend a user** (post-MVP)
**US-41 View audit history** (post-MVP)
