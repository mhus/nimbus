package de.mhus.nimbus.registry.consumer;

import de.mhus.nimbus.registry.service.PlanetRegistryService;
import de.mhus.nimbus.shared.avro.PlanetLookupRequest;
import de.mhus.nimbus.shared.avro.PlanetLookupResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka-Consumer für Planet-Lookup-Anfragen
 * Delegiert die Business-Logik an den PlanetRegistryService
 */
@Component
@Slf4j
public class LookupPlanetConsumer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final PlanetRegistryService planetRegistryService;

    public LookupPlanetConsumer(KafkaTemplate<String, Object> kafkaTemplate,
                               PlanetRegistryService planetRegistryService) {
        this.kafkaTemplate = kafkaTemplate;
        this.planetRegistryService = planetRegistryService;
    }

    @KafkaListener(
        topics = "planet-lookup",
        groupId = "nimbus-registry-group",
        containerFactory = "avroKafkaListenerContainerFactory"
    )
    public void consumePlanetLookupMessage(@Payload PlanetLookupRequest request,
                                         @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                         @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                         @Header(KafkaHeaders.OFFSET) long offset,
                                         Acknowledgment acknowledgment) {

        log.info("Received planet lookup request from topic: '{}', partition: {}, offset: {}",
                   topic, partition, offset);

        try {
            // Validiere Request
            planetRegistryService.validateRequest(request);

            log.info("Processing planet lookup request: requestId={}, planet={}, world={}, environment={}, requestedBy={}",
                       request.getRequestId(), request.getPlanetName(), request.getWorldName(),
                       request.getEnvironment(), request.getRequestedBy());

            // Delegiere an Service-Schicht
            PlanetLookupResponse response = planetRegistryService.processLookupRequest(request);

            // Sende die Antwort zurück
            sendPlanetLookupResponse(response);

            // Manual acknowledgment nach erfolgreicher Verarbeitung
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }

        } catch (IllegalArgumentException e) {
            log.warn("Invalid planet lookup request: requestId={}, error={}",
                       request.getRequestId(), e.getMessage());
            sendErrorResponse(request, "Invalid request: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error processing planet lookup message: requestId={}",
                        request.getRequestId(), e);
            sendErrorResponse(request, "Internal processing error");
        }
    }

    private void sendPlanetLookupResponse(PlanetLookupResponse response) {
        try {
            kafkaTemplate.send("planet-lookup-response", response.getRequestId(), response);
            log.info("Sent planet lookup response for requestId: {} with status: {}",
                       response.getRequestId(), response.getStatus());
        } catch (Exception e) {
            log.error("Error sending planet lookup response for requestId: {}",
                        response.getRequestId(), e);
        }
    }

    private void sendErrorResponse(PlanetLookupRequest request, String errorMessage) {
        try {
            PlanetLookupResponse errorResponse = planetRegistryService.createErrorResponse(request, errorMessage);
            sendPlanetLookupResponse(errorResponse);
        } catch (Exception e) {
            log.error("Error sending error response for requestId: {}",
                        request.getRequestId(), e);
        }
    }
}
