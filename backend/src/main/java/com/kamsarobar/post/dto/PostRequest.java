package com.kamsarobar.post.dto;

import com.kamsarobar.post.PostCategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostRequest(
        PostCategory category,
        @NotBlank(message = "Title is required") @Size(max = 200) String title,
        @NotBlank(message = "Content is required") @Size(max = 5000) String content) {
}
