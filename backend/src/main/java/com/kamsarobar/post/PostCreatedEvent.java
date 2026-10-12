package com.kamsarobar.post;

/** Published when a post is created; listeners (e.g. push notifications) run after the transaction commits. */
public record PostCreatedEvent(Long postId) {
}
