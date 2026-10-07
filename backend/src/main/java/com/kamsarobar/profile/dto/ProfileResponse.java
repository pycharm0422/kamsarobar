package com.kamsarobar.profile.dto;

import java.util.List;

import com.kamsarobar.profile.NamedTag;
import com.kamsarobar.profile.UserProfile;

public record ProfileResponse(String linkedinUrl, String currentCompany, String position,
                              Integer yearsOfExperience, String bio, boolean openToHelp,
                              List<String> referralCompanies, List<String> expertise, boolean completed) {

    public static ProfileResponse empty() {
        return new ProfileResponse(null, null, null, null, null, true, List.of(), List.of(), false);
    }

    public static ProfileResponse from(UserProfile profile) {
        return new ProfileResponse(profile.getLinkedinUrl(), profile.getCurrentCompany(), profile.getPosition(),
                profile.getYearsOfExperience(), profile.getBio(), profile.isOpenToHelp(),
                profile.getReferralCompanies().stream().map(NamedTag::getName).sorted().toList(),
                profile.getSkills().stream().map(NamedTag::getName).sorted().toList(), true);
    }
}
