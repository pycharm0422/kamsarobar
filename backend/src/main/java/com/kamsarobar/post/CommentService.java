package com.kamsarobar.post;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kamsarobar.access.AccessPolicy;
import com.kamsarobar.common.exception.ResourceNotFoundException;
import com.kamsarobar.common.web.PageResponse;
import com.kamsarobar.post.dto.CommentRequest;
import com.kamsarobar.post.dto.CommentResponse;
import com.kamsarobar.security.UserPrincipal;
import com.kamsarobar.user.UserService;

@Service
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostService postService;
    private final UserService userService;
    private final AccessPolicy accessPolicy;

    public CommentService(CommentRepository commentRepository, PostService postService,
                          UserService userService, AccessPolicy accessPolicy) {
        this.commentRepository = commentRepository;
        this.postService = postService;
        this.userService = userService;
        this.accessPolicy = accessPolicy;
    }

    public PageResponse<CommentResponse> list(Long postId, Pageable pageable, UserPrincipal viewer) {
        Post post = postService.getEntity(postId);
        Long cityId = post.getCity().getId();
        return PageResponse.of(commentRepository.findByPostIdOrderByCreatedAtAsc(postId, pageable),
                c -> CommentResponse.from(c, accessPolicy.canModifyContent(viewer, c.getAuthor().getId(), cityId)));
    }

    @Transactional
    public CommentResponse add(Long postId, CommentRequest request, UserPrincipal actor) {
        Post post = postService.getEntity(postId);
        Comment comment = commentRepository.save(
                new Comment(post, userService.getEntity(actor.id()), request.content().trim()));
        return CommentResponse.from(comment, true);
    }

    @Transactional
    public CommentResponse update(Long id, CommentRequest request, UserPrincipal actor) {
        Comment comment = getEntity(id);
        accessPolicy.requireContentModifier(actor, comment.getAuthor().getId(), comment.getPost().getCity().getId());
        comment.setContent(request.content().trim());
        commentRepository.flush();
        return CommentResponse.from(comment, true);
    }

    @Transactional
    public void delete(Long id, UserPrincipal actor) {
        Comment comment = getEntity(id);
        accessPolicy.requireContentModifier(actor, comment.getAuthor().getId(), comment.getPost().getCity().getId());
        commentRepository.delete(comment);
    }

    private Comment getEntity(Long id) {
        return commentRepository.findWithPostById(id).orElseThrow(() -> new ResourceNotFoundException("Comment", id));
    }
}
