package de.mhus.nimbus.registry.consumer;

import de.mhus.nimbus.registry.service.PlanetRegistryService;
import de.mhus.nimbus.shared.avro.WorldUnregistrationRequest;
import de.mhus.nimbus.shared.avro.WorldUnregistrationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka-Consumer für World-Deregistrierungs-Anfragen
 * Delegiert die Business-Logik an den PlanetRegistryService
 */
@Component
public class UnregisterWorldConsumer {

    private static final Logger logger = LoggerFactory.getLogger(UnregisterWorldConsumer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final PlanetRegistryService planetRegistryService;

    public UnregisterWorldConsumer(KafkaTemplate<String, Object> kafkaTemplate,
                                 PlanetRegistryService planetRegistryService) {
        this.kafkaTemplate = kafkaTemplate;
        this.planetRegistryService = planetRegistryService;
    }

    @KafkaListener(
        topics = "world-unregistration",
        groupId = "nimbus-registry-group",
        containerFactory = "avroKafkaListenerContainerFactory"
    )
    public void consumeWorldUnregistrationMessage(@Payload WorldUnregistrationRequest request,
                                                @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                                @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                                @Header(KafkaHeaders.OFFSET) long offset,
                                                Acknowledgment acknowledgment) {

        logger.info("Received world unregistration request from topic: '{}', partition: {}, offset: {}",
                   topic, partition, offset);

        try {
            // Validiere Request
            planetRegistryService.validateWorldUnregistrationRequest(request);

            logger.info("Processing world unregistration request: requestId={}, worldId={}, planetName={}, environment={}, unregisteredBy={}",
                       request.getRequestId(), request.getWorldId(), request.getPlanetName(),
                       request.getEnvironment(), request.getUnregisteredBy());

            // Delegiere an Service-Schicht
            WorldUnregistrationResponse response = planetRegistryService.processWorldUnregistrationRequest(request);

            // Sende die Antwort zurück
            sendWorldUnregistrationResponse(response);

            // Manual acknowledgment nach erfolgreicher Verarbeitung
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid world unregistration request: requestId={}, error={}",
                       request.getRequestId(), e.getMessage());
            sendErrorResponse(request, "Invalid request: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error processing world unregistration message: requestId={}",
                        request.getRequestId(), e);
            sendErrorResponse(request, "Internal processing error");
        }
    }

    private void sendWorldUnregistrationResponse(WorldUnregistrationResponse response) {
        try {
            kafkaTemplate.send("world-unregistration-response", response.getRequestId(), response);
            logger.info("Sent world unregistration response for requestId: {} with status: {}",
                       response.getRequestId(), response.getStatus());
        } catch (Exception e) {
            logger.error("Error sending world unregistration response for requestId: {}",
                        response.getRequestId(), e);
        }
    }

    private void sendErrorResponse(WorldUnregistrationRequest request, String errorMessage) {
        try {
            WorldUnregistrationResponse errorResponse = planetRegistryService.createWorldUnregistrationErrorResponse(request, errorMessage);
            sendWorldUnregistrationResponse(errorResponse);
        } catch (Exception e) {
            logger.error("Error sending world unregistration error response for requestId: {}",
                        request.getRequestId(), e);
        }
    }
}
