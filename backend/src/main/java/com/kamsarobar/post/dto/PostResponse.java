package com.kamsarobar.post.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.kamsarobar.city.dto.CitySummary;
import com.kamsarobar.media.dto.ImageResponse;
import com.kamsarobar.post.Post;
import com.kamsarobar.post.PostCategory;
import com.kamsarobar.post.PostVisibility;

public record PostResponse(Long id, PostCategory category, PostVisibility visibility, String title, String content, AuthorSummary author,
                           CitySummary city, List<ImageResponse> images, EventInfo event, long commentCount,
                           boolean canEdit, LocalDateTime createdAt, LocalDateTime updatedAt) {

    public static PostResponse from(Post post, long commentCount, boolean canEdit, EventInfo event) {
        return new PostResponse(post.getId(), post.getCategory(), post.getVisibility(), post.getTitle(), post.getContent(),
                AuthorSummary.from(post.getAuthor()), CitySummary.from(post.getCity()),
                post.getImages().stream().map(ImageResponse::from).toList(), event, commentCount, canEdit,
                post.getCreatedAt(), post.getUpdatedAt());
    }
}
