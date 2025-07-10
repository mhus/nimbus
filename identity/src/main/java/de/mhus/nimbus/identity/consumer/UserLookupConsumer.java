package de.mhus.nimbus.identity.consumer;

import de.mhus.nimbus.identity.service.IdentityLookupService;
import de.mhus.nimbus.shared.avro.UserLookupRequest;
import de.mhus.nimbus.shared.avro.UserLookupResponse;
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
 * Kafka Consumer für User-Lookup-Anfragen
 */
@Component
@Slf4j
public class UserLookupConsumer {

    private final IdentityLookupService identityLookupService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public UserLookupConsumer(IdentityLookupService identityLookupService,
                             KafkaTemplate<String, Object> kafkaTemplate) {
        this.identityLookupService = identityLookupService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "user-lookup-request", groupId = "identity-service")
    public void handleUserLookupRequest(@Payload UserLookupRequest request,
                                       @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                       @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                       @Header(KafkaHeaders.OFFSET) long offset,
                                       Acknowledgment acknowledgment) {

        log.info("Received user lookup request: requestId={}, userId={}, username={}, email={}, topic={}, partition={}, offset={}",
                   request.getRequestId(), request.getUserId(), request.getUsername(), request.getEmail(),
                   topic, partition, offset);

        try {
            // Validiere die Anfrage
            identityLookupService.validateUserLookupRequest(request);

            // Verarbeite die Anfrage
            UserLookupResponse response = identityLookupService.processUserLookupRequest(request);

            // Sende die Antwort zurück
            kafkaTemplate.send("user-lookup-response", request.getRequestId(), response);

            log.info("Successfully processed user lookup request: requestId={}, status={}",
                       request.getRequestId(), response.getStatus());

            // Bestätige die Verarbeitung
            acknowledgment.acknowledge();

        } catch (IllegalArgumentException e) {
            log.error("Invalid user lookup request: requestId={}, error={}",
                        request.getRequestId(), e.getMessage());

            // Sende Error-Response
            UserLookupResponse errorResponse = identityLookupService.createUserLookupErrorResponse(request, e.getMessage());
            kafkaTemplate.send("user-lookup-response", request.getRequestId(), errorResponse);

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing user lookup request: requestId={}",
                        request.getRequestId(), e);

            // Sende Error-Response
            UserLookupResponse errorResponse = identityLookupService.createUserLookupErrorResponse(
                request, "Internal error processing user lookup request");
            kafkaTemplate.send("user-lookup-response", request.getRequestId(), errorResponse);

            acknowledgment.acknowledge();
        }
    }
}
