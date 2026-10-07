package com.kamsarobar.admin.dto;

import jakarta.validation.constraints.NotNull;

public record AssignCityAdminRequest(@NotNull Long userId, @NotNull Long cityId) {
}
