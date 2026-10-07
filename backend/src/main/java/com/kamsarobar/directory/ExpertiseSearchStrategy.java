package com.kamsarobar.directory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.kamsarobar.profile.UserProfile;
import com.kamsarobar.profile.UserProfileRepository;

@Component
public class ExpertiseSearchStrategy implements DirectorySearchStrategy {

    private final UserProfileRepository repository;

    public ExpertiseSearchStrategy(UserProfileRepository repository) {
        this.repository = repository;
    }

    @Override
    public SearchType type() {
        return SearchType.EXPERTISE;
    }

    @Override
    public Page<UserProfile> search(String likePattern, Long cityId, Long viewerId, Pageable pageable) {
        return cityId == null
                ? repository.findExperts(likePattern, viewerId, pageable)
                : repository.findExpertsInCity(likePattern, cityId, viewerId, pageable);
    }
}
