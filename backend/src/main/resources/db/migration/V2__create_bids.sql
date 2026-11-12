-- Milestone 6: bidding.
-- Bids are insert-only (R6): there is no UPDATE or DELETE path in the application.
-- Only accepted bids are stored; a rejected bid is answered with an error and leaves no row.

CREATE TABLE bids (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    auction_id BIGINT         NOT NULL,
    bidder_id  BIGINT         NOT NULL,
    amount     NUMERIC(12, 2) NOT NULL,
    placed_at  TIMESTAMPTZ    NOT NULL,
    CONSTRAINT fk_bids_auction FOREIGN KEY (auction_id) REFERENCES auctions (id),
    CONSTRAINT fk_bids_bidder  FOREIGN KEY (bidder_id)  REFERENCES users (id),
    CONSTRAINT ck_bids_amount_positive CHECK (amount > 0)
);

-- "Bid history for one auction, newest first" and "highest bid for one auction".
CREATE INDEX idx_bids_auction_placed_at ON bids (auction_id, placed_at DESC);
-- "My bids" (US-24).
CREATE INDEX idx_bids_bidder ON bids (bidder_id);

-- The current price already lives on the auction row. The leading bidder joins it, so lists and
-- details never need to aggregate the bids table. NULL means "no bids yet".
ALTER TABLE auctions
    ADD COLUMN leading_bidder_id BIGINT,
    ADD CONSTRAINT fk_auctions_leading_bidder FOREIGN KEY (leading_bidder_id) REFERENCES users (id);
