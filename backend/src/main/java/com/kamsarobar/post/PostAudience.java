package com.kamsarobar.post;

import java.util.Objects;

import com.kamsarobar.security.UserPrincipal;

/**
 * The single home of the "who can see this post" rule. A post is visible to a viewer when any of these hold:
 * Posts by blocked members are hidden from everyone. Otherwise, a post is visible when:
 * <ul>
 *   <li>the post is shared with {@link PostVisibility#EVERYONE};</li>
 *   <li>the viewer lives in the post's city;</li>
 *   <li>the viewer is the admin of the post's city (to moderate it) or the main admin;</li>
 *   <li>the viewer wrote it (even after moving to another city).</li>
 * </ul>
 * {@link #canSee(Post)} applies it in Java; {@link #JPQL_FILTER} applies the same rule inside database queries,
 * so lists never load posts the viewer may not see.
 */
public record PostAudience(Long viewerId, Long cityId, Long managedCityId, boolean seesAll) {

    /** Wrapped in parentheses so it can follow "and" directly. Requires the parameters :seesAll, :viewerId, :viewerCityId and :viewerManagedCityId. */
    public static final String JPQL_FILTER = """
            (p.author.blocked = false and (:seesAll = true
             or p.visibility = com.kamsarobar.post.PostVisibility.EVERYONE
             or p.city.id = :viewerCityId
             or p.city.id = :viewerManagedCityId
             or p.author.id = :viewerId))""";

    public static PostAudience of(UserPrincipal viewer) {
        return new PostAudience(viewer.id(), viewer.cityId(), viewer.managedCityId(), viewer.isMainAdmin());
    }

    public boolean canSee(Post post) {
        if (post.getAuthor().isBlocked()) {
            return false;
        }
        Long postCity = post.getCity().getId();
        return seesAll
                || post.getVisibility() == PostVisibility.EVERYONE
                || Objects.equals(postCity, cityId)
                || Objects.equals(postCity, managedCityId)
                || Objects.equals(post.getAuthor().getId(), viewerId);
    }
}
