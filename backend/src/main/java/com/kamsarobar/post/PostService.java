package com.kamsarobar.post;

import java.time.Instant;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kamsarobar.access.AccessPolicy;
import com.kamsarobar.common.exception.BadRequestException;
import com.kamsarobar.common.exception.ResourceNotFoundException;
import com.kamsarobar.common.util.TextNormalizer;
import com.kamsarobar.common.web.PageResponse;
import com.kamsarobar.media.Image;
import com.kamsarobar.media.ImageService;
import com.kamsarobar.post.dto.PostRequest;
import com.kamsarobar.post.dto.PostResponse;
import com.kamsarobar.security.UserPrincipal;
import com.kamsarobar.user.User;
import com.kamsarobar.user.UserService;

@Service
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final UserService userService;
    private final ImageService imageService;
    private final AccessPolicy accessPolicy;
    private final PostResponseAssembler assembler;
    private final ApplicationEventPublisher events;

    public PostService(PostRepository postRepository, UserService userService, ImageService imageService,
                       AccessPolicy accessPolicy, PostResponseAssembler assembler, ApplicationEventPublisher events) {
        this.events = events;
        this.postRepository = postRepository;
        this.userService = userService;
        this.imageService = imageService;
        this.accessPolicy = accessPolicy;
        this.assembler = assembler;
    }

    /**
     * Feed of posts the viewer may see, newest first. cityId and category are optional filters; when browsing
     * another city, only that city's posts shared with everyone appear.
     */
    public PageResponse<PostResponse> feed(Long cityId, PostCategory category, Pageable pageable,
                                           UserPrincipal viewer) {
        return assembler.toPage(postRepository.findFeed(cityId, category, PostAudience.of(viewer), pageable), viewer);
    }

    public PostResponse get(Long id, UserPrincipal viewer) {
        return assembler.toResponse(getVisibleEntity(id, viewer), viewer);
    }

    /** Members post into their own city's community. */
    @Transactional
    public PostResponse create(PostRequest request, UserPrincipal actor) {
        User author = userService.getEntity(actor.id());
        PostCategory category = categoryOrDefault(request);
        validate(request, category, true);
        Post post = new Post(author, author.getCity(), category, TextNormalizer.clean(request.title()),
                trimToNull(request.content()));
        post.setVisibility(visibilityOrDefault(request));
        applyEvent(post, request, category);
        postRepository.save(post);
        imageService.setPostImages(post, request.imageIds(), actor.id());
        events.publishEvent(new PostCreatedEvent(post.getId()));
        return assembler.toResponse(post, actor);
    }

    @Transactional
    public PostResponse update(Long id, PostRequest request, UserPrincipal actor) {
        Post post = getEntity(id);
        accessPolicy.requireContentModifier(actor, post.getAuthor().getId(), post.getCity().getId());
        PostCategory category = categoryOrDefault(request);
        validate(request, category, false);
        post.edit(category, TextNormalizer.clean(request.title()), trimToNull(request.content()));
        post.setVisibility(visibilityOrDefault(request));
        applyEvent(post, request, category);
        imageService.setPostImages(post, request.imageIds(), actor.id());
        postRepository.flush();
        return assembler.toResponse(post, actor);
    }

    @Transactional
    public void delete(Long id, UserPrincipal actor) {
        Post post = getEntity(id);
        accessPolicy.requireContentModifier(actor, post.getAuthor().getId(), post.getCity().getId());
        post.getImages().forEach(Image::detach); // the photos are removed by the cleanup job
        postRepository.delete(post);
    }

    Post getEntity(Long id) {
        return postRepository.findWithAuthorById(id).orElseThrow(() -> new ResourceNotFoundException("Post", id));
    }

    /** A post the viewer may see; a hidden post looks exactly like a missing one, so nothing leaks. */
    public Post getVisibleEntity(Long id, UserPrincipal viewer) {
        Post post = getEntity(id);
        if (!PostAudience.of(viewer).canSee(post)) {
            throw new ResourceNotFoundException("Post", id);
        }
        return post;
    }

    private static void validate(PostRequest request, PostCategory category, boolean creating) {
        boolean hasText = trimToNull(request.content()) != null;
        boolean hasPhotos = request.imageIds() != null && !request.imageIds().isEmpty();
        if (!category.isEvent() && !hasText && !hasPhotos) {
            throw new BadRequestException("Write something or add a photo");
        }
        if (!category.isEvent()) {
            return;
        }
        if (TextNormalizer.clean(request.title()) == null) {
            throw new BadRequestException("Please give the event a title");
        }
        if (request.eventStartsAt() == null) {
            throw new BadRequestException("Please add the date and time of the event");
        }
        if (creating && request.eventStartsAt().isBefore(Instant.now())) {
            throw new BadRequestException("The event date must be in the future");
        }
        if (request.eventEndsAt() != null && !request.eventEndsAt().isAfter(request.eventStartsAt())) {
            throw new BadRequestException("The event must end after it starts");
        }
    }

    private static void applyEvent(Post post, PostRequest request, PostCategory category) {
        if (category.isEvent()) {
            post.setEvent(request.eventStartsAt(), request.eventEndsAt(),
                    TextNormalizer.clean(request.eventLocation()), TextNormalizer.clean(request.eventLink()));
        } else {
            post.setEvent(null, null, null, null);
        }
    }

    private static PostVisibility visibilityOrDefault(PostRequest request) {
        return request.visibility() == null ? PostVisibility.CITY : request.visibility();
    }

    private static PostCategory categoryOrDefault(PostRequest request) {
        return request.category() == null ? PostCategory.GENERAL : request.category();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
