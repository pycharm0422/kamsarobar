package com.kamsarobar.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DeviceRequest(
        @NotBlank @Size(max = 255)
        @Pattern(regexp = "^Expo(nent)?PushToken\\[.+]$", message = "Not an Expo push token")
        String token,
        @Size(max = 20) String platform) {
}
