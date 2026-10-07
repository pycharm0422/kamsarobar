package com.kamsarobar.media;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.kamsarobar.post.Post;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** An uploaded photo. Its id is a random UUID, which also makes the public image URL unguessable. */
@Entity
@Table(name = "images")
public class Image {

    @Id
    private String id;

    @Column(name = "uploader_id")
    private Long uploaderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false)
    private String storageKey;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private long sizeBytes;

    private Integer width;

    private Integer height;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Image() {
    }

    public Image(String id, Long uploaderId, String storageKey, String contentType, long sizeBytes, Integer width,
                 Integer height) {
        this.id = id;
        this.uploaderId = uploaderId;
        this.storageKey = storageKey;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.width = width;
        this.height = height;
    }

    public void attachTo(Post post, int position) {
        this.post = post;
        this.position = position;
    }

    public void detach() {
        this.post = null;
    }

    public String getId() {
        return id;
    }

    public Long getUploaderId() {
        return uploaderId;
    }

    public Post getPost() {
        return post;
    }

    public int getPosition() {
        return position;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public Integer getWidth() {
        return width;
    }

    public Integer getHeight() {
        return height;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
