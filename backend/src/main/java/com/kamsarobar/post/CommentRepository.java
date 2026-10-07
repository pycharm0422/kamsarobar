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

    /** Comments of a post, oldest first, leaving out comments by blocked members. */
    @EntityGraph(attributePaths = {"author"})
    @Query(value = "select c from Comment c where c.post.id = :postId and c.author.blocked = false order by c.createdAt asc",
            countQuery = "select count(c) from Comment c where c.post.id = :postId and c.author.blocked = false")
    Page<Comment> findVisibleByPostId(@Param("postId") Long postId, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "post", "post.city"})
    Optional<Comment> findWithPostById(Long id);

    long countByAuthorId(Long authorId);

    /** Comment counts for a page of posts in a single query (avoids N+1 on the feed). */
    @Query("select c.post.id, count(c) from Comment c where c.post.id in :postIds and c.author.blocked = false group by c.post.id")
    List<Object[]> countByPostIds(@Param("postIds") Collection<Long> postIds);
}
