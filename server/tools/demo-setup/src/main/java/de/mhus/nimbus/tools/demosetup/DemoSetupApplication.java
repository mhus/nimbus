package de.mhus.nimbus.tools.demosetup;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.List;

@SpringBootApplication
@RequiredArgsConstructor
public class DemoSetupApplication implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DemoSetupApplication.class);
    private final List<ClientService> services; // wird via Spring injiziert
    private final AdminService adminService; // neu
    private final SetupUsersRunner setupUsersRunner; // neu für User Anlage
    private final RegionSetupRunner regionSetupRunner; // neu
    private final RegionServerPublicKeyService regionServerPublicKeyService; // neu

    public static void main(String[] args) {
        SpringApplication.run(DemoSetupApplication.class, args);
    }

    @Override
    public void run(String... args) {

        int failures = checkServiceHealth();
        if (failures > 0) {
            LOG.error("Konnte nicht starten, weil Services nicht erreichbar sind.");
            System.exit(1);
        }

        LOG.info("Starte Demo-Setup CLI Tool - initialisiere Services");
        // Admin Passwort zuerst laden
        adminService.getAdminPassword().ifPresentOrElse(
                pw -> LOG.info("Admin-Passwort erfolgreich geladen."),
                () -> LOG.warn("Kein Admin-Passwort geladen.")
        );




        // Nutzer sicherstellen (nur wenn Universe konfiguriert)
        setupUsersRunner.run();
        regionSetupRunner.run();

        regionServerPublicKeyService.getPublicKeyBase64().ifPresentOrElse(
                pk -> LOG.info("RegionServer Public Key geladen ({} Zeichen, Anfang='{}...')", pk.length(), pk.substring(0, Math.min(16, pk.length()))),
                () -> LOG.warn("RegionServer Public Key nicht gefunden")
        );
        // TODO

        failures = checkServiceHealth();
        int exitCode = (failures == 0) ? 0 : 1;
        LOG.info("Beende Anwendung mit Exit-Code {}", exitCode);
        System.exit(exitCode);
    }

    private int checkServiceHealth() {
        int failures = 0;
        for (ClientService service : services) {
            String name = service.getName();
            if (!service.isConfigured()) {
                LOG.warn("Service '{}' nicht konfiguriert - überspringe", name);
                continue;
            }
            LOG.info("Prüfe Service '{}' ...", name);
            boolean ok = service.check();
            if (ok) {
                LOG.info("Service '{}' OK", name);
            } else {
                LOG.error("Service '{}' FEHLER", name);
                failures++;
            }
        }
        if (failures == 0) {
            LOG.info("Alle konfigurierten Services erfolgreich geprüft.");
        } else {
            LOG.error("{} Service(s) mit Fehlern.", failures);
        }
        return failures;
    }
}
