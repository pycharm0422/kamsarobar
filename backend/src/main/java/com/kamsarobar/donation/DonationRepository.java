package com.kamsarobar.donation;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DonationRepository extends JpaRepository<Donation, Long> {

    @EntityGraph(attributePaths = {"city", "campaign"})
    Page<Donation> findByDonorIdOrderByCreatedAtDesc(Long donorId, Pageable pageable);

    @EntityGraph(attributePaths = {"city", "campaign"})
    Page<Donation> findByCityIdOrderByCreatedAtDesc(Long cityId, Pageable pageable);

    @EntityGraph(attributePaths = {"city", "campaign"})
    Page<Donation> findByCityIdAndStatusOrderByCreatedAtDesc(Long cityId, DonationStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"city", "campaign"})
    Optional<Donation> findWithCityById(Long id);

    /** Rows of [cityId, status, sum(amount), count] - aggregated in the database, not in memory. */
    @Query("select d.city.id, d.status, sum(d.amount), count(d) from Donation d group by d.city.id, d.status")
    List<Object[]> aggregateByCityAndStatus();

    /** Rows of [status, sum(amount), count] for one city. */
    @Query("select d.status, sum(d.amount), count(d) from Donation d where d.city.id = :cityId group by d.status")
    List<Object[]> aggregateForCity(@Param("cityId") Long cityId);

    /** Rows of [campaignId, sum(amount)] for verified donations. */
    @Query("""
            select d.campaign.id, sum(d.amount) from Donation d
            where d.status = com.kamsarobar.donation.DonationStatus.VERIFIED and d.campaign.id in :campaignIds
            group by d.campaign.id
            """)
    List<Object[]> verifiedTotalsByCampaign(@Param("campaignIds") Collection<Long> campaignIds);

    @Query("""
            select coalesce(sum(d.amount), 0) from Donation d
            where d.status = com.kamsarobar.donation.DonationStatus.VERIFIED
            """)
    BigDecimal totalVerified();
}
