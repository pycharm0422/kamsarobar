package com.kamsarobar.donation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.kamsarobar.donation.Campaign;

public record CampaignResponse(Long id, Long cityId, String cityName, String title, String description,
                               BigDecimal goalAmount, BigDecimal raisedAmount, boolean active,
                               LocalDateTime createdAt) {

    public static CampaignResponse from(Campaign c, BigDecimal raised) {
        return new CampaignResponse(c.getId(), c.getCity().getId(), c.getCity().getName(), c.getTitle(),
                c.getDescription(), c.getGoalAmount(), raised, c.isActive(), c.getCreatedAt());
    }
}
