package com.kamsarobar.donation;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kamsarobar.access.AccessPolicy;
import com.kamsarobar.city.City;
import com.kamsarobar.city.CityService;
import com.kamsarobar.common.exception.BadRequestException;
import com.kamsarobar.common.exception.ResourceNotFoundException;
import com.kamsarobar.common.util.TextNormalizer;
import com.kamsarobar.common.web.PageResponse;
import com.kamsarobar.donation.dto.CityDonationSummary;
import com.kamsarobar.donation.dto.DonationRequest;
import com.kamsarobar.donation.dto.DonationResponse;
import com.kamsarobar.donation.dto.DonationSummaryResponse;
import com.kamsarobar.donation.dto.SupporterResponse;
import com.kamsarobar.security.UserPrincipal;
import com.kamsarobar.user.UserService;

@Service
@Transactional(readOnly = true)
public class DonationService {

    private final DonationRepository donationRepository;
    private final CampaignRepository campaignRepository;
    private final CityService cityService;
    private final UserService userService;
    private final AccessPolicy accessPolicy;

    public DonationService(DonationRepository donationRepository, CampaignRepository campaignRepository,
                           CityService cityService, UserService userService, AccessPolicy accessPolicy) {
        this.donationRepository = donationRepository;
        this.campaignRepository = campaignRepository;
        this.cityService = cityService;
        this.userService = userService;
        this.accessPolicy = accessPolicy;
    }

    /** City-wise collected totals (verified donations only) - shown on the public community page. */
    public DonationSummaryResponse summary() {
        Map<Long, BigDecimal> collected = new HashMap<>();
        Map<Long, Long> counts = new HashMap<>();
        Map<Long, BigDecimal> pending = new HashMap<>();
        for (Object[] row : donationRepository.aggregateByCityAndStatus()) {
            Long cityId = (Long) row[0];
            DonationStatus status = (DonationStatus) row[1];
            BigDecimal sum = (BigDecimal) row[2];
            long count = ((Number) row[3]).longValue();
            if (status == DonationStatus.VERIFIED) {
                collected.put(cityId, sum);
                counts.put(cityId, count);
            } else if (status == DonationStatus.PENDING) {
                pending.put(cityId, sum);
            }
        }
        List<CityDonationSummary> cities = cityService.listActive().stream()
                .map(c -> new CityDonationSummary(c.id(), c.name(),
                        collected.getOrDefault(c.id(), BigDecimal.ZERO),
                        counts.getOrDefault(c.id(), 0L),
                        pending.getOrDefault(c.id(), BigDecimal.ZERO)))
                .sorted((a, b) -> b.collected().compareTo(a.collected()))
                .toList();
        BigDecimal total = cities.stream().map(CityDonationSummary::collected).reduce(BigDecimal.ZERO,
                BigDecimal::add);
        return new DonationSummaryResponse(total, cities);
    }

    /** Collected / pending amounts for one city - what a member sees by default (their own city). */
    public CityDonationSummary summaryForCity(Long cityId) {
        City city = cityService.getEntity(cityId);
        BigDecimal collected = BigDecimal.ZERO;
        BigDecimal pending = BigDecimal.ZERO;
        long count = 0;
        for (Object[] row : donationRepository.aggregateForCity(cityId)) {
            DonationStatus status = (DonationStatus) row[0];
            if (status == DonationStatus.VERIFIED) {
                collected = (BigDecimal) row[1];
                count = ((Number) row[2]).longValue();
            } else if (status == DonationStatus.PENDING) {
                pending = (BigDecimal) row[1];
            }
        }
        return new CityDonationSummary(city.getId(), city.getName(), collected, count, pending);
    }

    @Transactional
    public DonationResponse record(DonationRequest request, UserPrincipal actor) {
        City city = cityService.getActiveEntity(request.cityId());
        Campaign campaign = null;
        if (request.campaignId() != null) {
            campaign = campaignRepository.findById(request.campaignId())
                    .orElseThrow(() -> new ResourceNotFoundException("Campaign", request.campaignId()));
            if (!campaign.getCity().getId().equals(city.getId()) || !campaign.isActive()) {
                throw new BadRequestException("This campaign is not accepting donations for the selected city");
            }
        }
        Donation donation = new Donation(city, campaign, userService.getEntity(actor.id()), request.amount(),
                TextNormalizer.clean(request.transactionRef()), TextNormalizer.clean(request.note()),
                request.anonymous());
        return DonationResponse.from(donationRepository.save(donation));
    }

    public PageResponse<DonationResponse> myDonations(UserPrincipal actor, Pageable pageable) {
        return PageResponse.of(donationRepository.findByDonorIdOrderByCreatedAtDesc(actor.id(), pageable),
                DonationResponse::from);
    }

    /** Verified supporters of a city - visible to every member. */
    public PageResponse<SupporterResponse> supporters(Long cityId, Pageable pageable) {
        return PageResponse.of(donationRepository.findByCityIdAndStatusOrderByCreatedAtDesc(cityId,
                DonationStatus.VERIFIED, pageable), SupporterResponse::from);
    }

    /** Full donation list of a city, for its admin to reconcile with the bank statement. */
    public PageResponse<DonationResponse> forCity(Long cityId, DonationStatus status, Pageable pageable,
                                                  UserPrincipal actor) {
        accessPolicy.requireCityManager(actor, cityId);
        Page<Donation> page = status == null
                ? donationRepository.findByCityIdOrderByCreatedAtDesc(cityId, pageable)
                : donationRepository.findByCityIdAndStatusOrderByCreatedAtDesc(cityId, status, pageable);
        return PageResponse.of(page, DonationResponse::from);
    }

    @Transactional
    public DonationResponse review(Long donationId, DonationStatus status, UserPrincipal actor) {
        if (status == DonationStatus.PENDING) {
            throw new BadRequestException("A donation can only be marked VERIFIED or REJECTED");
        }
        Donation donation = donationRepository.findWithCityById(donationId)
                .orElseThrow(() -> new ResourceNotFoundException("Donation", donationId));
        accessPolicy.requireCityManager(actor, donation.getCity().getId());
        donation.review(status, userService.getEntity(actor.id()));
        return DonationResponse.from(donation);
    }
}
