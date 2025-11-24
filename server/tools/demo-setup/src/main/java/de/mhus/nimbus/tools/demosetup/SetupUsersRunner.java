package de.mhus.nimbus.tools.demosetup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SetupUsersRunner {

    private static final Logger LOG = LoggerFactory.getLogger(SetupUsersRunner.class);

    private final AdminService adminService;
    private final UniverseClientService universeClientService;

    public SetupUsersRunner(AdminService adminService, UniverseClientService universeClientService) {
        this.adminService = adminService;
        this.universeClientService = universeClientService;
    }

    public void run() {
        if (!universeClientService.isConfigured()) {
            LOG.warn("Universe Service nicht konfiguriert - überspringe User Setup");
            return;
        }
        var pwOpt = adminService.getAdminPassword();
        if (pwOpt.isEmpty()) {
            LOG.warn("Kein Admin Passwort - kann Users nicht anlegen");
            return;
        }
        boolean loginOk = universeClientService.loginAdmin(pwOpt.get());
        if (!loginOk) {
            LOG.error("Admin Login fehlgeschlagen - breche User Setup ab");
            return;
        }
        LOG.info("Beginne Sicherstellung der User");
        List<UserDef> users = List.of(
                new UserDef("samanthaevelyncook", "password1"),
                new UserDef("wadewatts", "password1"),
                new UserDef("helenharris", "password1"),
                new UserDef("toshiroyoshiaki", "password1"),
                new UserDef("akihidekaratsu", "password1")
        );
        for (UserDef u : users) {
            universeClientService.ensureUser(u.username(), u.password());
        }
        LOG.info("User Setup abgeschlossen");
    }

    record UserDef(String username, String password) {}
}
