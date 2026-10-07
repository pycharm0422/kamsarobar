package com.kamsarobar.post;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

    @EntityGraph(attributePaths = {"author", "city"})
    Page<Post> findByCityIdOrderByCreatedAtDesc(Long cityId, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "city"})
    Page<Post> findByCityIdAndCategoryOrderByCreatedAtDesc(Long cityId, PostCategory category, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "city"})
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"author", "city"})
    Page<Post> findByCategoryOrderByCreatedAtDesc(PostCategory category, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "city"})
    Optional<Post> findWithAuthorById(Long id);

    // --- Events & seminars. "Upcoming" = not yet ended: end time in the future, or (no end time)
    // started less than 6 hours ago. ---

    @EntityGraph(attributePaths = {"author", "city"})
    @Query(value = """
            select p from Post p
            where p.eventStartsAt is not null
              and (p.eventEndsAt >= :now or (p.eventEndsAt is null and p.eventStartsAt >= :startedAfter))
              and exists (select 1 from EventAttendee a where a.postId = p.id and a.userId = :userId)
            order by p.eventStartsAt asc
            """,
            countQuery = """
            select count(p) from Post p
            where p.eventStartsAt is not null
              and (p.eventEndsAt >= :now or (p.eventEndsAt is null and p.eventStartsAt >= :startedAfter))
              and exists (select 1 from EventAttendee a where a.postId = p.id and a.userId = :userId)
            """)
    Page<Post> findUpcomingAttendedBy(@Param("userId") Long userId, @Param("now") Instant now,
                                      @Param("startedAfter") Instant startedAfter, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "city"})
    @Query(value = """
            select p from Post p
            where p.eventStartsAt is not null and p.city.id = :cityId
              and (p.eventEndsAt >= :now or (p.eventEndsAt is null and p.eventStartsAt >= :startedAfter))
            order by p.eventStartsAt asc
            """,
            countQuery = """
            select count(p) from Post p
            where p.eventStartsAt is not null and p.city.id = :cityId
              and (p.eventEndsAt >= :now or (p.eventEndsAt is null and p.eventStartsAt >= :startedAfter))
            """)
    Page<Post> findUpcomingInCity(@Param("cityId") Long cityId, @Param("now") Instant now,
                                  @Param("startedAfter") Instant startedAfter, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "city"})
    @Query(value = """
            select p from Post p
            where p.eventStartsAt is not null
              and (p.eventEndsAt >= :now or (p.eventEndsAt is null and p.eventStartsAt >= :startedAfter))
            order by p.eventStartsAt asc
            """,
            countQuery = """
            select count(p) from Post p
            where p.eventStartsAt is not null
              and (p.eventEndsAt >= :now or (p.eventEndsAt is null and p.eventStartsAt >= :startedAfter))
            """)
    Page<Post> findUpcoming(@Param("now") Instant now, @Param("startedAfter") Instant startedAfter,
                            Pageable pageable);
}
