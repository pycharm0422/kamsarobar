package com.kamsarobar.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Edits the basic (first) registration form. */
public record UpdateUserRequest(
        @NotBlank(message = "Name is required") @Size(max = 120) String name,
        @NotBlank(message = "Mobile number is required") String mobile,
        @NotNull(message = "City is required") Long cityId) {
}
