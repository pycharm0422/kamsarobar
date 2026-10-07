package com.kamsarobar.profile;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kamsarobar.common.util.TextNormalizer;
import com.kamsarobar.profile.dto.ProfileRequest;
import com.kamsarobar.profile.dto.ProfileResponse;
import com.kamsarobar.user.UserService;

@Service
@Transactional(readOnly = true)
public class ProfileService {

    private final UserProfileRepository profileRepository;
    private final UserService userService;
    private final TagResolver<Company> companyResolver;
    private final TagResolver<Skill> skillResolver;

    public ProfileService(UserProfileRepository profileRepository, UserService userService,
                          TagResolver<Company> companyResolver, TagResolver<Skill> skillResolver) {
        this.profileRepository = profileRepository;
        this.userService = userService;
        this.companyResolver = companyResolver;
        this.skillResolver = skillResolver;
    }

    public ProfileResponse getForUser(Long userId) {
        return profileRepository.findByUserId(userId).map(ProfileResponse::from).orElseGet(ProfileResponse::empty);
    }

    @Transactional
    public ProfileResponse save(Long userId, ProfileRequest request) {
        UserProfile profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> new UserProfile(userService.getEntity(userId)));

        profile.setLinkedinUrl(normalizeUrl(request.linkedinUrl()));
        profile.setCurrentCompany(TextNormalizer.clean(request.currentCompany()));
        profile.setPosition(TextNormalizer.clean(request.position()));
        profile.setYearsOfExperience(request.yearsOfExperience());
        profile.setBio(TextNormalizer.clean(request.bio()));
        if (request.openToHelp() != null) {
            profile.setOpenToHelp(request.openToHelp());
        }
        profile.replaceReferralCompanies(companyResolver.resolve(request.referralCompanies()));
        profile.replaceSkills(skillResolver.resolve(request.expertise()));
        return ProfileResponse.from(profileRepository.save(profile));
    }

    public List<String> suggestCompanies(String query) {
        return companyResolver.suggest(query);
    }

    public List<String> suggestExpertise(String query) {
        return skillResolver.suggest(query);
    }

    private static String normalizeUrl(String url) {
        String cleaned = TextNormalizer.clean(url);
        if (cleaned == null) {
            return null;
        }
        return cleaned.startsWith("http") ? cleaned : "https://" + cleaned;
    }
}
