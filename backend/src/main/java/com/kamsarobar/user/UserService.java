package com.kamsarobar.user;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kamsarobar.city.City;
import com.kamsarobar.city.CityService;
import com.kamsarobar.common.exception.BadRequestException;
import com.kamsarobar.common.exception.ConflictException;
import com.kamsarobar.common.exception.ForbiddenException;
import com.kamsarobar.common.exception.ResourceNotFoundException;
import com.kamsarobar.common.util.PhoneNumberNormalizer;
import com.kamsarobar.common.util.TextNormalizer;
import com.kamsarobar.user.dto.ChangePasswordRequest;
import com.kamsarobar.user.dto.UpdateUserRequest;
import com.kamsarobar.user.dto.UserResponse;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final CityService cityService;
    private final PhoneNumberNormalizer phoneNormalizer;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher events;

    public UserService(UserRepository userRepository, CityService cityService,
                       PhoneNumberNormalizer phoneNormalizer, PasswordEncoder passwordEncoder,
                       ApplicationEventPublisher events) {
        this.userRepository = userRepository;
        this.cityService = cityService;
        this.phoneNormalizer = phoneNormalizer;
        this.passwordEncoder = passwordEncoder;
        this.events = events;
    }

    public User getEntity(Long id) {
        return userRepository.findWithCitiesById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    public UserResponse get(Long id) {
        return UserResponse.from(getEntity(id));
    }

    @Transactional
    public UserResponse updateBasicInfo(Long userId, UpdateUserRequest request) {
        User user = getEntity(userId);
        String mobile = phoneNormalizer.normalize(request.mobile());
        if (!mobile.equals(user.getMobile()) && userRepository.existsByMobile(mobile)) {
            throw new ConflictException("This mobile number is already registered");
        }
        City city = cityService.getActiveEntity(request.cityId());
        if (!city.getId().equals(user.getCity().getId()) && user.getRole() == Role.CITY_ADMIN) {
            throw new ForbiddenException("You are the admin of " + user.getManagedCity().getName()
                    + ", so only the main admin can change your city.");
        }
        user.setName(TextNormalizer.clean(request.name()));
        user.setMobile(mobile);
        moveToCity(user, city);
        return UserResponse.from(user);
    }

    /** The one place a member's home city changes. Must run inside the caller's transaction. */
    public void moveToCity(User user, City city) {
        Long from = user.getCity().getId();
        if (from.equals(city.getId())) {
            return;
        }
        user.setCity(city);
        events.publishEvent(new MemberMovedEvent(user.getId(), from, city.getId()));
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = getEntity(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    }
}
