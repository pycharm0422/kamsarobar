package com.kamsarobar.directory;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kamsarobar.common.exception.BadRequestException;
import com.kamsarobar.common.util.TextNormalizer;
import com.kamsarobar.common.web.PageResponse;
import com.kamsarobar.directory.dto.MemberCard;
import com.kamsarobar.profile.NamedTag;
import com.kamsarobar.profile.UserProfile;

@Service
@Transactional(readOnly = true)
public class DirectoryService {

    private final Map<SearchType, DirectorySearchStrategy> strategies = new EnumMap<>(SearchType.class);

    public DirectoryService(List<DirectorySearchStrategy> strategies) {
        strategies.forEach(strategy -> this.strategies.put(strategy.type(), strategy));
    }

    public PageResponse<MemberCard> search(SearchType type, String term, Long cityId, Long viewerId,
                                           Pageable pageable) {
        String key = TextNormalizer.key(term);
        if (key == null || key.length() < 2) {
            throw new BadRequestException("Type at least 2 characters to search");
        }
        DirectorySearchStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new BadRequestException("Unsupported search: " + type);
        }
        Page<UserProfile> page = strategy.search(TextNormalizer.likeContains(key), cityId, viewerId, pageable);
        return PageResponse.of(page, profile -> MemberCard.from(profile, matchedTags(type, profile, key)));
    }

    private static List<String> matchedTags(SearchType type, UserProfile profile, String key) {
        Collection<? extends NamedTag> tags = type == SearchType.REFERRAL
                ? profile.getReferralCompanies()
                : profile.getSkills();
        return tags.stream().filter(t -> t.getNormalizedName().contains(key)).map(NamedTag::getName).sorted()
                .toList();
    }
}
