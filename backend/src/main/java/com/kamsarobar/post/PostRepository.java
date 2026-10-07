package com.kamsarobar.post;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
