package com.kamsarobar.post;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventAttendeeRepository extends JpaRepository<EventAttendee, EventAttendee.Key> {

    long countByUserId(Long userId);

    /**
     * Attendees whose event starts within the window and who have not been reminded yet:
     * rows of [postId, userId, postTitle, eventStartsAt].
     */
    @Query("""
            select a.postId, a.userId, p.title, p.eventStartsAt from EventAttendee a, Post p, User u
            where p.id = a.postId and u.id = a.userId and a.reminderSentAt is null
              and p.eventStartsAt > :now and p.eventStartsAt <= :until
              and u.blocked = false and u.notificationSettings.eventReminders = true and p.author.blocked = false
            order by p.eventStartsAt
            """)
    List<Object[]> findDueReminders(@Param("now") java.time.Instant now, @Param("until") java.time.Instant until,
                                    org.springframework.data.domain.Pageable pageable);

    /** Claims a reminder; returns 1 for exactly one caller even if several servers run the job at once. */
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("""
            update EventAttendee a set a.reminderSentAt = :sentAt
            where a.postId = :postId and a.userId = :userId and a.reminderSentAt is null
            """)
    int claimReminder(@Param("postId") Long postId, @Param("userId") Long userId,
                      @Param("sentAt") java.time.LocalDateTime sentAt);

    /** Rows of [postId, count] for a page of posts, in one query. */
    @Query("select a.postId, count(a) from EventAttendee a where a.postId in :postIds group by a.postId")
    List<Object[]> countByPostIds(@Param("postIds") Collection<Long> postIds);

    @Query("select a.postId from EventAttendee a where a.userId = :userId and a.postId in :postIds")
    List<Long> findAttendingPostIds(@Param("userId") Long userId, @Param("postIds") Collection<Long> postIds);
}
