package com.kamsarobar.common.util;

import java.util.Locale;

public final class TextNormalizer {

    private TextNormalizer() {
    }

    /** Trims and collapses inner whitespace, keeping the original case (for display). */
    public static String clean(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim().replaceAll("\\s+", " ");
        return cleaned.isEmpty() ? null : cleaned;
    }

    /** Case-insensitive key used for lookups and de-duplication. */
    public static String key(String value) {
        String cleaned = clean(value);
        return cleaned == null ? null : cleaned.toLowerCase(Locale.ROOT);
    }

    /** Escapes LIKE wildcards in user input so "%" or "_" are matched literally. */
    public static String likeContains(String normalized) {
        String escaped = normalized.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
