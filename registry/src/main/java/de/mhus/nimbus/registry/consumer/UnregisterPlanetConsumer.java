package de.mhus.nimbus.registry.consumer;

import de.mhus.nimbus.registry.service.PlanetRegistryService;
import de.mhus.nimbus.shared.avro.PlanetUnregistrationRequest;
import de.mhus.nimbus.shared.avro.PlanetUnregistrationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka-Consumer für Planet-Deregistrierungs-Anfragen
 * Delegiert die Business-Logik an den PlanetRegistryService
 */
@Component
@Slf4j
public class UnregisterPlanetConsumer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final PlanetRegistryService planetRegistryService;

    public UnregisterPlanetConsumer(KafkaTemplate<String, Object> kafkaTemplate,
                                  PlanetRegistryService planetRegistryService) {
        this.kafkaTemplate = kafkaTemplate;
        this.planetRegistryService = planetRegistryService;
    }

    @KafkaListener(
        topics = "planet-unregistration",
        groupId = "nimbus-registry-group",
        containerFactory = "avroKafkaListenerContainerFactory"
    )
    public void consumePlanetUnregistrationMessage(@Payload PlanetUnregistrationRequest request,
                                                 @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                                 @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                                 @Header(KafkaHeaders.OFFSET) long offset,
                                                 Acknowledgment acknowledgment) {

        log.info("Received planet unregistration request from topic: '{}', partition: {}, offset: {}",
                   topic, partition, offset);

        try {
            // Validiere Request
            planetRegistryService.validateUnregistrationRequest(request);

            log.info("Processing planet unregistration request: requestId={}, planet={}, unregisteredBy={}",
                       request.getRequestId(), request.getPlanetName(), request.getUnregisteredBy());

            // Delegiere an Service-Schicht
            PlanetUnregistrationResponse response = planetRegistryService.processUnregistrationRequest(request);

            // Sende die Antwort zurück
            sendPlanetUnregistrationResponse(response);

            // Manual acknowledgment nach erfolgreicher Verarbeitung
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }

        } catch (IllegalArgumentException e) {
            log.warn("Invalid planet unregistration request: requestId={}, error={}",
                       request.getRequestId(), e.getMessage());
            sendErrorResponse(request, "Invalid request: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error processing planet unregistration message: requestId={}",
                        request.getRequestId(), e);
            sendErrorResponse(request, "Internal processing error");
        }
    }

    private void sendPlanetUnregistrationResponse(PlanetUnregistrationResponse response) {
        try {
            kafkaTemplate.send("planet-unregistration-response", response.getRequestId(), response);
            log.info("Sent planet unregistration response for requestId: {} with status: {}",
                       response.getRequestId(), response.getStatus());
        } catch (Exception e) {
            log.error("Error sending planet unregistration response for requestId: {}",
                        response.getRequestId(), e);
        }
    }

    private void sendErrorResponse(PlanetUnregistrationRequest request, String errorMessage) {
        try {
            PlanetUnregistrationResponse errorResponse = planetRegistryService.createUnregistrationErrorResponse(request, errorMessage);
            sendPlanetUnregistrationResponse(errorResponse);
        } catch (Exception e) {
            log.error("Error sending unregistration error response for requestId: {}",
                        request.getRequestId(), e);
        }
    }
}
