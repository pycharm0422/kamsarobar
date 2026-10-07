package com.kamsarobar.post.dto;

import java.time.Instant;

import com.kamsarobar.post.Post;

public record EventInfo(Instant startsAt, Instant endsAt, String location, String link, long attendeeCount,
                        boolean attending, boolean ended) {

    public static EventInfo of(Post post, long attendeeCount, boolean attending) {
        if (!post.isEvent()) {
            return null;
        }
        return new EventInfo(post.getEventStartsAt(), post.getEventEndsAt(), post.getEventLocation(),
                post.getEventLink(), attendeeCount, attending, post.hasEnded(Instant.now()));
    }
}
