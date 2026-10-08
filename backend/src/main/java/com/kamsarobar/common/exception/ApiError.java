package com.kamsarobar.common.exception;

import java.time.LocalDateTime;
import java.util.Map;

public record ApiError(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        Map<String, String> fieldErrors,
        /** Set for errors the app reacts to specifically, e.g. CITY_HAS_ADMIN. */
        String code) {
}
