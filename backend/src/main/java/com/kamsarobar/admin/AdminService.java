package com.kamsarobar.admin;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kamsarobar.admin.dto.AdminStats;
import com.kamsarobar.city.City;
import com.kamsarobar.city.CityRepository;
import com.kamsarobar.city.CityService;
import com.kamsarobar.common.exception.BadRequestException;
import com.kamsarobar.common.exception.CityHasAdminException;
import com.kamsarobar.common.util.TextNormalizer;
import com.kamsarobar.common.web.PageResponse;
import com.kamsarobar.donation.DonationRepository;
import com.kamsarobar.post.PostRepository;
import com.kamsarobar.user.Role;
import com.kamsarobar.user.User;
import com.kamsarobar.user.UserRepository;
import com.kamsarobar.user.UserService;
import com.kamsarobar.user.dto.UserResponse;

/**
 * Operations reserved for the main admin: appointing city heads and overseeing members.
 */
@Service
@Transactional(readOnly = true)
public class AdminService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final CityService cityService;
    private final CityRepository cityRepository;
    private final PostRepository postRepository;
    private final DonationRepository donationRepository;

    public AdminService(UserRepository userRepository, UserService userService, CityService cityService,
                        CityRepository cityRepository, PostRepository postRepository,
                        DonationRepository donationRepository) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.cityService = cityService;
        this.cityRepository = cityRepository;
        this.postRepository = postRepository;
        this.donationRepository = donationRepository;
    }

    public AdminStats stats() {
        return new AdminStats(userRepository.count(), cityRepository.count(), postRepository.count(),
                userRepository.findAllByRoleOrderByNameAsc(Role.CITY_ADMIN).size(),
                donationRepository.totalVerified());
    }

    public PageResponse<UserResponse> searchUsers(String query, Long cityId, Pageable pageable) {
        String key = TextNormalizer.key(query);
        String pattern = key == null ? null : TextNormalizer.likeContains(key);
        return PageResponse.of(userRepository.search(cityId, null, pattern, pageable), UserResponse::from);
    }

    public List<UserResponse> cityAdmins() {
        return userRepository.findAllByRoleOrderByNameAsc(Role.CITY_ADMIN).stream().map(UserResponse::from).toList();
    }

    /**
     * Makes the member the admin of the city. A city has one admin: if it already has one, this fails with
     * CITY_HAS_ADMIN unless replaceExisting is true, in which case the current admin becomes a regular member.
     */
    @Transactional
    public UserResponse assignCityAdmin(Long userId, Long cityId, boolean replaceExisting) {
        User user = userService.getEntity(userId);
        if (user.getRole() == Role.MAIN_ADMIN) {
            throw new BadRequestException("The main admin already manages every city");
        }
        City city = cityService.getActiveEntity(cityId);
        makeSoleAdmin(user, city, replaceExisting);
        return UserResponse.from(user);
    }

    /**
     * Moves a member to another city (only the main admin may do this for city admins). A city admin who is moved
     * becomes the admin of the new city - subject to the same one-admin-per-city rule - and their old city is
     * left without an admin until the main admin appoints one.
     */
    @Transactional
    public UserResponse changeMemberCity(Long userId, Long cityId, boolean replaceExisting) {
        User user = userService.getEntity(userId);
        City city = cityService.getActiveEntity(cityId);
        if (user.getRole() == Role.CITY_ADMIN) {
            makeSoleAdmin(user, city, replaceExisting);
        }
        user.setCity(city);
        return UserResponse.from(user);
    }

    private void makeSoleAdmin(User user, City city, boolean replaceExisting) {
        List<User> others = userRepository.findByRoleAndManagedCityId(Role.CITY_ADMIN, city.getId()).stream()
                .filter(other -> !other.getId().equals(user.getId()))
                .toList();
        if (!others.isEmpty()) {
            if (!replaceExisting) {
                throw new CityHasAdminException(city.getName(),
                        String.join(", ", others.stream().map(User::getName).toList()));
            }
            others.forEach(User::revokeCityAdmin);
        }
        user.makeCityAdmin(city);
    }

    @Transactional
    public UserResponse revokeCityAdmin(Long userId) {
        User user = userService.getEntity(userId);
        if (user.getRole() != Role.CITY_ADMIN) {
            throw new BadRequestException("This member is not a city admin");
        }
        user.revokeCityAdmin();
        return UserResponse.from(user);
    }
}
