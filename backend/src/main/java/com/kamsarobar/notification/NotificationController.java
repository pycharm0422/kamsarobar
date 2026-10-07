package com.kamsarobar.notification;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kamsarobar.notification.dto.DeviceRequest;
import com.kamsarobar.notification.dto.NotificationSettingsDto;
import com.kamsarobar.security.UserPrincipal;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final DeviceService deviceService;

    public NotificationController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @PutMapping("/devices")
    public ResponseEntity<Void> register(@Valid @RequestBody DeviceRequest request,
                                         @AuthenticationPrincipal UserPrincipal user) {
        deviceService.register(user.id(), request.token(), request.platform());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/devices")
    public ResponseEntity<Void> unregister(@RequestParam String token, @AuthenticationPrincipal UserPrincipal user) {
        deviceService.unregister(user.id(), token);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/settings")
    public NotificationSettingsDto settings(@AuthenticationPrincipal UserPrincipal user) {
        return deviceService.settings(user.id());
    }

    @PutMapping("/settings")
    public NotificationSettingsDto updateSettings(@RequestBody NotificationSettingsDto request,
                                                  @AuthenticationPrincipal UserPrincipal user) {
        return deviceService.updateSettings(user.id(), request);
    }
}
