-- Challenge 11a: every account has a role. Sign-in, permissions and seller approval build on it (11b-11e).
-- Product decision 8: USER can bid (and sell once approved); ADMIN also approves sellers.
ALTER TABLE users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';
ALTER TABLE users ADD CONSTRAINT ck_users_role CHECK (role IN ('USER', 'ADMIN'));
