package com.kamsarobar.city.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CityRequest(
        @NotBlank(message = "City name is required") @Size(max = 100) String name,
        @Size(max = 100) String state,
        Boolean active) {
}
