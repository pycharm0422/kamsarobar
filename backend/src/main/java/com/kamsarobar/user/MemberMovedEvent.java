package com.kamsarobar.user;

/**
 * Published inside the transaction when a member's home city changes, whether they changed it themselves or the
 * main admin moved them. Listeners run in the same transaction, so the move and its consequences commit together.
 */
public record MemberMovedEvent(Long userId, Long fromCityId, Long toCityId) {
}
