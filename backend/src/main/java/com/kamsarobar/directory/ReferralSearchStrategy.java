package com.kamsarobar.directory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.kamsarobar.profile.UserProfile;
import com.kamsarobar.profile.UserProfileRepository;

@Component
public class ReferralSearchStrategy implements DirectorySearchStrategy {

    private final UserProfileRepository repository;

    public ReferralSearchStrategy(UserProfileRepository repository) {
        this.repository = repository;
    }

    @Override
    public SearchType type() {
        return SearchType.REFERRAL;
    }

    @Override
    public Page<UserProfile> search(String likePattern, Long cityId, Long viewerId, Pageable pageable) {
        return cityId == null
                ? repository.findReferrers(likePattern, viewerId, pageable)
                : repository.findReferrersInCity(likePattern, cityId, viewerId, pageable);
    }
}
