package com.kamsarobar.city.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Settings a city head (city admin) maintains for their area.
 */
public record CitySettingsRequest(
        @Size(max = 500)
        @Pattern(regexp = "^$|^https://(chat\\.whatsapp\\.com|wa\\.me|whatsapp\\.com)/.*$",
                message = "Must be a WhatsApp link, e.g. https://chat.whatsapp.com/XXXX")
        String whatsappGroupUrl,
        @Size(max = 150) String bankAccountName,
        @Size(max = 50) @Pattern(regexp = "^$|^[0-9]{6,20}$", message = "Account number must be 6-20 digits")
        String bankAccountNumber,
        @Size(max = 20) @Pattern(regexp = "^$|^[A-Za-z]{4}0[A-Za-z0-9]{6}$", message = "Invalid IFSC code")
        String bankIfsc,
        @Size(max = 150) String bankName,
        @Size(max = 100) @Pattern(regexp = "^$|^[\\w.\\-]{2,}@[A-Za-z]{2,}$", message = "Invalid UPI id")
        String upiId) {
}
