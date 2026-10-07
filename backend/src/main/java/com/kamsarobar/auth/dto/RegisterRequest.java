package com.kamsarobar.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Form 1 - basic registration: name, mobile number and city (plus a password to sign in later). */
public record RegisterRequest(
        @NotBlank(message = "Name is required") @Size(max = 120) String name,
        @NotBlank(message = "Mobile number is required") String mobile,
        @NotNull(message = "City is required") Long cityId,
        @NotBlank @Size(min = 6, max = 100, message = "Password must be at least 6 characters") String password) {
}
