package de.mhus.nimbus.shared.client;

import de.mhus.nimbus.worldgenerator.dto.AddPhaseRequest;
import de.mhus.nimbus.worldgenerator.dto.CreateWorldGeneratorRequest;
import de.mhus.nimbus.worldgenerator.entity.WorldGenerator;
import de.mhus.nimbus.worldgenerator.entity.WorldGeneratorPhase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class GeneratorServiceClient {

    private final RestTemplate restTemplate;

    @Value("${nimbus.generator.service.url:http://localhost:8083}")
    private String generatorServiceUrl;

    @Value("${nimbus.generator.shared-secret}")
    private String sharedSecret;

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(sharedSecret);
        return headers;
    }

    public Optional<WorldGenerator> createWorldGenerator(String name, String description,
                                                        java.util.Map<String, Object> parameters) {
        try {
            CreateWorldGeneratorRequest request = CreateWorldGeneratorRequest.builder()
                    .name(name)
                    .description(description)
                    .parameters(parameters)
                    .build();

            HttpEntity<CreateWorldGeneratorRequest> entity = new HttpEntity<>(request, createHeaders());

            ResponseEntity<WorldGenerator> response = restTemplate.exchange(
                    generatorServiceUrl + "/api/generator/create",
                    HttpMethod.POST,
                    entity,
                    WorldGenerator.class
            );

            return Optional.ofNullable(response.getBody());
        } catch (Exception e) {
            log.error("Error creating world generator: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    public Optional<WorldGeneratorPhase> addPhase(Long worldGeneratorId, String processor, String name,
                                                 String description, Integer phaseOrder,
                                                 java.util.Map<String, Object> parameters) {
        try {
            AddPhaseRequest request = AddPhaseRequest.builder()
                    .processor(processor)
                    .name(name)
                    .description(description)
                    .phaseOrder(phaseOrder)
                    .parameters(parameters)
                    .build();

            HttpEntity<AddPhaseRequest> entity = new HttpEntity<>(request, createHeaders());

            ResponseEntity<WorldGeneratorPhase> response = restTemplate.exchange(
                    generatorServiceUrl + "/api/generator/" + worldGeneratorId + "/phases",
                    HttpMethod.POST,
                    entity,
                    WorldGeneratorPhase.class
            );

            return Optional.ofNullable(response.getBody());
        } catch (Exception e) {
            log.error("Error adding phase: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    public boolean startGeneration(Long worldGeneratorId) {
        try {
            HttpEntity<?> entity = new HttpEntity<>(createHeaders());

            ResponseEntity<Void> response = restTemplate.exchange(
                    generatorServiceUrl + "/api/generator/" + worldGeneratorId + "/start",
                    HttpMethod.POST,
                    entity,
                    Void.class
            );

            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("Error starting generation: {}", e.getMessage(), e);
            return false;
        }
    }

    public List<WorldGenerator> getAllWorldGenerators() {
        try {
            HttpEntity<?> entity = new HttpEntity<>(createHeaders());

            ResponseEntity<List<WorldGenerator>> response = restTemplate.exchange(
                    generatorServiceUrl + "/api/generator",
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<WorldGenerator>>() {}
            );

            return response.getBody();
        } catch (Exception e) {
            log.error("Error getting all world generators: {}", e.getMessage(), e);
            return List.of();
        }
    }

    public Optional<WorldGenerator> getWorldGenerator(Long id) {
        try {
            HttpEntity<?> entity = new HttpEntity<>(createHeaders());

            ResponseEntity<WorldGenerator> response = restTemplate.exchange(
                    generatorServiceUrl + "/api/generator/" + id,
                    HttpMethod.GET,
                    entity,
                    WorldGenerator.class
            );

            return Optional.ofNullable(response.getBody());
        } catch (Exception e) {
            log.error("Error getting world generator: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    public Optional<WorldGenerator> getWorldGeneratorByName(String name) {
        try {
            HttpEntity<?> entity = new HttpEntity<>(createHeaders());

            ResponseEntity<WorldGenerator> response = restTemplate.exchange(
                    generatorServiceUrl + "/api/generator/by-name/" + name,
                    HttpMethod.GET,
                    entity,
                    WorldGenerator.class
            );

            return Optional.ofNullable(response.getBody());
        } catch (Exception e) {
            log.error("Error getting world generator by name: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    public List<WorldGeneratorPhase> getPhases(Long worldGeneratorId) {
        try {
            HttpEntity<?> entity = new HttpEntity<>(createHeaders());

            ResponseEntity<List<WorldGeneratorPhase>> response = restTemplate.exchange(
                    generatorServiceUrl + "/api/generator/" + worldGeneratorId + "/phases",
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<WorldGeneratorPhase>>() {}
            );

            return response.getBody();
        } catch (Exception e) {
            log.error("Error getting phases: {}", e.getMessage(), e);
            return List.of();
        }
    }
}
