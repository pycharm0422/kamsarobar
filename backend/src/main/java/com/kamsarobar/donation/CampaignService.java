package com.kamsarobar.donation;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kamsarobar.access.AccessPolicy;
import com.kamsarobar.city.CityService;
import com.kamsarobar.common.exception.ResourceNotFoundException;
import com.kamsarobar.common.util.TextNormalizer;
import com.kamsarobar.donation.dto.CampaignRequest;
import com.kamsarobar.donation.dto.CampaignResponse;
import com.kamsarobar.security.UserPrincipal;
import com.kamsarobar.user.UserService;

@Service
@Transactional(readOnly = true)
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final DonationRepository donationRepository;
    private final CityService cityService;
    private final UserService userService;
    private final AccessPolicy accessPolicy;

    public CampaignService(CampaignRepository campaignRepository, DonationRepository donationRepository,
                           CityService cityService, UserService userService, AccessPolicy accessPolicy) {
        this.campaignRepository = campaignRepository;
        this.donationRepository = donationRepository;
        this.cityService = cityService;
        this.userService = userService;
        this.accessPolicy = accessPolicy;
    }

    public List<CampaignResponse> forCity(Long cityId, boolean activeOnly) {
        List<Campaign> campaigns = activeOnly
                ? campaignRepository.findByCityIdAndActiveTrueOrderByCreatedAtDesc(cityId)
                : campaignRepository.findByCityIdOrderByCreatedAtDesc(cityId);
        Map<Long, BigDecimal> raised = new HashMap<>();
        if (!campaigns.isEmpty()) {
            donationRepository.verifiedTotalsByCampaign(campaigns.stream().map(Campaign::getId).toList())
                    .forEach(row -> raised.put((Long) row[0], (BigDecimal) row[1]));
        }
        return campaigns.stream()
                .map(c -> CampaignResponse.from(c, raised.getOrDefault(c.getId(), BigDecimal.ZERO)))
                .toList();
    }

    @Transactional
    public CampaignResponse create(Long cityId, CampaignRequest request, UserPrincipal actor) {
        accessPolicy.requireCityManager(actor, cityId);
        Campaign campaign = new Campaign(cityService.getEntity(cityId), TextNormalizer.clean(request.title()),
                TextNormalizer.clean(request.description()), request.goalAmount(), userService.getEntity(actor.id()));
        return CampaignResponse.from(campaignRepository.save(campaign), BigDecimal.ZERO);
    }

    @Transactional
    public CampaignResponse update(Long id, CampaignRequest request, UserPrincipal actor) {
        Campaign campaign = getEntity(id);
        accessPolicy.requireCityManager(actor, campaign.getCity().getId());
        campaign.edit(TextNormalizer.clean(request.title()), TextNormalizer.clean(request.description()),
                request.goalAmount(), request.active() == null || request.active());
        BigDecimal raised = donationRepository.verifiedTotalsByCampaign(List.of(id)).stream()
                .map(row -> (BigDecimal) row[1]).findFirst().orElse(BigDecimal.ZERO);
        return CampaignResponse.from(campaign, raised);
    }

    @Transactional
    public void delete(Long id, UserPrincipal actor) {
        Campaign campaign = getEntity(id);
        accessPolicy.requireCityManager(actor, campaign.getCity().getId());
        campaignRepository.delete(campaign);
    }

    private Campaign getEntity(Long id) {
        return campaignRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Campaign", id));
    }
}
