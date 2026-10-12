package com.kamsarobar.notification.dto;

import com.kamsarobar.user.NotificationSettings;

public record NotificationSettingsDto(boolean cityPosts, boolean cityEvents, boolean allEvents,
                                      boolean eventReminders) {

    public static NotificationSettingsDto from(NotificationSettings s) {
        return new NotificationSettingsDto(s.isCityPosts(), s.isCityEvents(), s.isAllEvents(), s.isEventReminders());
    }
}
