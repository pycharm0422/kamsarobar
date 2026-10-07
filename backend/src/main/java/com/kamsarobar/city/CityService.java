package com.kamsarobar.city;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kamsarobar.access.AccessPolicy;
import com.kamsarobar.city.dto.CityDetails;
import com.kamsarobar.city.dto.CityRequest;
import com.kamsarobar.city.dto.CitySettingsRequest;
import com.kamsarobar.city.dto.CitySummary;
import com.kamsarobar.common.exception.ConflictException;
import com.kamsarobar.common.exception.ResourceNotFoundException;
import com.kamsarobar.common.util.TextNormalizer;
import com.kamsarobar.security.UserPrincipal;

@Service
@Transactional(readOnly = true)
public class CityService {

    public static final String CACHE = "cities";

    private final CityRepository cityRepository;
    private final AccessPolicy accessPolicy;

    public CityService(CityRepository cityRepository, AccessPolicy accessPolicy) {
        this.cityRepository = cityRepository;
        this.accessPolicy = accessPolicy;
    }

    @Cacheable(cacheNames = CACHE, key = "'active'")
    public List<CitySummary> listActive() {
        return cityRepository.findAllByActiveTrueOrderByNameAsc().stream().map(CitySummary::from).toList();
    }

    public List<CitySummary> listAll() {
        return cityRepository.findAllByOrderByNameAsc().stream().map(CitySummary::from).toList();
    }

    public City getEntity(Long id) {
        return cityRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("City", id));
    }

    public City getActiveEntity(Long id) {
        City city = getEntity(id);
        if (!city.isActive()) {
            throw new ResourceNotFoundException("City", id);
        }
        return city;
    }

    public CityDetails getDetails(Long id) {
        return CityDetails.from(getEntity(id));
    }

    @Transactional
    @CacheEvict(cacheNames = CACHE, allEntries = true)
    public CitySummary create(CityRequest request) {
        String name = TextNormalizer.clean(request.name());
        if (cityRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("City already exists: " + name);
        }
        City city = new City(name, TextNormalizer.clean(request.state()));
        if (request.active() != null) {
            city.setActive(request.active());
        }
        return CitySummary.from(cityRepository.save(city));
    }

    @Transactional
    @CacheEvict(cacheNames = CACHE, allEntries = true)
    public CitySummary update(Long id, CityRequest request) {
        City city = getEntity(id);
        String name = TextNormalizer.clean(request.name());
        cityRepository.findByNameIgnoreCase(name)
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new ConflictException("City already exists: " + name);
                });
        city.setName(name);
        city.setState(TextNormalizer.clean(request.state()));
        if (request.active() != null) {
            city.setActive(request.active());
        }
        return CitySummary.from(city);
    }

    @Transactional
    public CityDetails updateSettings(Long id, CitySettingsRequest request, UserPrincipal actor) {
        accessPolicy.requireCityManager(actor, id);
        City city = getEntity(id);
        city.setWhatsappGroupUrl(TextNormalizer.clean(request.whatsappGroupUrl()));
        city.setBankAccountName(TextNormalizer.clean(request.bankAccountName()));
        city.setBankAccountNumber(TextNormalizer.clean(request.bankAccountNumber()));
        String ifsc = TextNormalizer.clean(request.bankIfsc());
        city.setBankIfsc(ifsc == null ? null : ifsc.toUpperCase());
        city.setBankName(TextNormalizer.clean(request.bankName()));
        city.setUpiId(TextNormalizer.clean(request.upiId()));
        return CityDetails.from(city);
    }
}
