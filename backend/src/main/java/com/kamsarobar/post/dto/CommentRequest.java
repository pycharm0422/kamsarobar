package com.kamsarobar.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentRequest(@NotBlank(message = "Comment cannot be empty") @Size(max = 2000) String content) {
}
