package com.kamsarobar.notification;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kamsarobar.notification.dto.NotificationSettingsDto;
import com.kamsarobar.user.User;
import com.kamsarobar.user.UserService;

/** Registers phones for push notifications and stores each member's notification choices. */
@Service
@Transactional(readOnly = true)
public class DeviceService {

    private final DeviceTokenRepository tokenRepository;
    private final UserService userService;

    public DeviceService(DeviceTokenRepository tokenRepository, UserService userService) {
        this.tokenRepository = tokenRepository;
        this.userService = userService;
    }

    /** Called by the app after login and on each start; a phone that changes hands moves to the new member. */
    @Transactional
    public void register(Long userId, String token, String platform) {
        tokenRepository.findByToken(token).ifPresentOrElse(
                existing -> existing.refresh(userId, platform),
                () -> tokenRepository.save(new DeviceToken(userId, token, platform)));
    }

    /** Called on logout so the phone stops receiving this member's notifications. */
    @Transactional
    public void unregister(Long userId, String token) {
        tokenRepository.deleteForUser(token, userId);
    }

    public NotificationSettingsDto settings(Long userId) {
        return NotificationSettingsDto.from(userService.getEntity(userId).getNotificationSettings());
    }

    @Transactional
    public NotificationSettingsDto updateSettings(Long userId, NotificationSettingsDto request) {
        User user = userService.getEntity(userId);
        user.getNotificationSettings().update(request.cityPosts(), request.cityEvents(), request.allEvents(),
                request.eventReminders());
        return NotificationSettingsDto.from(user.getNotificationSettings());
    }
}
