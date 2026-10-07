package com.kamsarobar.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.kamsarobar.city.City;
import com.kamsarobar.city.CityRepository;
import com.kamsarobar.common.util.PhoneNumberNormalizer;
import com.kamsarobar.user.Role;
import com.kamsarobar.user.User;
import com.kamsarobar.user.UserRepository;

/**
 * Creates the very first main admin on an empty database, using the app.bootstrap-admin.* settings.
 */
@Component
public class BootstrapAdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminInitializer.class);

    private final UserRepository userRepository;
    private final CityRepository cityRepository;
    private final PasswordEncoder passwordEncoder;
    private final PhoneNumberNormalizer phoneNormalizer;
    private final AppProperties properties;

    public BootstrapAdminInitializer(UserRepository userRepository, CityRepository cityRepository,
                                     PasswordEncoder passwordEncoder, PhoneNumberNormalizer phoneNormalizer,
                                     AppProperties properties) {
        this.userRepository = userRepository;
        this.cityRepository = cityRepository;
        this.passwordEncoder = passwordEncoder;
        this.phoneNormalizer = phoneNormalizer;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRole(Role.MAIN_ADMIN)) {
            return;
        }
        AppProperties.BootstrapAdmin admin = properties.bootstrapAdmin();
        String mobile = phoneNormalizer.normalize(admin.mobile());
        City city = cityRepository.findByNameIgnoreCase(admin.city())
                .orElseGet(() -> cityRepository.save(new City(admin.city(), null)));

        User user = userRepository.findByMobile(mobile)
                .orElseGet(() -> new User(admin.name(), mobile, passwordEncoder.encode(admin.password()), city));
        user.setRole(Role.MAIN_ADMIN);
        userRepository.save(user);
        log.info("Bootstrap main admin ready (mobile {}). Change the password after first login.", mobile);
    }
}
