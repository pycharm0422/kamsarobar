package com.kamsarobar.post.dto;

import java.time.LocalDateTime;

import com.kamsarobar.city.dto.CitySummary;
import com.kamsarobar.post.Post;
import com.kamsarobar.post.PostCategory;

public record PostResponse(Long id, PostCategory category, String title, String content, AuthorSummary author,
                           CitySummary city, long commentCount, boolean canEdit, LocalDateTime createdAt,
                           LocalDateTime updatedAt) {

    public static PostResponse from(Post post, long commentCount, boolean canEdit) {
        return new PostResponse(post.getId(), post.getCategory(), post.getTitle(), post.getContent(),
                AuthorSummary.from(post.getAuthor()), CitySummary.from(post.getCity()), commentCount, canEdit,
                post.getCreatedAt(), post.getUpdatedAt());
    }
}
