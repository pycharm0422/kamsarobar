package com.kamsarobar.admin.dto;

import jakarta.validation.constraints.NotNull;

/** replaceExistingAdmin: if the city already has an admin, make this member the admin instead. */
public record AssignCityAdminRequest(@NotNull Long userId, @NotNull Long cityId, boolean replaceExistingAdmin) {
}
