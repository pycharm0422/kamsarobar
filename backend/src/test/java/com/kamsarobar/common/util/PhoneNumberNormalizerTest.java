package com.kamsarobar.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.kamsarobar.common.exception.BadRequestException;
import com.kamsarobar.config.AppProperties;

class PhoneNumberNormalizerTest {

    private final PhoneNumberNormalizer normalizer = new PhoneNumberNormalizer(
            new AppProperties(null, new AppProperties.Cors(List.of()), null, new AppProperties.Phone("91"), null));

    @Test
    void addsDefaultCountryCodeToTenDigitNumbers() {
        assertThat(normalizer.normalize("98765 43210")).isEqualTo("919876543210");
        assertThat(normalizer.normalize("09876543210")).isEqualTo("919876543210");
    }

    @Test
    void keepsInternationalNumbers() {
        assertThat(normalizer.normalize("+91 98765-43210")).isEqualTo("919876543210");
        assertThat(normalizer.normalize("00971501234567")).isEqualTo("971501234567");
    }

    @Test
    void rejectsInvalidNumbers() {
        assertThatThrownBy(() -> normalizer.normalize("12345")).isInstanceOf(BadRequestException.class);
    }
}
