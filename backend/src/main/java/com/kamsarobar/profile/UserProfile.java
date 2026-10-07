package com.kamsarobar.profile;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

import org.hibernate.annotations.UpdateTimestamp;

import com.kamsarobar.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Form 2 - the professional profile: LinkedIn, current job, companies the member can refer to, expertise.
 */
@Entity
@Table(name = "user_profiles")
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private String linkedinUrl;

    private String currentCompany;

    private String position;

    private Integer yearsOfExperience;

    private String bio;

    /** When false the member is hidden from referral / expert search. */
    @Column(nullable = false)
    private boolean openToHelp = true;

    @ManyToMany
    @JoinTable(name = "profile_referral_companies",
            joinColumns = @JoinColumn(name = "profile_id"),
            inverseJoinColumns = @JoinColumn(name = "company_id"))
    private Set<Company> referralCompanies = new LinkedHashSet<>();

    @ManyToMany
    @JoinTable(name = "profile_skills",
            joinColumns = @JoinColumn(name = "profile_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id"))
    private Set<Skill> skills = new LinkedHashSet<>();

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected UserProfile() {
    }

    public UserProfile(User user) {
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getLinkedinUrl() {
        return linkedinUrl;
    }

    public void setLinkedinUrl(String linkedinUrl) {
        this.linkedinUrl = linkedinUrl;
    }

    public String getCurrentCompany() {
        return currentCompany;
    }

    public void setCurrentCompany(String currentCompany) {
        this.currentCompany = currentCompany;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public Integer getYearsOfExperience() {
        return yearsOfExperience;
    }

    public void setYearsOfExperience(Integer yearsOfExperience) {
        this.yearsOfExperience = yearsOfExperience;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public boolean isOpenToHelp() {
        return openToHelp;
    }

    public void setOpenToHelp(boolean openToHelp) {
        this.openToHelp = openToHelp;
    }

    public Set<Company> getReferralCompanies() {
        return referralCompanies;
    }

    public void replaceReferralCompanies(Set<Company> companies) {
        referralCompanies.clear();
        referralCompanies.addAll(companies);
    }

    public Set<Skill> getSkills() {
        return skills;
    }

    public void replaceSkills(Set<Skill> newSkills) {
        skills.clear();
        skills.addAll(newSkills);
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
