package de.mhus.nimbus.tools.demosetup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

public abstract class BaseClientService implements ClientService {

    protected final Logger LOG = LoggerFactory.getLogger(getClass());
    private final String baseUrl;
    protected final WebClient webClient;

    protected BaseClientService(String baseUrl) {
        this.baseUrl = baseUrl != null && !baseUrl.isBlank() ? baseUrl.trim() : null;
        this.webClient = WebClient.builder().baseUrl(this.baseUrl == null ? "http://localhost" : this.baseUrl).build();
    }

    @Override
    public boolean isConfigured() {
        return baseUrl != null;
    }

    protected boolean getHealth(String path) {
        if (!isConfigured()) return false;
        try {
            return webClient.get()
                    .uri(path)
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(Duration.ofSeconds(5))
                    .map(r -> r.getStatusCode().is2xxSuccessful())
                    .onErrorResume(e -> {
                        LOG.error("Fehler beim Health-Check {}: {}", path, e.toString());
                        return Mono.just(false);
                    })
                    .blockOptional()
                    .orElse(false);
        } catch (Exception e) {
            LOG.error("Exception beim Aufruf {}: {}", path, e.toString());
            return false;
        }
    }

    @Override
    public boolean check() {
        // Default: versuche /actuator/health dann /health dann Root
        if (!isConfigured()) return false;
        if (getHealth("/actuator/health")) return true;
        if (getHealth("/health")) return true;
        return getHealth("/");
    }
}

