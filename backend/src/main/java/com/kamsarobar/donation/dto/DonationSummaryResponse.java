package com.kamsarobar.donation.dto;

import java.math.BigDecimal;
import java.util.List;

public record DonationSummaryResponse(BigDecimal totalCollected, List<CityDonationSummary> cities) {
}
