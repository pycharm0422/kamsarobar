package com.kamsarobar.donation.dto;

import com.kamsarobar.donation.DonationStatus;

import jakarta.validation.constraints.NotNull;

public record DonationReviewRequest(@NotNull DonationStatus status) {
}
