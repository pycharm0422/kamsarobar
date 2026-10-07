package com.kamsarobar.donation.dto;

import java.math.BigDecimal;

public record CityDonationSummary(Long cityId, String cityName, BigDecimal collected, long donations,
                                  BigDecimal pending) {
}
