package com.kamsarobar.directory.dto;

import java.util.List;

import com.kamsarobar.city.dto.CitySummary;
import com.kamsarobar.profile.NamedTag;
import com.kamsarobar.profile.UserProfile;
import com.kamsarobar.user.User;

/**
 * A member as shown in search results, with the WhatsApp-ready mobile number so the
 * frontend can open a chat with a pre-filled referral / advice request.
 */
public record MemberCard(Long userId, String name, String mobile, CitySummary city, String linkedinUrl,
                         String currentCompany, String position, Integer yearsOfExperience, String bio,
                         List<String> referralCompanies, List<String> expertise, List<String> matched) {

    public static MemberCard from(UserProfile profile, List<String> matched) {
        User user = profile.getUser();
        return new MemberCard(user.getId(), user.getName(), user.getMobile(), CitySummary.from(user.getCity()),
                profile.getLinkedinUrl(), profile.getCurrentCompany(), profile.getPosition(),
                profile.getYearsOfExperience(), profile.getBio(),
                profile.getReferralCompanies().stream().map(NamedTag::getName).sorted().toList(),
                profile.getSkills().stream().map(NamedTag::getName).sorted().toList(), matched);
    }
}
