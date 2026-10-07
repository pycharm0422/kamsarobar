package com.kamsarobar.admin;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kamsarobar.admin.dto.BlockRequest;
import com.kamsarobar.admin.dto.MemberDetails;
import com.kamsarobar.common.web.PageResponse;
import com.kamsarobar.common.web.Pages;
import com.kamsarobar.security.UserPrincipal;
import com.kamsarobar.user.dto.UserResponse;

import jakarta.validation.Valid;

/** Member management for the main admin (all cities) and city admins (their own city). */
@RestController
@RequestMapping("/api/admin/members")
@PreAuthorize("hasAnyRole('MAIN_ADMIN', 'CITY_ADMIN')")
public class MemberAdminController {

    private final MemberAdminService memberAdminService;

    public MemberAdminController(MemberAdminService memberAdminService) {
        this.memberAdminService = memberAdminService;
    }

    @GetMapping
    public PageResponse<UserResponse> list(@RequestParam(required = false) String q,
                                           @RequestParam(required = false) Long cityId,
                                           @RequestParam(required = false) MemberStatus status,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size,
                                           @AuthenticationPrincipal UserPrincipal actor) {
        return memberAdminService.list(actor, q, cityId, status, Pages.of(page, size));
    }

    @GetMapping("/{id}")
    public MemberDetails details(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal actor) {
        return memberAdminService.details(id, actor);
    }

    @PostMapping("/{id}/block")
    public MemberDetails block(@PathVariable Long id, @Valid @RequestBody BlockRequest request,
                               @AuthenticationPrincipal UserPrincipal actor) {
        return memberAdminService.block(id, request.reason(), actor);
    }

    @DeleteMapping("/{id}/block")
    public MemberDetails unblock(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal actor) {
        return memberAdminService.unblock(id, actor);
    }
}
