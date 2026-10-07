package com.kamsarobar.event;

import java.time.Duration;
import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kamsarobar.common.exception.BadRequestException;
import com.kamsarobar.common.exception.ResourceNotFoundException;
import com.kamsarobar.common.web.PageResponse;
import com.kamsarobar.post.EventAttendee;
import com.kamsarobar.post.EventAttendeeRepository;
import com.kamsarobar.post.Post;
import com.kamsarobar.post.PostRepository;
import com.kamsarobar.post.PostResponseAssembler;
import com.kamsarobar.post.dto.PostResponse;
import com.kamsarobar.security.UserPrincipal;

/** Events and seminars are posts with a date; members add them to their "My upcoming events" list. */
@Service
@Transactional(readOnly = true)
public class EventService {

    /** An event without an end time stays "upcoming" for this long after it starts. */
    private static final Duration DEFAULT_DURATION = Duration.ofHours(6);

    private final PostRepository postRepository;
    private final EventAttendeeRepository attendeeRepository;
    private final PostResponseAssembler assembler;

    public EventService(PostRepository postRepository, EventAttendeeRepository attendeeRepository,
                        PostResponseAssembler assembler) {
        this.postRepository = postRepository;
        this.attendeeRepository = attendeeRepository;
        this.assembler = assembler;
    }

    public PageResponse<PostResponse> myUpcoming(UserPrincipal viewer, Pageable pageable) {
        Instant now = Instant.now();
        return assembler.toPage(postRepository.findUpcomingAttendedBy(viewer.id(), now,
                now.minus(DEFAULT_DURATION), pageable), viewer);
    }

    public PageResponse<PostResponse> upcoming(Long cityId, UserPrincipal viewer, Pageable pageable) {
        Instant now = Instant.now();
        Instant startedAfter = now.minus(DEFAULT_DURATION);
        Page<Post> page = cityId == null
                ? postRepository.findUpcoming(now, startedAfter, pageable)
                : postRepository.findUpcomingInCity(cityId, now, startedAfter, pageable);
        return assembler.toPage(page, viewer);
    }

    @Transactional
    public PostResponse attend(Long postId, UserPrincipal viewer) {
        Post post = getEvent(postId);
        if (post.hasEnded(Instant.now())) {
            throw new BadRequestException("This event has already ended");
        }
        if (!attendeeRepository.existsById(new EventAttendee.Key(postId, viewer.id()))) {
            attendeeRepository.save(new EventAttendee(postId, viewer.id()));
            attendeeRepository.flush();
        }
        return assembler.toResponse(post, viewer);
    }

    @Transactional
    public PostResponse leave(Long postId, UserPrincipal viewer) {
        Post post = getEvent(postId);
        attendeeRepository.deleteById(new EventAttendee.Key(postId, viewer.id()));
        attendeeRepository.flush();
        return assembler.toResponse(post, viewer);
    }

    private Post getEvent(Long postId) {
        Post post = postRepository.findWithAuthorById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", postId));
        if (!post.isEvent()) {
            throw new BadRequestException("This post is not an event");
        }
        return post;
    }
}
