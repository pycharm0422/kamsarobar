package com.kamsarobar.directory;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kamsarobar.common.web.PageResponse;
import com.kamsarobar.common.web.Pages;
import com.kamsarobar.directory.dto.MemberCard;
import com.kamsarobar.security.UserPrincipal;

@RestController
@RequestMapping("/api/directory")
public class DirectoryController {

    private final DirectoryService directoryService;

    public DirectoryController(DirectoryService directoryService) {
        this.directoryService = directoryService;
    }

    /** "Who can refer me to <company>?" */
    @GetMapping("/referrers")
    public PageResponse<MemberCard> referrers(@RequestParam String company,
                                              @RequestParam(required = false) Long cityId,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size,
                                              @AuthenticationPrincipal UserPrincipal viewer) {
        return directoryService.search(SearchType.REFERRAL, company, cityId, viewer.id(), Pages.of(page, size));
    }

    /** "Who can advise me on <expertise>?" */
    @GetMapping("/experts")
    public PageResponse<MemberCard> experts(@RequestParam String expertise,
                                            @RequestParam(required = false) Long cityId,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size,
                                            @AuthenticationPrincipal UserPrincipal viewer) {
        return directoryService.search(SearchType.EXPERTISE, expertise, cityId, viewer.id(), Pages.of(page, size));
    }
}
