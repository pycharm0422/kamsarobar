package com.kamsarobar.post;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.kamsarobar.access.AccessPolicy;
import com.kamsarobar.common.web.PageResponse;
import com.kamsarobar.post.dto.EventInfo;
import com.kamsarobar.post.dto.PostResponse;
import com.kamsarobar.security.UserPrincipal;

/**
 * Builds post responses for a whole page at once: comment counts, attendee counts and the viewer's
 * "attending" flags each come from a single grouped query instead of one query per post.
 */
@Component
public class PostResponseAssembler {

    private final CommentRepository commentRepository;
    private final EventAttendeeRepository attendeeRepository;
    private final AccessPolicy accessPolicy;

    public PostResponseAssembler(CommentRepository commentRepository, EventAttendeeRepository attendeeRepository,
                                 AccessPolicy accessPolicy) {
        this.commentRepository = commentRepository;
        this.attendeeRepository = attendeeRepository;
        this.accessPolicy = accessPolicy;
    }

    public PageResponse<PostResponse> toPage(Page<Post> page, UserPrincipal viewer) {
        return PageResponse.of(page, toResponses(page.getContent(), viewer));
    }

    public PostResponse toResponse(Post post, UserPrincipal viewer) {
        return toResponses(List.of(post), viewer).get(0);
    }

    public List<PostResponse> toResponses(List<Post> posts, UserPrincipal viewer) {
        if (posts.isEmpty()) {
            return List.of();
        }
        List<Long> ids = posts.stream().map(Post::getId).toList();
        Map<Long, Long> comments = counts(commentRepository.countByPostIds(ids));

        List<Long> eventIds = posts.stream().filter(Post::isEvent).map(Post::getId).toList();
        Map<Long, Long> attendees = eventIds.isEmpty() ? Map.of() : counts(attendeeRepository.countByPostIds(eventIds));
        Set<Long> attending = eventIds.isEmpty() || viewer == null ? Set.of()
                : new HashSet<>(attendeeRepository.findAttendingPostIds(viewer.id(), eventIds));

        return posts.stream().map(post -> PostResponse.from(post,
                comments.getOrDefault(post.getId(), 0L),
                accessPolicy.canModifyContent(viewer, post.getAuthor().getId(), post.getCity().getId()),
                EventInfo.of(post, attendees.getOrDefault(post.getId(), 0L), attending.contains(post.getId()))))
                .toList();
    }

    private static Map<Long, Long> counts(List<Object[]> rows) {
        Map<Long, Long> map = new HashMap<>();
        rows.forEach(row -> map.put((Long) row[0], ((Number) row[1]).longValue()));
        return map;
    }
}
