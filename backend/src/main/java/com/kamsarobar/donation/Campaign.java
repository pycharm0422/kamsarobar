package com.kamsarobar.donation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.kamsarobar.city.City;
import com.kamsarobar.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A fundraising cause in a city, e.g. "Winter blankets drive". */
@Entity
@Table(name = "campaigns")
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "city_id", nullable = false)
    private City city;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(precision = 12, scale = 2)
    private BigDecimal goalAmount;

    @Column(nullable = false)
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Campaign() {
    }

    public Campaign(City city, String title, String description, BigDecimal goalAmount, User createdBy) {
        this.city = city;
        this.title = title;
        this.description = description;
        this.goalAmount = goalAmount;
        this.createdBy = createdBy;
    }

    public void edit(String title, String description, BigDecimal goalAmount, boolean active) {
        this.title = title;
        this.description = description;
        this.goalAmount = goalAmount;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public City getCity() {
        return city;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getGoalAmount() {
        return goalAmount;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
