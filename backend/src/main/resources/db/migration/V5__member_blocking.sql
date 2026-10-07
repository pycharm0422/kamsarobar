-- Admins can block members (spam, fake accounts, abuse). Blocking is fully reversible: nothing is deleted.
ALTER TABLE users ADD COLUMN blocked BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE users ADD COLUMN blocked_reason VARCHAR(300);
ALTER TABLE users ADD COLUMN blocked_at TIMESTAMP;
ALTER TABLE users ADD COLUMN blocked_by_id BIGINT REFERENCES users (id) ON DELETE SET NULL;
