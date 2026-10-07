package com.kamsarobar.post.dto;

import java.time.Instant;
import java.util.List;

import com.kamsarobar.post.PostCategory;
import com.kamsarobar.post.PostVisibility;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * A post can be anything: text, photos, or both. Title is optional except for events / seminars,
 * which also need a start time. Photos are uploaded first via POST /api/images and referenced by id.
 */
public record PostRequest(
        PostCategory category,
        /** CITY (default) = only members living in the author's city; EVERYONE = all cities. */
        PostVisibility visibility,
        @Size(max = 200) String title,
        @Size(max = 5000) String content,
        @Size(max = 6, message = "You can add at most 6 photos") List<String> imageIds,
        Instant eventStartsAt,
        Instant eventEndsAt,
        @Size(max = 300) String eventLocation,
        @Size(max = 500) @Pattern(regexp = "^$|^https?://.+$", message = "Online link must start with http:// or https://")
        String eventLink) {
}
