package com.kamsarobar.profile;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kamsarobar.profile.dto.ProfileRequest;
import com.kamsarobar.profile.dto.ProfileResponse;
import com.kamsarobar.security.UserPrincipal;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/profile/me")
    public ProfileResponse myProfile(@AuthenticationPrincipal UserPrincipal principal) {
        return profileService.getForUser(principal.id());
    }

    @PutMapping("/profile/me")
    public ProfileResponse saveMyProfile(@AuthenticationPrincipal UserPrincipal principal,
                                         @Valid @RequestBody ProfileRequest request) {
        return profileService.save(principal.id(), request);
    }

    /** Autocomplete for company names already known to the community. */
    @GetMapping("/suggestions/companies")
    public List<String> suggestCompanies(@RequestParam("q") String query) {
        return profileService.suggestCompanies(query);
    }

    @GetMapping("/suggestions/expertise")
    public List<String> suggestExpertise(@RequestParam("q") String query) {
        return profileService.suggestExpertise(query);
    }
}
