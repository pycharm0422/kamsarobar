package com.kamsarobar.donation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.kamsarobar.city.dto.CitySummary;
import com.kamsarobar.donation.Donation;
import com.kamsarobar.donation.DonationStatus;

public record DonationResponse(Long id, CitySummary city, Long campaignId, String campaignTitle, String donorName,
                               BigDecimal amount, String transactionRef, String note, boolean anonymous,
                               DonationStatus status, LocalDateTime createdAt, LocalDateTime verifiedAt) {

    public static DonationResponse from(Donation d) {
        return new DonationResponse(d.getId(), CitySummary.from(d.getCity()),
                d.getCampaign() == null ? null : d.getCampaign().getId(),
                d.getCampaign() == null ? null : d.getCampaign().getTitle(),
                d.getDonorName(), d.getAmount(), d.getTransactionRef(), d.getNote(), d.isAnonymous(), d.getStatus(),
                d.getCreatedAt(), d.getVerifiedAt());
    }
}
