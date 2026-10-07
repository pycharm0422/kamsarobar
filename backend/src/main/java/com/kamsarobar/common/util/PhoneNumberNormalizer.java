package com.kamsarobar.common.util;

import org.springframework.stereotype.Component;

import com.kamsarobar.common.exception.BadRequestException;
import com.kamsarobar.config.AppProperties;

/**
 * Normalises mobile numbers to digits-only international format (e.g. 919876543210),
 * which is also the format WhatsApp's click-to-chat links expect.
 */
@Component
public class PhoneNumberNormalizer {

    private final String defaultCountryCode;

    public PhoneNumberNormalizer(AppProperties properties) {
        this.defaultCountryCode = properties.phone().defaultCountryCode();
    }

    public String normalize(String raw) {
        if (raw == null) {
            throw new BadRequestException("Mobile number is required");
        }
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.startsWith("00")) {
            digits = digits.substring(2);
        } else if (digits.length() == 11 && digits.startsWith("0")) {
            digits = digits.substring(1);
        }
        if (digits.length() == 10) {
            digits = defaultCountryCode + digits;
        }
        if (digits.length() < 11 || digits.length() > 15) {
            throw new BadRequestException("Please enter a valid mobile number");
        }
        return digits;
    }
}
