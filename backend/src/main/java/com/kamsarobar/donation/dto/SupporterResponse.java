package com.kamsarobar.donation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.kamsarobar.donation.Donation;

/** Public "thank you" wall entry - hides the name of anonymous donors and never exposes transaction refs. */
public record SupporterResponse(String name, BigDecimal amount, String campaignTitle, LocalDateTime date) {

    public static SupporterResponse from(Donation d) {
        return new SupporterResponse(d.isAnonymous() ? "Anonymous" : d.getDonorName(), d.getAmount(),
                d.getCampaign() == null ? null : d.getCampaign().getTitle(), d.getVerifiedAt());
    }
}
