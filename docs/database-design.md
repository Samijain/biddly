# Database design

PostgreSQL is the **source of truth**. Schema changes are made only through Flyway migrations in `backend/src/main/resources/db/migration`.

## Current schema (V1 + V2)

```mermaid
erDiagram
    USERS ||--o{ AUCTIONS : sells
    CATEGORIES ||--o{ AUCTIONS : groups
    AUCTIONS ||--o{ BIDS : receives
    USERS ||--o{ BIDS : places
    USERS |o--o{ AUCTIONS : leads

    USERS {
        bigint id PK
        varchar display_name
        varchar email "unique on lower(email)"
        varchar password_hash "nullable until M5"
        varchar status "ACTIVE | SUSPENDED"
        timestamptz created_at
        timestamptz updated_at
    }
    CATEGORIES {
        bigint id PK
        varchar name UK
        varchar slug UK
    }
    AUCTIONS {
        bigint id PK
        bigint seller_id FK
        bigint category_id FK "nullable"
        varchar title
        text description
        varchar cover_image_url
        numeric starting_price "12,2 > 0"
        numeric current_price "12,2 >= starting"
        numeric minimum_increment "12,2 > 0"
        timestamptz start_time
        timestamptz end_time "> start_time"
        varchar status
        bigint version "optimistic lock"
        bigint leading_bidder_id FK "nullable, V2"
        timestamptz created_at
        timestamptz updated_at
    }
    BIDS {
        bigint id PK
        bigint auction_id FK
        bigint bidder_id FK
        numeric amount "12,2 > 0"
        timestamptz placed_at "server clock"
    }
```

### Constraints and indexes

| Name | Purpose |
|---|---|
| `uq_users_email_lower` | One account per email, case-insensitive |
| `ck_auctions_*` | Positive money, `current ≥ starting`, `end > start`, valid status |
| `idx_auctions_status_end_time` | "Active auctions ordered by end time", the home page query and the auto-close job |
| `idx_auctions_category_status` | Category filter |
| `idx_auctions_seller` | "My auctions" and the foreign-key lookup |
| `ck_bids_amount_positive` | A bid is always more than zero |
| `idx_bids_auction_placed_at` | Bid history of one auction, newest first |
| `idx_bids_bidder` | "My bids" (US-24) and the foreign-key lookup |
| `fk_auctions_leading_bidder` | The leader is a real user; `NULL` until the first bid |

`auctions.current_price` and `leading_bidder_id` are a **denormalised copy** of the highest bid, so lists don't need to scan `bids`. They must change in the same transaction as the bid insert.

## Planned tables

| Table | Milestone | Notes |
|---|---|---|
| `roles`, `user_roles` | M5 | BIDDER, SELLER, ADMIN |
| `refresh_tokens` | M5 | Store a hash, never the raw token |
| `idempotency_keys` | M6 | Or Redis with a TTL |
| `watchlist` | M7 | Composite PK `(user_id, auction_id)` |
| `notifications` | M7 | `read_at` nullable |
| `audit_events` | M8 | `metadata JSONB` |
| `auction_images` | Later | Replaces `cover_image_url` when upload exists |

## Conventions

- Table names are plural and snake_case; columns are snake_case (Hibernate maps `startTime` → `start_time` automatically).
- Every table has `id BIGINT GENERATED ALWAYS AS IDENTITY`.
- Money is `NUMERIC(12,2)` (up to ₹9,99,99,99,999.99).
- Time is `TIMESTAMPTZ`. PostgreSQL stores UTC internally.
- Name every constraint (`ck_`, `uq_`, `fk_`, `idx_`) so error messages and migrations are readable.
- Migration files are **immutable** once applied. Fixes go in a new `V{n+1}__...sql`.
