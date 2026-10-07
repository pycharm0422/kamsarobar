package com.kamsarobar.directory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.kamsarobar.profile.UserProfile;

/**
 * One way of finding members. To add a new search (e.g. by current position), implement this interface
 * as a Spring bean - {@link DirectoryService} picks it up automatically, no existing code changes.
 */
public interface DirectorySearchStrategy {

    SearchType type();

    /**
     * @param likePattern already-normalised, escaped LIKE pattern
     * @param cityId      optional city filter (null = all cities)
     * @param viewerId    the member searching; excluded from the results
     */
    Page<UserProfile> search(String likePattern, Long cityId, Long viewerId, Pageable pageable);
}
