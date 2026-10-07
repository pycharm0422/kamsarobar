package com.kamsarobar.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kamsarobar.auth.dto.AuthResponse;
import com.kamsarobar.auth.dto.LoginRequest;
import com.kamsarobar.auth.dto.RegisterRequest;
import com.kamsarobar.city.City;
import com.kamsarobar.city.CityService;
import com.kamsarobar.common.exception.ConflictException;
import com.kamsarobar.common.exception.ForbiddenException;
import com.kamsarobar.common.exception.UnauthorizedException;
import com.kamsarobar.common.util.PhoneNumberNormalizer;
import com.kamsarobar.common.util.TextNormalizer;
import com.kamsarobar.security.TokenService;
import com.kamsarobar.security.UserPrincipal;
import com.kamsarobar.user.User;
import com.kamsarobar.user.UserRepository;
import com.kamsarobar.user.dto.UserResponse;

@Service
public class AuthService {

    static final String BLOCKED_MESSAGE = "Your account has been blocked. Please contact your city admin.";

    private final UserRepository userRepository;
    private final CityService cityService;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final PhoneNumberNormalizer phoneNormalizer;

    public AuthService(UserRepository userRepository, CityService cityService, PasswordEncoder passwordEncoder,
                       TokenService tokenService, PhoneNumberNormalizer phoneNormalizer) {
        this.userRepository = userRepository;
        this.cityService = cityService;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.phoneNormalizer = phoneNormalizer;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String mobile = phoneNormalizer.normalize(request.mobile());
        userRepository.findByMobile(mobile).ifPresent(existing -> {
            throw new ConflictException(existing.isBlocked() ? BLOCKED_MESSAGE
                    : "This mobile number is already registered. Please log in.");
        });
        City city = cityService.getActiveEntity(request.cityId());
        User user = userRepository.save(new User(TextNormalizer.clean(request.name()), mobile,
                passwordEncoder.encode(request.password()), city));
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String mobile;
        try {
            mobile = phoneNormalizer.normalize(request.mobile());
        } catch (RuntimeException ex) {
            throw new UnauthorizedException("Invalid mobile number or password");
        }
        User user = userRepository.findByMobile(mobile)
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new UnauthorizedException("Invalid mobile number or password"));
        if (user.isBlocked()) {
            throw new ForbiddenException(BLOCKED_MESSAGE);
        }
        return toResponse(user);
    }

    private AuthResponse toResponse(User user) {
        return new AuthResponse(tokenService.issueToken(UserPrincipal.from(user)), UserResponse.from(user));
    }
}
