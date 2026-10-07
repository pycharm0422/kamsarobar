package com.kamsarobar.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BlockRequest(
        @NotBlank(message = "Please give a reason, e.g. spam or fake account") @Size(max = 300) String reason) {
}
