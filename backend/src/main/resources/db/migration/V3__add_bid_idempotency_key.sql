-- Milestone 6: idempotent bids (Challenge 10).
-- The client sends a fresh UUID with every "Place bid" attempt and repeats it when it retries.
-- The key is stored with the accepted bid, so a repeated request can be answered with the original bid.
-- Rejected bids are never stored (R20), so a retried rejection is simply judged again.

ALTER TABLE bids ADD COLUMN idempotency_key UUID;

-- One key per bidder. A key from one bidder never clashes with, or reveals, another bidder's bid.
-- Bids placed without a key (older clients, curl) have NULL and are not constrained.
CREATE UNIQUE INDEX uq_bids_bidder_idempotency_key ON bids (bidder_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;
