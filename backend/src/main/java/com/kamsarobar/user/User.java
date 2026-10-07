package com.kamsarobar.user;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import com.kamsarobar.city.City;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String mobile;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false)
    private Role role = Role.MEMBER;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "city_id", nullable = false)
    private City city;

    /** Set only for city admins: the city they head. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "managed_city_id")
    private City managedCity;

    /** Blocked members cannot log in, are hidden from search, and their posts and comments are hidden. */
    @Column(nullable = false)
    private boolean blocked;

    private String blockedReason;

    private LocalDateTime blockedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_by_id")
    private User blockedBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected User() {
    }

    public User(String name, String mobile, String passwordHash, City city) {
        this.name = name;
        this.mobile = mobile;
        this.passwordHash = passwordHash;
        this.city = city;
    }

    public void makeCityAdmin(City cityToManage) {
        this.role = Role.CITY_ADMIN;
        this.managedCity = cityToManage;
    }

    public void block(String reason, User by) {
        this.blocked = true;
        this.blockedReason = reason;
        this.blockedAt = LocalDateTime.now();
        this.blockedBy = by;
    }

    /** Fully restores the account; the block history fields are cleared. */
    public void unblock() {
        this.blocked = false;
        this.blockedReason = null;
        this.blockedAt = null;
        this.blockedBy = null;
    }

    public void revokeCityAdmin() {
        if (role == Role.CITY_ADMIN) {
            this.role = Role.MEMBER;
        }
        this.managedCity = null;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public City getCity() {
        return city;
    }

    public void setCity(City city) {
        this.city = city;
    }

    public City getManagedCity() {
        return managedCity;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public String getBlockedReason() {
        return blockedReason;
    }

    public LocalDateTime getBlockedAt() {
        return blockedAt;
    }

    public User getBlockedBy() {
        return blockedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
