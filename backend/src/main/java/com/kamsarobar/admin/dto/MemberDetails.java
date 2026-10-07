package com.kamsarobar.admin.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.kamsarobar.profile.dto.ProfileResponse;
import com.kamsarobar.user.dto.UserResponse;

/** Everything an admin sees on a member's page. */
public record MemberDetails(UserResponse member, ProfileResponse profile, Activity activity, BlockInfo block,
                            boolean canBlock) {

    public record Activity(long posts, long comments, long donations, BigDecimal donatedVerified,
                           long eventsAdded) {
    }

    public record BlockInfo(boolean blocked, String reason, LocalDateTime blockedAt, String blockedBy) {
    }
}
