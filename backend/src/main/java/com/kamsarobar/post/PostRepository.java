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

    /** The feed a viewer is allowed to see; cityId and category are optional filters (null = any). */
    @EntityGraph(attributePaths = {"author", "city"})
    @Query(value = """
            select p from Post p
            where (:cityId is null or p.city.id = :cityId)
              and (:category is null or p.category = :category)
              and """ + PostAudience.JPQL_FILTER + """

            order by p.createdAt desc
            """,
            countQuery = """
            select count(p) from Post p
            where (:cityId is null or p.city.id = :cityId)
              and (:category is null or p.category = :category)
              and """ + PostAudience.JPQL_FILTER)
    Page<Post> findFeed(@Param("cityId") Long cityId, @Param("category") PostCategory category,
                        @Param("seesAll") boolean seesAll, @Param("viewerId") Long viewerId,
                        @Param("viewerCityId") Long viewerCityId,
                        @Param("viewerManagedCityId") Long viewerManagedCityId, Pageable pageable);

    default Page<Post> findFeed(Long cityId, PostCategory category, PostAudience audience, Pageable pageable) {
        return findFeed(cityId, category, audience.seesAll(), audience.viewerId(), audience.cityId(),
                audience.managedCityId(), pageable);
    }

    @EntityGraph(attributePaths = {"author", "city"})
    Optional<Post> findWithAuthorById(Long id);

    // --- Events & seminars. "Upcoming" = not yet ended: end time in the future, or (no end time)
    // started less than 6 hours ago. Visibility rules apply here too. ---

    String UPCOMING = """
            p.eventStartsAt is not null
            and (p.eventEndsAt >= :now or (p.eventEndsAt is null and p.eventStartsAt >= :startedAfter))
            and """ + PostAudience.JPQL_FILTER;

    @EntityGraph(attributePaths = {"author", "city"})
    @Query(value = "select p from Post p where " + UPCOMING + """

             and exists (select 1 from EventAttendee a where a.postId = p.id and a.userId = :viewerId)
            order by p.eventStartsAt asc
            """,
            countQuery = "select count(p) from Post p where " + UPCOMING + """

             and exists (select 1 from EventAttendee a where a.postId = p.id and a.userId = :viewerId)
            """)
    Page<Post> findUpcomingAttended(@Param("now") Instant now, @Param("startedAfter") Instant startedAfter,
                                    @Param("seesAll") boolean seesAll, @Param("viewerId") Long viewerId,
                                    @Param("viewerCityId") Long viewerCityId,
                                    @Param("viewerManagedCityId") Long viewerManagedCityId, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "city"})
    @Query(value = "select p from Post p where (:cityId is null or p.city.id = :cityId) and " + UPCOMING
            + " order by p.eventStartsAt asc",
            countQuery = "select count(p) from Post p where (:cityId is null or p.city.id = :cityId) and " + UPCOMING)
    Page<Post> findUpcoming(@Param("cityId") Long cityId, @Param("now") Instant now,
                            @Param("startedAfter") Instant startedAfter, @Param("seesAll") boolean seesAll,
                            @Param("viewerId") Long viewerId, @Param("viewerCityId") Long viewerCityId,
                            @Param("viewerManagedCityId") Long viewerManagedCityId, Pageable pageable);

    default Page<Post> findUpcomingAttended(PostAudience a, Instant now, Instant startedAfter, Pageable pageable) {
        return findUpcomingAttended(now, startedAfter, a.seesAll(), a.viewerId(), a.cityId(), a.managedCityId(),
                pageable);
    }

    default Page<Post> findUpcoming(Long cityId, PostAudience a, Instant now, Instant startedAfter,
                                    Pageable pageable) {
        return findUpcoming(cityId, now, startedAfter, a.seesAll(), a.viewerId(), a.cityId(), a.managedCityId(),
                pageable);
    }
}
