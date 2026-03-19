package com.busgallery.busgallery.job;

import com.busgallery.busgallery.auth.UserRole;
import com.busgallery.busgallery.entity.User;
import com.busgallery.busgallery.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "busgallery.bootstrap.station.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class StationAccountBootstrapRunner implements CommandLineRunner {

    private static final String DEFAULT_STATION_USERNAME = "admin";
    private static final String DEFAULT_STATION_PASSWORD = "12345678";
    private static final String DEFAULT_STATION_DISPLAY_NAME = "站长";

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        User admin = userService.findByUsername(DEFAULT_STATION_USERNAME);
        if (admin == null) {
            User created = new User();
            created.setUsername(DEFAULT_STATION_USERNAME);
            created.setDisplayName(DEFAULT_STATION_DISPLAY_NAME);
            created.setPasswordHash(passwordEncoder.encode(DEFAULT_STATION_PASSWORD));
            created.setRole(UserRole.STATION);
            userService.save(created);
            log.info("Default station account created: username={}", DEFAULT_STATION_USERNAME);
            return;
        }

        boolean changed = false;
        if (admin.getRole() != UserRole.STATION) {
            admin.setRole(UserRole.STATION);
            admin.setReviewRegionId(null);
            changed = true;
        }
        if (!passwordEncoder.matches(DEFAULT_STATION_PASSWORD, admin.getPasswordHash())) {
            admin.setPasswordHash(passwordEncoder.encode(DEFAULT_STATION_PASSWORD));
            changed = true;
        }
        if (changed) {
            userService.save(admin);
            log.info("Default station account normalized: username={}", DEFAULT_STATION_USERNAME);
        }
    }
}
