package com.kamsarobar.donation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.kamsarobar.city.City;
import com.kamsarobar.user.User;

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

/**
 * A contribution made by bank transfer / UPI to the city's account. The member records it here with the
 * transaction reference, and the city admin verifies it against the bank statement.
 */
@Entity
@Table(name = "donations")
public class Donation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "city_id", nullable = false)
    private City city;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "donor_id")
    private User donor;

    @Column(nullable = false)
    private String donorName;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    private String transactionRef;

    private String note;

    @Column(nullable = false)
    private boolean anonymous;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false)
    private DonationStatus status = DonationStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by_id")
    private User verifiedBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime verifiedAt;

    protected Donation() {
    }

    public Donation(City city, Campaign campaign, User donor, BigDecimal amount, String transactionRef, String note,
                    boolean anonymous) {
        this.city = city;
        this.campaign = campaign;
        this.donor = donor;
        this.donorName = donor.getName();
        this.amount = amount;
        this.transactionRef = transactionRef;
        this.note = note;
        this.anonymous = anonymous;
    }

    public void review(DonationStatus newStatus, User reviewer) {
        this.status = newStatus;
        this.verifiedBy = reviewer;
        this.verifiedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public City getCity() {
        return city;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public User getDonor() {
        return donor;
    }

    public String getDonorName() {
        return donorName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getTransactionRef() {
        return transactionRef;
    }

    public String getNote() {
        return note;
    }

    public boolean isAnonymous() {
        return anonymous;
    }

    public DonationStatus getStatus() {
        return status;
    }

    public User getVerifiedBy() {
        return verifiedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }
}
