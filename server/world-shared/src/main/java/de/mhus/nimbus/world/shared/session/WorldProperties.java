package de.mhus.nimbus.world.shared.session;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Konfiguration der Session Lebensdauern.
 * Wird aus application.yaml mit Prefix 'world.session' geladen.
 */
@Component
@ConfigurationProperties(prefix = "world.session")
@Data
public class WorldProperties {

    /** Ablaufdauer für neue Sessions im Status WAITING (Minuten). */
    private long waitingMinutes = 5;
    /** Ablaufdauer nach Wechsel in Status RUNNING (Stunden). */
    private long runningHours = 12;
    /** Ablaufdauer nach Wechsel in Status DEPRECATED (Minuten). */
    private long deprecatedMinutes = 30;

    /** Anzahl Keys pro SCAN Happen beim Aufräumen. */
    private int cleanupScanCount = 500;
    /** Maximale Anzahl Löschungen pro Cleanup-Aufruf. */
    private int cleanupMaxDeletes = 1000;
    /** Cleanup aktivieren (wird nur genutzt wenn aufrufender Code Methode nutzt). */
    private boolean cleanupEnabled = true;
    /** Intervall für Scheduler (fixed delay). */
    private int cleanupIntervalSeconds = 60;
}
