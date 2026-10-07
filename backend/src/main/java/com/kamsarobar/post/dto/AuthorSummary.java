package com.kamsarobar.post.dto;

import com.kamsarobar.user.User;

public record AuthorSummary(Long id, String name) {

    public static AuthorSummary from(User user) {
        return user == null ? null : new AuthorSummary(user.getId(), user.getName());
    }
}
