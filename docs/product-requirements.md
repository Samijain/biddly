# Product requirements

## Vision

Biddly is a real-time auction marketplace. Sellers create timed auctions; registered bidders compete until the auction closes. Every accepted bid is recorded permanently, and each auction ends with at most one winner.

## The core engineering problem

Many users may bid on the same item at almost the same moment. Biddly must accept only valid bids, keep them in the correct order, and declare exactly one winner. Everything else is built around this guarantee.

## Users

| Role | Can |
|---|---|
| **Visitor** | Browse and search auctions, view details and bid history |
| **Bidder** | Everything a visitor can do, plus place bids, watch auctions, get outbid/won notifications, and see their bids and wins |
| **Seller** | Create, edit (draft only), publish and cancel their own auctions; see the winner |
| **Administrator** | Manage users, review reported auctions, suspend accounts, view the audit history |

A user can be both a bidder and a seller, but never bids on their own auction.

## MVP scope

1. Registration and sign-in
2. Bidder and seller roles
3. Create and edit a draft auction
4. Publish an auction
5. List, search, sort and filter active auctions
6. View auction details
7. Place a valid bid; reject an invalid bid with a clear reason
8. Close expired auctions automatically and identify the winner
9. Bid history
10. Docker Compose setup, automated tests, GitHub Actions CI

## Out of scope for the MVP

Real payments (they will be simulated), microservices, Kafka, autoscaling, recommendations, and native mobile apps.

## Non-functional requirements

| Area | Target |
|---|---|
| Correctness | Zero inconsistent auctions under concurrent bidding (proven by a concurrency test) |
| Latency | Bid accepted or rejected in under 300 ms (p95) locally; price updates reach other viewers in under 1 s once WebSocket is in place |
| Security | OWASP basics: hashed passwords, HttpOnly refresh cookie, role checks, input validation, rate limiting, no secrets in Git |
| Usability | Mobile-first responsive UI, light and dark themes, keyboard accessible, clear error messages |
| Operability | Health probes, structured logs, one-command local start |
| Time | All times stored in UTC; shown in the viewer's local time |
