package com.kamsarobar.post;

import java.time.Instant;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.kamsarobar.user.MemberMovedEvent;

/**
 * When a member moves to another city, their "only my city" posts move with them, so the members of their new
 * city see them. Posts shared with everyone are visible everywhere already and stay as they are. Seminars and
 * events that haven't finished stay in the old city: they take place there, and the people who added them keep
 * seeing them.
 */
@Component
public class MemberPostsMover {

    private final PostRepository postRepository;

    public MemberPostsMover(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @EventListener
    public void onMemberMoved(MemberMovedEvent event) {
        Instant now = Instant.now();
        postRepository.moveCityOnlyPosts(event.userId(), event.fromCityId(), event.toCityId(), now,
                now.minus(Post.DEFAULT_EVENT_DURATION));
    }
}
