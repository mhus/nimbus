package de.mhus.nimbus.identity.config;

import de.mhus.nimbus.identity.service.IdentityService;
import de.mhus.nimbus.server.shared.dto.CreateUserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Component that initializes the default admin user on application startup.
 * Creates an admin user with credentials 'admin/admin' and ADMIN role if it doesn't exist.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminUserInitializer {

    private final IdentityService identityService;

    /**
     * Initializes the admin user when the application is ready.
     * This method is called after the application context is fully loaded.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeAdminUser() {
        log.info("Checking for admin user existence...");

        try {
            // Check if admin user already exists
            identityService.getUser("admin");
            log.info("Admin user already exists, skipping initialization");
        } catch (IllegalArgumentException e) {
            // Admin user doesn't exist, create it
            log.info("Admin user not found, creating default admin user...");

            CreateUserDto adminUser = CreateUserDto.builder()
                    .id("admin")
                    .name("Administrator")
                    .nickname("Admin")
                    .email("admin@nimbus.local")
                    .password("admin")
                    .roles(List.of("ADMIN"))
                    .build();

            try {
                identityService.createUser(adminUser);
                log.info("Successfully created default admin user with ID 'admin' and password 'admin'");
                log.warn("SECURITY WARNING: Please change the default admin password after first login!");
            } catch (Exception ex) {
                log.error("Failed to create default admin user: {}", ex.getMessage(), ex);
            }
        } catch (Exception e) {
            log.error("Error during admin user initialization: {}", e.getMessage(), e);
        }
    }
}
