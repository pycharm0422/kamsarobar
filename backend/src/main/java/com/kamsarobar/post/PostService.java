package com.kamsarobar.post;

import java.time.Instant;

import org.springframework.data.domain.Page;
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

    public PostService(PostRepository postRepository, UserService userService, ImageService imageService,
                       AccessPolicy accessPolicy, PostResponseAssembler assembler) {
        this.postRepository = postRepository;
        this.userService = userService;
        this.imageService = imageService;
        this.accessPolicy = accessPolicy;
        this.assembler = assembler;
    }

    /** City feed (or all cities when cityId is null), newest first, optionally filtered by category. */
    public PageResponse<PostResponse> feed(Long cityId, PostCategory category, Pageable pageable,
                                           UserPrincipal viewer) {
        Page<Post> page;
        if (cityId != null) {
            page = category == null
                    ? postRepository.findByCityIdOrderByCreatedAtDesc(cityId, pageable)
                    : postRepository.findByCityIdAndCategoryOrderByCreatedAtDesc(cityId, category, pageable);
        } else {
            page = category == null
                    ? postRepository.findAllByOrderByCreatedAtDesc(pageable)
                    : postRepository.findByCategoryOrderByCreatedAtDesc(category, pageable);
        }
        return assembler.toPage(page, viewer);
    }

    public PostResponse get(Long id, UserPrincipal viewer) {
        return assembler.toResponse(getEntity(id), viewer);
    }

    /** Members post into their own city's community. */
    @Transactional
    public PostResponse create(PostRequest request, UserPrincipal actor) {
        User author = userService.getEntity(actor.id());
        PostCategory category = categoryOrDefault(request);
        validate(request, category, true);
        Post post = new Post(author, author.getCity(), category, TextNormalizer.clean(request.title()),
                trimToNull(request.content()));
        applyEvent(post, request, category);
        postRepository.save(post);
        imageService.setPostImages(post, request.imageIds(), actor.id());
        return assembler.toResponse(post, actor);
    }

    @Transactional
    public PostResponse update(Long id, PostRequest request, UserPrincipal actor) {
        Post post = getEntity(id);
        accessPolicy.requireContentModifier(actor, post.getAuthor().getId(), post.getCity().getId());
        PostCategory category = categoryOrDefault(request);
        validate(request, category, false);
        post.edit(category, TextNormalizer.clean(request.title()), trimToNull(request.content()));
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
