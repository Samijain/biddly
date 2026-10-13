# PostgreSQL primer for Biddly

You said PostgreSQL is new to you, so this page covers what you need for this project, in the order you'll need it.

## 1. Connect to the database

PostgreSQL runs in Docker (`docker compose up -d`). The `psql` command-line client ships inside the container:

```powershell
docker exec -it biddly-postgres psql -U biddly -d biddly
```

Useful `psql` meta-commands (they start with a backslash):

| Command | Does |
|---|---|
| `\dt` | List tables |
| `\d auctions` | Describe a table: columns, indexes, constraints |
| `\di` | List indexes |
| `\x` | Toggle expanded output (one column per line), good for wide rows |
| `\q` | Quit |

Prefer a GUI? `docker compose --profile tools up -d` and open <http://localhost:5050> (pgAdmin), or connect IntelliJ/DBeaver to `localhost:5432`, database `biddly`, user `biddly`, password `biddly`.

## 2. Try these queries

```sql
-- What did Flyway do?
SELECT installed_rank, version, description, success FROM flyway_schema_history;

-- Active auctions ending soonest, with seller name (a JOIN)
SELECT a.id, a.title, a.current_price, a.end_time, u.display_name AS seller
FROM auctions a
JOIN users u ON u.id = a.seller_id
WHERE a.status = 'ACTIVE'
ORDER BY a.end_time
LIMIT 5;

-- How long until each auction ends? (interval arithmetic)
SELECT title, end_time - now() AS time_left FROM auctions ORDER BY time_left;

-- Try to break a rule: the CHECK constraint rejects it
UPDATE auctions SET current_price = 1 WHERE id = 1;
-- ERROR: new row for relation "auctions" violates check constraint "ck_auctions_current_gte_starting"

-- Does the query use our index?
EXPLAIN ANALYZE SELECT * FROM auctions WHERE status = 'ACTIVE' ORDER BY end_time;
```

> With only 6 rows, PostgreSQL may pick a sequential scan anyway, because reading 6 rows is cheaper than using an index. That's correct behaviour, not a bug.

## 3. Concepts you'll meet in this project

| Concept | One-line explanation | Where in Biddly |
|---|---|---|
| **Primary key / identity** | Unique row id, auto-generated | every `id` column |
| **Foreign key** | A column that must match a row in another table | `auctions.seller_id → users.id` |
| **CHECK constraint** | A rule the database enforces on every write | positive prices, `end > start` |
| **Index** | A sorted lookup structure that makes `WHERE`/`ORDER BY` fast and writes a bit slower | `(status, end_time)` |
| **NUMERIC** | Exact decimal, required for money | all prices |
| **TIMESTAMPTZ** | A moment in time, stored as UTC | all times |
| **Transaction** | A group of statements that all succeed or all fail (`BEGIN ... COMMIT`) | placing a bid (M6) |
| **Isolation level** | How much concurrent transactions see of each other. The default is READ COMMITTED | M6 |
| **Row lock** `SELECT ... FOR UPDATE` | Makes other transactions wait for this row | pessimistic bid locking (M6) |
| **Optimistic locking** | A `version` column; an update fails if someone changed the row first | `auctions.version` (M6) |
| **JSONB** | A binary, indexable JSON column | `audit_events.metadata` (M8) |

## 4. A concurrency experiment (preview of Milestone 6)

Open **two** `psql` sessions side by side:

```sql
-- Session A
BEGIN;
SELECT current_price FROM auctions WHERE id = 1 FOR UPDATE;   -- A locks the row

-- Session B
BEGIN;
SELECT current_price FROM auctions WHERE id = 1 FOR UPDATE;   -- B WAITS here...

-- Session A
UPDATE auctions SET current_price = current_price + 1000 WHERE id = 1;
COMMIT;                                                        -- ...B now continues and sees the NEW price

-- Session B
ROLLBACK;
```

You just saw the core of a correct bidding engine.

## 5. Reset everything

```powershell
docker compose down -v   # deletes the volume = all data
docker compose up -d     # fresh, empty DB; Flyway recreates tables on the next backend start
```

## Further reading

- Official tutorial: <https://www.postgresql.org/docs/current/tutorial.html>
- Explicit locking: <https://www.postgresql.org/docs/current/explicit-locking.html>
- Flyway concepts: <https://documentation.red-gate.com/fd/migrations-184127470.html>
