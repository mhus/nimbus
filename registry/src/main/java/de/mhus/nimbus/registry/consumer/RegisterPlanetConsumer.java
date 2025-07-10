package de.mhus.nimbus.registry.consumer;

import de.mhus.nimbus.registry.service.PlanetRegistryService;
import de.mhus.nimbus.shared.avro.PlanetRegistrationRequest;
import de.mhus.nimbus.shared.avro.PlanetRegistrationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka-Consumer für Planet-Registrierungs-Anfragen
 * Delegiert die Business-Logik an den PlanetRegistryService
 */
@Component
@Slf4j
public class RegisterPlanetConsumer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final PlanetRegistryService planetRegistryService;

    public RegisterPlanetConsumer(KafkaTemplate<String, Object> kafkaTemplate,
                                PlanetRegistryService planetRegistryService) {
        this.kafkaTemplate = kafkaTemplate;
        this.planetRegistryService = planetRegistryService;
    }

    @KafkaListener(
        topics = "planet-registration",
        groupId = "nimbus-registry-group",
        containerFactory = "avroKafkaListenerContainerFactory"
    )
    public void consumePlanetRegistrationMessage(@Payload PlanetRegistrationRequest request,
                                               @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                               @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                               @Header(KafkaHeaders.OFFSET) long offset,
                                               Acknowledgment acknowledgment) {

        log.info("Received planet registration request from topic: '{}', partition: {}, offset: {}",
                   topic, partition, offset);

        try {
            // Validiere Request
            planetRegistryService.validateRegistrationRequest(request);

            log.info("Processing planet registration request: requestId={}, planet={}, environment={}, worlds={}, registeredBy={}",
                       request.getRequestId(), request.getPlanetName(), request.getEnvironment(),
                       request.getWorlds().size(), request.getRegisteredBy());

            // Delegiere an Service-Schicht
            PlanetRegistrationResponse response = planetRegistryService.processRegistrationRequest(request);

            // Sende die Antwort zurück
            sendPlanetRegistrationResponse(response);

            // Manual acknowledgment nach erfolgreicher Verarbeitung
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }

        } catch (IllegalArgumentException e) {
            log.warn("Invalid planet registration request: requestId={}, error={}",
                       request.getRequestId(), e.getMessage());
            sendErrorResponse(request, "Invalid request: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error processing planet registration message: requestId={}",
                        request.getRequestId(), e);
            sendErrorResponse(request, "Internal processing error");
        }
    }

    private void sendPlanetRegistrationResponse(PlanetRegistrationResponse response) {
        try {
            kafkaTemplate.send("planet-registration-response", response.getRequestId(), response);
            log.info("Sent planet registration response for requestId: {} with status: {}",
                       response.getRequestId(), response.getStatus());
        } catch (Exception e) {
            log.error("Error sending planet registration response for requestId: {}",
                        response.getRequestId(), e);
        }
    }

    private void sendErrorResponse(PlanetRegistrationRequest request, String errorMessage) {
        try {
            PlanetRegistrationResponse errorResponse = planetRegistryService.createRegistrationErrorResponse(request, errorMessage);
            sendPlanetRegistrationResponse(errorResponse);
        } catch (Exception e) {
            log.error("Error sending registration error response for requestId: {}",
                        request.getRequestId(), e);
        }
    }
}
