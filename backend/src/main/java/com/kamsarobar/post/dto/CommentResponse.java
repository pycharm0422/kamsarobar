package com.kamsarobar.post.dto;

import java.time.LocalDateTime;

import com.kamsarobar.post.Comment;

public record CommentResponse(Long id, Long postId, String content, AuthorSummary author, boolean canEdit,
                              LocalDateTime createdAt, LocalDateTime updatedAt) {

    public static CommentResponse from(Comment comment, boolean canEdit) {
        return new CommentResponse(comment.getId(), comment.getPost().getId(), comment.getContent(),
                AuthorSummary.from(comment.getAuthor()), canEdit, comment.getCreatedAt(), comment.getUpdatedAt());
    }
}
