package com.kamsarobar.post;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = {"author"})
    Page<Comment> findByPostIdOrderByCreatedAtAsc(Long postId, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "post", "post.city"})
    Optional<Comment> findWithPostById(Long id);

    long countByPostId(Long postId);

    /** Comment counts for a page of posts in a single query (avoids N+1 on the feed). */
    @Query("select c.post.id, count(c) from Comment c where c.post.id in :postIds group by c.post.id")
    List<Object[]> countByPostIds(@Param("postIds") Collection<Long> postIds);
}
