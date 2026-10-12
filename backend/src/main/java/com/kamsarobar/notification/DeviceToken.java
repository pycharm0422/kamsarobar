package com.kamsarobar.notification;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A phone that receives push notifications for a member (one member may have several phones). */
@Entity
@Table(name = "device_tokens")
public class DeviceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, unique = true)
    private String token;

    private String platform;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime lastSeenAt = LocalDateTime.now();

    protected DeviceToken() {
    }

    public DeviceToken(Long userId, String token, String platform) {
        this.userId = userId;
        this.token = token;
        this.platform = platform;
    }

    /** The same phone signed in again (possibly as another member). */
    public void refresh(Long userId, String platform) {
        this.userId = userId;
        this.platform = platform;
        this.lastSeenAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getToken() {
        return token;
    }
}
