-- Flyway runs this file exactly once per database, in version order.
-- Never edit a migration after it has been applied; add a new V2__..., V3__... file instead.

CREATE TABLE users (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    display_name  VARCHAR(100) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255),
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'SUSPENDED'))
);

-- Expression index: makes "Asha@Mail.com" and "asha@mail.com" the same address.
CREATE UNIQUE INDEX uq_users_email_lower ON users (lower(email));

CREATE TABLE categories (
    id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(60) NOT NULL UNIQUE,
    slug VARCHAR(60) NOT NULL UNIQUE
);

CREATE TABLE auctions (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    seller_id         BIGINT        NOT NULL REFERENCES users (id),
    category_id       BIGINT        REFERENCES categories (id),
    title             VARCHAR(120)  NOT NULL,
    description       TEXT,
    cover_image_url   VARCHAR(500),
    starting_price    NUMERIC(12, 2) NOT NULL,
    current_price     NUMERIC(12, 2) NOT NULL,
    minimum_increment NUMERIC(12, 2) NOT NULL,
    start_time        TIMESTAMPTZ   NOT NULL,
    end_time          TIMESTAMPTZ   NOT NULL,
    status            VARCHAR(20)   NOT NULL,
    version           BIGINT        NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_auctions_starting_price_positive CHECK (starting_price > 0),
    CONSTRAINT ck_auctions_increment_positive      CHECK (minimum_increment > 0),
    CONSTRAINT ck_auctions_current_gte_starting    CHECK (current_price >= starting_price),
    CONSTRAINT ck_auctions_time_window             CHECK (end_time > start_time),
    CONSTRAINT ck_auctions_status CHECK (status IN ('DRAFT', 'SCHEDULED', 'ACTIVE', 'COMPLETED', 'CANCELLED'))
);

CREATE INDEX idx_auctions_status_end_time   ON auctions (status, end_time);
CREATE INDEX idx_auctions_category_status   ON auctions (category_id, status);
CREATE INDEX idx_auctions_seller            ON auctions (seller_id);

INSERT INTO categories (name, slug) VALUES
    ('Electronics',  'electronics'),
    ('Collectibles', 'collectibles'),
    ('Fashion',      'fashion'),
    ('Home & Living','home-living'),
    ('Art',          'art'),
    ('Sports',       'sports');
