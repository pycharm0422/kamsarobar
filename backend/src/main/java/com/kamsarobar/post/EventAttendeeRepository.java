package com.kamsarobar.post;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventAttendeeRepository extends JpaRepository<EventAttendee, EventAttendee.Key> {

    /** Rows of [postId, count] for a page of posts, in one query. */
    @Query("select a.postId, count(a) from EventAttendee a where a.postId in :postIds group by a.postId")
    List<Object[]> countByPostIds(@Param("postIds") Collection<Long> postIds);

    @Query("select a.postId from EventAttendee a where a.userId = :userId and a.postId in :postIds")
    List<Long> findAttendingPostIds(@Param("userId") Long userId, @Param("postIds") Collection<Long> postIds);
}
