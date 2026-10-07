package com.kamsarobar.admin.dto;

import java.math.BigDecimal;

public record AdminStats(long members, long cities, long posts, long cityAdmins, BigDecimal totalCollected) {
}
