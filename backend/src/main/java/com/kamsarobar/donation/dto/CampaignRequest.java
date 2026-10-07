package com.kamsarobar.donation.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CampaignRequest(
        @NotBlank(message = "Title is required") @Size(max = 200) String title,
        @Size(max = 3000) String description,
        @DecimalMin(value = "1.00", message = "Goal must be positive") BigDecimal goalAmount,
        Boolean active) {
}
