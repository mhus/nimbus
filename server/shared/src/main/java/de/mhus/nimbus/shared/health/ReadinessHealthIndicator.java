package de.mhus.nimbus.shared.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ReadinessHealthIndicator signalisiert, ob die Anwendung bereit ist Anfragen zu bedienen.
 * Bedingungen:
 *  - Wird erst nach vollständigem Bootstrapping (ContextRefreshedEvent) auf READY gesetzt.
 *  - Wechselt sofort auf NOT_READY sobald der Shutdown beginnt (ContextClosedEvent).
 */
@Slf4j
@Component("startupReadiness") // Bean-Name für /actuator/health/readiness
public class ReadinessHealthIndicator implements HealthIndicator, ApplicationListener<ApplicationEvent> {

    private final AtomicBoolean ready = new AtomicBoolean(false);

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextRefreshedEvent) {
            ready.set(true);
            log.info("Readiness set to READY (context refreshed)");
        } else if (event instanceof ContextClosedEvent) {
            ready.set(false);
            log.info("Readiness set to NOT_READY (context closing)");
        }
    }

    @Override
    public Health health() {
        if (ready.get()) {
            return Health.up().withDetail("readiness", "READY").build();
        }
        return Health.down().withDetail("readiness", "NOT_READY").build();
    }
}
