package com.kamsarobar.donation;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kamsarobar.common.web.PageResponse;
import com.kamsarobar.common.web.Pages;
import com.kamsarobar.donation.dto.CampaignRequest;
import com.kamsarobar.donation.dto.CampaignResponse;
import com.kamsarobar.donation.dto.DonationRequest;
import com.kamsarobar.donation.dto.DonationResponse;
import com.kamsarobar.donation.dto.DonationReviewRequest;
import com.kamsarobar.donation.dto.DonationSummaryResponse;
import com.kamsarobar.donation.dto.SupporterResponse;
import com.kamsarobar.security.UserPrincipal;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class DonationController {

    private final DonationService donationService;
    private final CampaignService campaignService;

    public DonationController(DonationService donationService, CampaignService campaignService) {
        this.donationService = donationService;
        this.campaignService = campaignService;
    }

    /** Public: total collected per city. */
    @GetMapping("/donations/summary")
    public DonationSummaryResponse summary() {
        return donationService.summary();
    }

    @PostMapping("/donations")
    @ResponseStatus(HttpStatus.CREATED)
    public DonationResponse record(@Valid @RequestBody DonationRequest request,
                                   @AuthenticationPrincipal UserPrincipal actor) {
        return donationService.record(request, actor);
    }

    @GetMapping("/donations/mine")
    public PageResponse<DonationResponse> mine(@RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size,
                                               @AuthenticationPrincipal UserPrincipal actor) {
        return donationService.myDonations(actor, Pages.of(page, size));
    }

    @GetMapping("/cities/{cityId}/supporters")
    public PageResponse<SupporterResponse> supporters(@PathVariable Long cityId,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        return donationService.supporters(cityId, Pages.of(page, size));
    }

    /** City admin: every donation recorded for the city, optionally filtered by status. */
    @GetMapping("/cities/{cityId}/donations")
    public PageResponse<DonationResponse> forCity(@PathVariable Long cityId,
                                                  @RequestParam(required = false) DonationStatus status,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size,
                                                  @AuthenticationPrincipal UserPrincipal actor) {
        return donationService.forCity(cityId, status, Pages.of(page, size), actor);
    }

    @PatchMapping("/donations/{id}/status")
    public DonationResponse review(@PathVariable Long id, @Valid @RequestBody DonationReviewRequest request,
                                   @AuthenticationPrincipal UserPrincipal actor) {
        return donationService.review(id, request.status(), actor);
    }

    // --- Campaigns ---

    @GetMapping("/cities/{cityId}/campaigns")
    public List<CampaignResponse> campaigns(@PathVariable Long cityId,
                                            @RequestParam(defaultValue = "true") boolean activeOnly) {
        return campaignService.forCity(cityId, activeOnly);
    }

    @PostMapping("/cities/{cityId}/campaigns")
    @ResponseStatus(HttpStatus.CREATED)
    public CampaignResponse createCampaign(@PathVariable Long cityId, @Valid @RequestBody CampaignRequest request,
                                           @AuthenticationPrincipal UserPrincipal actor) {
        return campaignService.create(cityId, request, actor);
    }

    @PutMapping("/campaigns/{id}")
    public CampaignResponse updateCampaign(@PathVariable Long id, @Valid @RequestBody CampaignRequest request,
                                           @AuthenticationPrincipal UserPrincipal actor) {
        return campaignService.update(id, request, actor);
    }

    @DeleteMapping("/campaigns/{id}")
    public ResponseEntity<Void> deleteCampaign(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal actor) {
        campaignService.delete(id, actor);
        return ResponseEntity.noContent().build();
    }
}
