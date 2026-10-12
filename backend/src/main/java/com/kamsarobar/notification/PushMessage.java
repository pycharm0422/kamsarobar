package com.kamsarobar.notification;

import java.util.Map;

/** One notification to one device. {@code data} travels with it so the app can open the right screen. */
public record PushMessage(String to, String title, String body, Map<String, Object> data) {
}
