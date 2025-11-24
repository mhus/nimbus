package de.mhus.nimbus.tools.demosetup;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class SetupUsersRunner {

    private final AdminService adminService;
    private final UniverseClientService universeClientService;

    public SetupUsersRunner(AdminService adminService, UniverseClientService universeClientService) {
        this.adminService = adminService;
        this.universeClientService = universeClientService;
    }

    public void run() {
        if (!universeClientService.isConfigured()) {
            log.warn("Universe Service nicht konfiguriert - überspringe User Setup");
            return;
        }
        log.info("Login Admin");
        universeClientService.loginAdmin(adminService.getAdminPassword().get());
        if (!universeClientService.hasToken()) {
            log.warn("Kein Token - User Setup übersprungen");
            return;
        }
        log.info("Beginne Sicherstellung der User");
        List<UserDef> users = List.of(
                new UserDef("samanthaevelyncook", "password1"),
                new UserDef("wadewatts", "password1"),
                new UserDef("helenharris", "password1"),
                new UserDef("toshiroyoshiaki", "password1"),
                new UserDef("akihidekaratsu", "password1")
        );
        for (UserDef u : users) {
            universeClientService.ensureUser(u.username(), u.password());
            var token = universeClientService.login(u.username(), u.password());
            System.out.println(token);
            if (token.isEmpty()) {
                log.error("Konnte mich nicht mit User '{}' einloggen nach Anlage!", u.username());
            } else {
                log.info("User '{}' erfolgreich angelegt und eingeloggt.", u.username());
            }
        }
        log.info("User Setup abgeschlossen");
    }

    record UserDef(String username, String password) {}
}
