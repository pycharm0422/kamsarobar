package com.kamsarobar.profile;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    @EntityGraph(attributePaths = {"user", "user.city"})
    Optional<UserProfile> findByUserId(Long userId);

    // --- Referral search: members who can refer to a company matching the pattern ---

    @EntityGraph(attributePaths = {"user", "user.city"})
    @Query(value = """
            select p from UserProfile p
            where p.openToHelp = true and p.user.blocked = false and p.user.id <> :viewerId
              and exists (select 1 from UserProfile p2 join p2.referralCompanies c
                          where p2 = p and c.normalizedName like :pattern escape '\\')
            order by p.user.name asc
            """,
            countQuery = """
            select count(p) from UserProfile p
            where p.openToHelp = true and p.user.blocked = false and p.user.id <> :viewerId
              and exists (select 1 from UserProfile p2 join p2.referralCompanies c
                          where p2 = p and c.normalizedName like :pattern escape '\\')
            """)
    Page<UserProfile> findReferrers(@Param("pattern") String pattern, @Param("viewerId") Long viewerId,
                                    Pageable pageable);

    @EntityGraph(attributePaths = {"user", "user.city"})
    @Query(value = """
            select p from UserProfile p
            where p.openToHelp = true and p.user.blocked = false and p.user.id <> :viewerId and p.user.city.id = :cityId
              and exists (select 1 from UserProfile p2 join p2.referralCompanies c
                          where p2 = p and c.normalizedName like :pattern escape '\\')
            order by p.user.name asc
            """,
            countQuery = """
            select count(p) from UserProfile p
            where p.openToHelp = true and p.user.blocked = false and p.user.id <> :viewerId and p.user.city.id = :cityId
              and exists (select 1 from UserProfile p2 join p2.referralCompanies c
                          where p2 = p and c.normalizedName like :pattern escape '\\')
            """)
    Page<UserProfile> findReferrersInCity(@Param("pattern") String pattern, @Param("cityId") Long cityId,
                                          @Param("viewerId") Long viewerId, Pageable pageable);

    // --- Expert search: members whose expertise matches the pattern ---

    @EntityGraph(attributePaths = {"user", "user.city"})
    @Query(value = """
            select p from UserProfile p
            where p.openToHelp = true and p.user.blocked = false and p.user.id <> :viewerId
              and exists (select 1 from UserProfile p2 join p2.skills s
                          where p2 = p and s.normalizedName like :pattern escape '\\')
            order by p.user.name asc
            """,
            countQuery = """
            select count(p) from UserProfile p
            where p.openToHelp = true and p.user.blocked = false and p.user.id <> :viewerId
              and exists (select 1 from UserProfile p2 join p2.skills s
                          where p2 = p and s.normalizedName like :pattern escape '\\')
            """)
    Page<UserProfile> findExperts(@Param("pattern") String pattern, @Param("viewerId") Long viewerId,
                                  Pageable pageable);

    @EntityGraph(attributePaths = {"user", "user.city"})
    @Query(value = """
            select p from UserProfile p
            where p.openToHelp = true and p.user.blocked = false and p.user.id <> :viewerId and p.user.city.id = :cityId
              and exists (select 1 from UserProfile p2 join p2.skills s
                          where p2 = p and s.normalizedName like :pattern escape '\\')
            order by p.user.name asc
            """,
            countQuery = """
            select count(p) from UserProfile p
            where p.openToHelp = true and p.user.blocked = false and p.user.id <> :viewerId and p.user.city.id = :cityId
              and exists (select 1 from UserProfile p2 join p2.skills s
                          where p2 = p and s.normalizedName like :pattern escape '\\')
            """)
    Page<UserProfile> findExpertsInCity(@Param("pattern") String pattern, @Param("cityId") Long cityId,
                                        @Param("viewerId") Long viewerId, Pageable pageable);
}
