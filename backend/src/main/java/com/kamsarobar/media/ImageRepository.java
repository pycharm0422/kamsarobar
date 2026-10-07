package com.kamsarobar.media;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageRepository extends JpaRepository<Image, String> {

    /** Photos not attached to any post and older than the cutoff - left over from abandoned drafts. */
    List<Image> findByPostIsNullAndCreatedAtBefore(LocalDateTime cutoff, Pageable pageable);
}
