package com.kamsarobar.donation.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DonationRequest(
        @NotNull(message = "City is required") Long cityId,
        Long campaignId,
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "1.00", message = "Minimum amount is 1")
        @DecimalMax(value = "1000000.00", message = "Maximum amount is 10,00,000")
        @Digits(integer = 10, fraction = 2)
        BigDecimal amount,
        @Size(max = 100) String transactionRef,
        @Size(max = 500) String note,
        boolean anonymous) {
}
