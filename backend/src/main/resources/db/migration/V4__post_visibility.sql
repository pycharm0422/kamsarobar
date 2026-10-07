-- Who can see a post: CITY = only members living in the post's city, EVERYONE = members of every city.
-- Posts created before this option existed were visible to all, so they keep that.
ALTER TABLE posts ADD COLUMN visibility VARCHAR(20) DEFAULT 'EVERYONE' NOT NULL;
CREATE INDEX idx_posts_visibility_created ON posts (visibility, created_at);
