package com.kamsarobar.admin.dto;

import jakarta.validation.constraints.NotNull;

/** replaceExistingAdmin: when moving a city admin to a city that already has one, replace that admin. */
public record ChangeCityRequest(@NotNull Long cityId, boolean replaceExistingAdmin) {
}
