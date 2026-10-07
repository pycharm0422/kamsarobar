-- Posts can now be "anything": a photo with no text, text with no title, or an event / seminar.
ALTER TABLE posts ALTER COLUMN title DROP NOT NULL;
ALTER TABLE posts ALTER COLUMN content DROP NOT NULL;

-- Event / seminar details (only set for EVENT and SEMINAR posts). Stored in UTC.
ALTER TABLE posts ADD COLUMN event_starts_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE posts ADD COLUMN event_ends_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE posts ADD COLUMN event_location VARCHAR(300);
ALTER TABLE posts ADD COLUMN event_link VARCHAR(500);
CREATE INDEX idx_posts_event_starts ON posts (event_starts_at);

-- Uploaded photos. A photo is uploaded first (post_id NULL) and attached when the post is saved;
-- photos that never get attached (or whose post is deleted) are cleaned up by a scheduled job.
CREATE TABLE images (
    id           VARCHAR(36)  PRIMARY KEY,
    uploader_id  BIGINT REFERENCES users (id) ON DELETE SET NULL,
    post_id      BIGINT REFERENCES posts (id) ON DELETE SET NULL,
    position     INT          NOT NULL DEFAULT 0,
    storage_key  VARCHAR(200) NOT NULL,
    content_type VARCHAR(50)  NOT NULL,
    size_bytes   BIGINT       NOT NULL,
    width        INT,
    height       INT,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_images_post ON images (post_id, position);
CREATE INDEX idx_images_orphans ON images (post_id, created_at);

-- Members who added an event / seminar to their "My upcoming events" list.
CREATE TABLE event_attendees (
    post_id    BIGINT    NOT NULL REFERENCES posts (id) ON DELETE CASCADE,
    user_id    BIGINT    NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (post_id, user_id)
);
CREATE INDEX idx_event_attendees_user ON event_attendees (user_id);
