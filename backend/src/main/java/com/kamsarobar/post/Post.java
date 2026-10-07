package com.kamsarobar.post;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import com.kamsarobar.city.City;
import com.kamsarobar.media.Image;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "posts")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "city_id", nullable = false)
    private City city;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false)
    private PostCategory category;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false)
    private PostVisibility visibility = PostVisibility.CITY;

    private String title;

    private String content;

    /** Set only for events and seminars. */
    private Instant eventStartsAt;

    private Instant eventEndsAt;

    private String eventLocation;

    private String eventLink;

    @OneToMany(mappedBy = "post")
    @OrderBy("position ASC")
    private List<Image> images = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected Post() {
    }

    public Post(User author, City city, PostCategory category, String title, String content) {
        this.author = author;
        this.city = city;
        this.category = category;
        this.title = title;
        this.content = content;
    }

    public void edit(PostCategory category, String title, String content) {
        this.category = category;
        this.title = title;
        this.content = content;
    }

    public void setEvent(Instant startsAt, Instant endsAt, String location, String link) {
        this.eventStartsAt = startsAt;
        this.eventEndsAt = endsAt;
        this.eventLocation = location;
        this.eventLink = link;
    }

    public PostVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(PostVisibility visibility) {
        this.visibility = visibility;
    }

    public boolean isEvent() {
        return eventStartsAt != null;
    }

    /** An event is over once its end time passes, or 6 hours after it started if no end time was given. */
    public boolean hasEnded(Instant now) {
        if (eventStartsAt == null) {
            return false;
        }
        Instant end = eventEndsAt != null ? eventEndsAt : eventStartsAt.plusSeconds(6 * 3600);
        return end.isBefore(now);
    }

    public Long getId() {
        return id;
    }

    public User getAuthor() {
        return author;
    }

    public City getCity() {
        return city;
    }

    public PostCategory getCategory() {
        return category;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public Instant getEventStartsAt() {
        return eventStartsAt;
    }

    public Instant getEventEndsAt() {
        return eventEndsAt;
    }

    public String getEventLocation() {
        return eventLocation;
    }

    public String getEventLink() {
        return eventLink;
    }

    public List<Image> getImages() {
        return images;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
