package de.mhus.nimbus.identity.consumer;

import de.mhus.nimbus.identity.service.IdentityLookupService;
import de.mhus.nimbus.shared.avro.PlayerCharacterLookupRequest;
import de.mhus.nimbus.shared.avro.PlayerCharacterLookupResponse;
import lombok.extern.slf4j.Slf4j;
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
 * Kafka Consumer für IdentityCharacter-Lookup-Anfragen
 */
@Component
@Slf4j
public class IdentityCharacterLookupConsumer {

    private final IdentityLookupService identityLookupService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public IdentityCharacterLookupConsumer(IdentityLookupService identityLookupService,
                                        KafkaTemplate<String, Object> kafkaTemplate) {
        this.identityLookupService = identityLookupService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "identity-character-lookup-request", groupId = "identity-service")
    public void handleIdentityCharacterLookupRequest(@Payload PlayerCharacterLookupRequest request,
                                                  @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                                  @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                                  @Header(KafkaHeaders.OFFSET) long offset,
                                                  Acknowledgment acknowledgment) {

        log.info("Received identity character lookup request: requestId={}, characterId={}, characterName={}, userId={}, planet={}, worldId={}, activeOnly={}, topic={}, partition={}, offset={}",
                   request.getRequestId(), request.getCharacterId(), request.getCharacterName(),
                   request.getUserId(), request.getCurrentPlanet(), request.getCurrentWorldId(),
                   request.getActiveOnly(), topic, partition, offset);

        try {
            // Validiere die Anfrage
            identityLookupService.validatePlayerCharacterLookupRequest(request);

            // Verarbeite die Anfrage
            PlayerCharacterLookupResponse response = identityLookupService.processPlayerCharacterLookupRequest(request);

            // Sende die Antwort zurück
            kafkaTemplate.send("identity-character-lookup-response", request.getRequestId(), response);

            log.info("Successfully processed identity character lookup request: requestId={}, status={}, characterCount={}",
                       request.getRequestId(), response.getStatus(), response.getCharacters().size());

            // Bestätige die Verarbeitung
            acknowledgment.acknowledge();

        } catch (IllegalArgumentException e) {
            log.error("Invalid identity character lookup request: requestId={}, error={}",
                        request.getRequestId(), e.getMessage());

            // Sende Error-Response
            PlayerCharacterLookupResponse errorResponse = identityLookupService.createPlayerCharacterLookupErrorResponse(request, e.getMessage());
            kafkaTemplate.send("identity-character-lookup-response", request.getRequestId(), errorResponse);

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing identity character lookup request: requestId={}",
                        request.getRequestId(), e);

            // Sende Error-Response
            PlayerCharacterLookupResponse errorResponse = identityLookupService.createPlayerCharacterLookupErrorResponse(
                request, "Internal error processing identity character lookup request");
            kafkaTemplate.send("identity-character-lookup-response", request.getRequestId(), errorResponse);

            acknowledgment.acknowledge();
        }
    }
}
