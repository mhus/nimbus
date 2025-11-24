package de.mhus.nimbus.shared.health;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;

import static org.junit.jupiter.api.Assertions.*;

class ReadinessHealthIndicatorTest {

    @Test
    void readinessTransitions() {
        var indicator = new ReadinessHealthIndicator();
        var ctx = new GenericApplicationContext();

        // Vor Refresh: NOT_READY
        assertEquals("DOWN", indicator.health().getStatus().getCode());

        // Refresh Event -> READY
        indicator.onApplicationEvent(new ContextRefreshedEvent(ctx));
        assertEquals("UP", indicator.health().getStatus().getCode());
        assertEquals("READY", indicator.health().getDetails().get("readiness"));

        // Closed Event -> NOT_READY
        indicator.onApplicationEvent(new ContextClosedEvent(ctx));
        assertEquals("DOWN", indicator.health().getStatus().getCode());
        assertEquals("NOT_READY", indicator.health().getDetails().get("readiness"));
    }
}

