package com.kamsarobar.post;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kamsarobar.common.web.PageResponse;
import com.kamsarobar.common.web.Pages;
import com.kamsarobar.post.dto.CommentRequest;
import com.kamsarobar.post.dto.CommentResponse;
import com.kamsarobar.post.dto.PostRequest;
import com.kamsarobar.post.dto.PostResponse;
import com.kamsarobar.security.UserPrincipal;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class PostController {

    private final PostService postService;
    private final CommentService commentService;

    public PostController(PostService postService, CommentService commentService) {
        this.postService = postService;
        this.commentService = commentService;
    }

    @GetMapping("/posts")
    public PageResponse<PostResponse> feed(@RequestParam(required = false) Long cityId,
                                           @RequestParam(required = false) PostCategory category,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "10") int size,
                                           @AuthenticationPrincipal UserPrincipal viewer) {
        return postService.feed(cityId, category, Pages.of(page, size), viewer);
    }

    @GetMapping("/posts/{id}")
    public PostResponse get(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal viewer) {
        return postService.get(id, viewer);
    }

    @PostMapping("/posts")
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse create(@Valid @RequestBody PostRequest request, @AuthenticationPrincipal UserPrincipal actor) {
        return postService.create(request, actor);
    }

    @PutMapping("/posts/{id}")
    public PostResponse update(@PathVariable Long id, @Valid @RequestBody PostRequest request,
                               @AuthenticationPrincipal UserPrincipal actor) {
        return postService.update(id, request, actor);
    }

    @DeleteMapping("/posts/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal actor) {
        postService.delete(id, actor);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/posts/{postId}/comments")
    public PageResponse<CommentResponse> comments(@PathVariable Long postId,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "50") int size,
                                                  @AuthenticationPrincipal UserPrincipal viewer) {
        return commentService.list(postId, Pages.of(page, size), viewer);
    }

    @PostMapping("/posts/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse addComment(@PathVariable Long postId, @Valid @RequestBody CommentRequest request,
                                      @AuthenticationPrincipal UserPrincipal actor) {
        return commentService.add(postId, request, actor);
    }

    @PutMapping("/comments/{id}")
    public CommentResponse updateComment(@PathVariable Long id, @Valid @RequestBody CommentRequest request,
                                         @AuthenticationPrincipal UserPrincipal actor) {
        return commentService.update(id, request, actor);
    }

    @DeleteMapping("/comments/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal actor) {
        commentService.delete(id, actor);
        return ResponseEntity.noContent().build();
    }
}
