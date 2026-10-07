package com.kamsarobar.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kamsarobar.city.CityService;
import com.kamsarobar.common.exception.BadRequestException;
import com.kamsarobar.common.exception.ConflictException;
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

    public UserService(UserRepository userRepository, CityService cityService,
                       PhoneNumberNormalizer phoneNormalizer, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.cityService = cityService;
        this.phoneNormalizer = phoneNormalizer;
        this.passwordEncoder = passwordEncoder;
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
        user.setName(TextNormalizer.clean(request.name()));
        user.setMobile(mobile);
        user.setCity(cityService.getActiveEntity(request.cityId()));
        return UserResponse.from(user);
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
