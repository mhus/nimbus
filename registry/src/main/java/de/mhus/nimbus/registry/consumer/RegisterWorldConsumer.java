package de.mhus.nimbus.registry.consumer;

import de.mhus.nimbus.registry.service.PlanetRegistryService;
import de.mhus.nimbus.shared.avro.WorldRegistrationRequest;
import de.mhus.nimbus.shared.avro.WorldRegistrationResponse;
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
 * Kafka-Consumer für World-Registrierungs-Anfragen
 * Delegiert die Business-Logik an den PlanetRegistryService
 */
@Component
public class RegisterWorldConsumer {

    private static final Logger logger = LoggerFactory.getLogger(RegisterWorldConsumer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final PlanetRegistryService planetRegistryService;

    public RegisterWorldConsumer(KafkaTemplate<String, Object> kafkaTemplate,
                               PlanetRegistryService planetRegistryService) {
        this.kafkaTemplate = kafkaTemplate;
        this.planetRegistryService = planetRegistryService;
    }

    @KafkaListener(
        topics = "world-registration",
        groupId = "nimbus-registry-group",
        containerFactory = "avroKafkaListenerContainerFactory"
    )
    public void consumeWorldRegistrationMessage(@Payload WorldRegistrationRequest request,
                                              @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                              @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                              @Header(KafkaHeaders.OFFSET) long offset,
                                              Acknowledgment acknowledgment) {

        logger.info("Received world registration request from topic: '{}', partition: {}, offset: {}",
                   topic, partition, offset);

        try {
            // Validiere Request
            planetRegistryService.validateWorldRegistrationRequest(request);

            logger.info("Processing world registration request: requestId={}, world={}, planet={}, environment={}, registeredBy={}",
                       request.getRequestId(), request.getWorldName(), request.getPlanetName(),
                       request.getEnvironment(), request.getRegisteredBy());

            // Delegiere an Service-Schicht
            WorldRegistrationResponse response = planetRegistryService.processWorldRegistrationRequest(request);

            // Sende die Antwort zurück
            sendWorldRegistrationResponse(response);

            // Manual acknowledgment nach erfolgreicher Verarbeitung
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid world registration request: requestId={}, error={}",
                       request.getRequestId(), e.getMessage());
            sendErrorResponse(request, "Invalid request: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error processing world registration message: requestId={}",
                        request.getRequestId(), e);
            sendErrorResponse(request, "Internal processing error");
        }
    }

    private void sendWorldRegistrationResponse(WorldRegistrationResponse response) {
        try {
            kafkaTemplate.send("world-registration-response", response.getRequestId(), response);
            logger.info("Sent world registration response for requestId: {} with status: {}",
                       response.getRequestId(), response.getStatus());
        } catch (Exception e) {
            logger.error("Error sending world registration response for requestId: {}",
                        response.getRequestId(), e);
        }
    }

    private void sendErrorResponse(WorldRegistrationRequest request, String errorMessage) {
        try {
            WorldRegistrationResponse errorResponse = planetRegistryService.createWorldRegistrationErrorResponse(request, errorMessage);
            sendWorldRegistrationResponse(errorResponse);
        } catch (Exception e) {
            logger.error("Error sending world registration error response for requestId: {}",
                        request.getRequestId(), e);
        }
    }
}
