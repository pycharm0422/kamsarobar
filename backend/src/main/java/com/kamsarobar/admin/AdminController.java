package com.kamsarobar.admin;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kamsarobar.admin.dto.AdminStats;
import com.kamsarobar.admin.dto.AssignCityAdminRequest;
import com.kamsarobar.city.CityService;
import com.kamsarobar.city.dto.CityRequest;
import com.kamsarobar.city.dto.CitySummary;
import com.kamsarobar.common.web.PageResponse;
import com.kamsarobar.common.web.Pages;
import com.kamsarobar.user.dto.UserResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('MAIN_ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final CityService cityService;

    public AdminController(AdminService adminService, CityService cityService) {
        this.adminService = adminService;
        this.cityService = cityService;
    }

    @GetMapping("/stats")
    public AdminStats stats() {
        return adminService.stats();
    }

    // --- Cities ---

    @GetMapping("/cities")
    public List<CitySummary> allCities() {
        return cityService.listAll();
    }

    @PostMapping("/cities")
    @ResponseStatus(HttpStatus.CREATED)
    public CitySummary createCity(@Valid @RequestBody CityRequest request) {
        return cityService.create(request);
    }

    @PutMapping("/cities/{id}")
    public CitySummary updateCity(@PathVariable Long id, @Valid @RequestBody CityRequest request) {
        return cityService.update(id, request);
    }

    // --- Members & city admins ---

    @GetMapping("/users")
    public PageResponse<UserResponse> users(@RequestParam(required = false) String q,
                                            @RequestParam(required = false) Long cityId,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        return adminService.searchUsers(q, cityId, Pages.of(page, size));
    }

    @GetMapping("/city-admins")
    public List<UserResponse> cityAdmins() {
        return adminService.cityAdmins();
    }

    @PostMapping("/city-admins")
    public UserResponse assignCityAdmin(@Valid @RequestBody AssignCityAdminRequest request) {
        return adminService.assignCityAdmin(request.userId(), request.cityId());
    }

    @DeleteMapping("/city-admins/{userId}")
    public UserResponse revokeCityAdmin(@PathVariable Long userId) {
        return adminService.revokeCityAdmin(userId);
    }
}
