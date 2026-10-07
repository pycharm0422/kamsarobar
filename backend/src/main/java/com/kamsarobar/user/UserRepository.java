package com.kamsarobar.user;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = {"city", "managedCity"})
    Optional<User> findByMobile(String mobile);

    @EntityGraph(attributePaths = {"city", "managedCity"})
    Optional<User> findWithCitiesById(Long id);

    boolean existsByMobile(String mobile);

    boolean existsByRole(Role role);

    @EntityGraph(attributePaths = {"city", "managedCity"})
    List<User> findAllByRoleOrderByNameAsc(Role role);

    @EntityGraph(attributePaths = {"city", "managedCity"})
    @Query(value = """
            select u from User u
            where (:cityId is null or u.city.id = :cityId)
              and (:pattern is null or lower(u.name) like :pattern or u.mobile like :pattern)
            order by u.name asc
            """,
            countQuery = """
            select count(u) from User u
            where (:cityId is null or u.city.id = :cityId)
              and (:pattern is null or lower(u.name) like :pattern or u.mobile like :pattern)
            """)
    Page<User> search(@Param("cityId") Long cityId, @Param("pattern") String pattern, Pageable pageable);

    long countByCityId(Long cityId);
}
