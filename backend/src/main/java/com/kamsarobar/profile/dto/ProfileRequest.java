package com.kamsarobar.profile.dto;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Form 2 - detailed professional profile. Every field is optional and can be edited any time. */
public record ProfileRequest(
        @Size(max = 300)
        @Pattern(regexp = "^$|^(https?://)?([a-z]{2,3}\\.)?linkedin\\.com/.+$",
                message = "Must be a LinkedIn profile URL, e.g. https://www.linkedin.com/in/your-name")
        String linkedinUrl,
        @Size(max = 150) String currentCompany,
        @Size(max = 150) String position,
        @Min(0) @Max(60) Integer yearsOfExperience,
        @Size(max = 1000) String bio,
        Boolean openToHelp,
        @Size(max = 20, message = "At most 20 companies") List<@Size(max = 150) String> referralCompanies,
        @Size(max = 30, message = "At most 30 areas of expertise") List<@Size(max = 100) String> expertise) {
}
