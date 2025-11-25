package de.mhus.nimbus.tools.demosetup;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
public abstract class BaseClientService implements ClientService {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    @Getter
    private final String baseUrl;
    protected final WebClient webClient;

    protected BaseClientService(String baseUrl) {
        this.baseUrl = baseUrl;
        this.webClient = WebClient.builder().baseUrl(this.baseUrl).build();
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
                        log.error("Fehler beim Health-Check {}: {}", path, e.toString());
                        return Mono.just(false);
                    })
                    .blockOptional()
                    .orElse(false);
        } catch (Exception e) {
            log.error("Exception beim Aufruf {}: {}", path, e.toString());
            return false;
        }
    }

    @Override
    public boolean check() {
        if (!isConfigured()) return false;
        return getHealth("/actuator/health");
    }
}

