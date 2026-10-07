package com.kamsarobar.city;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kamsarobar.city.dto.CityDetails;
import com.kamsarobar.city.dto.CitySettingsRequest;
import com.kamsarobar.city.dto.CitySummary;
import com.kamsarobar.security.UserPrincipal;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/cities")
public class CityController {

    private final CityService cityService;

    public CityController(CityService cityService) {
        this.cityService = cityService;
    }

    /** Public: used by the registration form. */
    @GetMapping
    public List<CitySummary> list() {
        return cityService.listActive();
    }

    @GetMapping("/{id}")
    public CityDetails get(@PathVariable Long id) {
        return cityService.getDetails(id);
    }

    /** City admins (for their own city) and the main admin update the WhatsApp group and bank details. */
    @PutMapping("/{id}/settings")
    public CityDetails updateSettings(@PathVariable Long id, @Valid @RequestBody CitySettingsRequest request,
                                      @AuthenticationPrincipal UserPrincipal actor) {
        return cityService.updateSettings(id, request, actor);
    }
}
