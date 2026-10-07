package com.kamsarobar.profile;

import com.kamsarobar.common.util.TextNormalizer;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

/**
 * A free-text label (company, skill...) stored once and shared by many profiles.
 * {@code normalizedName} is the case-insensitive key used for de-duplication and search.
 */
@MappedSuperclass
public abstract class NamedTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String normalizedName;

    protected NamedTag() {
    }

    protected NamedTag(String name) {
        this.name = TextNormalizer.clean(name);
        this.normalizedName = TextNormalizer.key(name);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getNormalizedName() {
        return normalizedName;
    }
}
