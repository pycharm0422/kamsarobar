package com.kamsarobar.post;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kamsarobar.access.AccessPolicy;
import com.kamsarobar.common.exception.ResourceNotFoundException;
import com.kamsarobar.common.web.PageResponse;
import com.kamsarobar.post.dto.PostRequest;
import com.kamsarobar.post.dto.PostResponse;
import com.kamsarobar.security.UserPrincipal;
import com.kamsarobar.user.User;
import com.kamsarobar.user.UserService;

@Service
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserService userService;
    private final AccessPolicy accessPolicy;

    public PostService(PostRepository postRepository, CommentRepository commentRepository, UserService userService,
                       AccessPolicy accessPolicy) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.userService = userService;
        this.accessPolicy = accessPolicy;
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
        Map<Long, Long> counts = commentCounts(page.getContent());
        return PageResponse.of(page, post -> toResponse(post, counts.getOrDefault(post.getId(), 0L), viewer));
    }

    public PostResponse get(Long id, UserPrincipal viewer) {
        Post post = getEntity(id);
        return toResponse(post, commentRepository.countByPostId(id), viewer);
    }

    /** Members post into their own city's community. */
    @Transactional
    public PostResponse create(PostRequest request, UserPrincipal actor) {
        User author = userService.getEntity(actor.id());
        Post post = postRepository.save(new Post(author, author.getCity(), categoryOrDefault(request),
                request.title().trim(), request.content().trim()));
        return toResponse(post, 0, actor);
    }

    @Transactional
    public PostResponse update(Long id, PostRequest request, UserPrincipal actor) {
        Post post = getEntity(id);
        accessPolicy.requireContentModifier(actor, post.getAuthor().getId(), post.getCity().getId());
        post.edit(categoryOrDefault(request), request.title().trim(), request.content().trim());
        postRepository.flush();
        return toResponse(post, commentRepository.countByPostId(id), actor);
    }

    @Transactional
    public void delete(Long id, UserPrincipal actor) {
        Post post = getEntity(id);
        accessPolicy.requireContentModifier(actor, post.getAuthor().getId(), post.getCity().getId());
        postRepository.delete(post);
    }

    Post getEntity(Long id) {
        return postRepository.findWithAuthorById(id).orElseThrow(() -> new ResourceNotFoundException("Post", id));
    }

    private PostResponse toResponse(Post post, long commentCount, UserPrincipal viewer) {
        boolean canEdit = accessPolicy.canModifyContent(viewer, post.getAuthor().getId(), post.getCity().getId());
        return PostResponse.from(post, commentCount, canEdit);
    }

    private Map<Long, Long> commentCounts(List<Post> posts) {
        Map<Long, Long> counts = new HashMap<>();
        if (posts.isEmpty()) {
            return counts;
        }
        for (Object[] row : commentRepository.countByPostIds(posts.stream().map(Post::getId).toList())) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return counts;
    }

    private static PostCategory categoryOrDefault(PostRequest request) {
        return request.category() == null ? PostCategory.GENERAL : request.category();
    }
}
