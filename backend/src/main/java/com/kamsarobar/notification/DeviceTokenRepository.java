package com.kamsarobar.notification;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * Audience queries return [deviceTokenId, token] rows in id order so large audiences are read in pages
 * (keyset pagination: pass the last id of the previous page as afterId).
 */
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    Optional<DeviceToken> findByToken(String token);

    List<DeviceToken> findByUserId(Long userId);

    @Modifying
    @Transactional
    @Query("delete from DeviceToken d where d.token in :tokens")
    int deleteByTokens(@Param("tokens") Collection<String> tokens);

    @Modifying
    @Transactional
    @Query("delete from DeviceToken d where d.token = :token and d.userId = :userId")
    int deleteForUser(@Param("token") String token, @Param("userId") Long userId);

    /** Members living in the city who want new-post notifications. */
    @Query("""
            select d.id, d.token from DeviceToken d join User u on u.id = d.userId
            where d.id > :afterId and u.id <> :authorId and u.blocked = false
              and u.city.id = :cityId and u.notificationSettings.cityPosts = true
            order by d.id
            """)
    List<Object[]> findPostAudience(@Param("cityId") Long cityId, @Param("authorId") Long authorId,
                                    @Param("afterId") Long afterId, Pageable pageable);

    /**
     * Members of the event's city who want event notifications, plus - for events shared with everyone -
     * members of other cities who opted in to events from all cities.
     */
    @Query("""
            select d.id, d.token from DeviceToken d join User u on u.id = d.userId
            where d.id > :afterId and u.id <> :authorId and u.blocked = false
              and ((u.city.id = :cityId and u.notificationSettings.cityEvents = true)
                   or (:everyone = true and u.city.id <> :cityId and u.notificationSettings.allEvents = true))
            order by d.id
            """)
    List<Object[]> findEventAudience(@Param("cityId") Long cityId, @Param("everyone") boolean everyone,
                                     @Param("authorId") Long authorId, @Param("afterId") Long afterId,
                                     Pageable pageable);
}
