package com.kamsarobar.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Cors cors, BootstrapAdmin bootstrapAdmin, Phone phone, Storage storage,
                            Push push) {

    public record Jwt(String secret, long expirationMinutes) {
    }

    public record Cors(List<String> allowedOrigins) {
    }

    public record BootstrapAdmin(String name, String mobile, String password, String city) {
    }

    public record Phone(String defaultCountryCode) {
    }

    public record Storage(String localPath, long maxImageBytes, int maxImagesPerPost) {
    }

    /** provider: "expo" (real push notifications) or "log" (just log them). */
    public record Push(String provider, String expoAccessToken) {
    }
}
