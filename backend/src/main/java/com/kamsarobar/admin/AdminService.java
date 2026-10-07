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
        return PageResponse.of(userRepository.search(cityId, pattern, pageable), UserResponse::from);
    }

    public List<UserResponse> cityAdmins() {
        return userRepository.findAllByRoleOrderByNameAsc(Role.CITY_ADMIN).stream().map(UserResponse::from).toList();
    }

    @Transactional
    public UserResponse assignCityAdmin(Long userId, Long cityId) {
        User user = userService.getEntity(userId);
        if (user.getRole() == Role.MAIN_ADMIN) {
            throw new BadRequestException("The main admin already manages every city");
        }
        City city = cityService.getActiveEntity(cityId);
        user.makeCityAdmin(city);
        return UserResponse.from(user);
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
