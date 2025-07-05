package de.mhus.nimbus.identity.consumer;

import de.mhus.nimbus.identity.service.PublicKeyService;
import de.mhus.nimbus.shared.avro.PublicKeyRequest;
import de.mhus.nimbus.shared.avro.PublicKeyResponse;
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
 * Kafka Consumer für Public Key Anfragen
 */
@Component
public class PublicKeyConsumer {

    private static final Logger logger = LoggerFactory.getLogger(PublicKeyConsumer.class);

    private final PublicKeyService publicKeyService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PublicKeyConsumer(PublicKeyService publicKeyService, KafkaTemplate<String, Object> kafkaTemplate) {
        this.publicKeyService = publicKeyService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "public-key-request", groupId = "identity-service")
    public void handlePublicKeyRequest(@Payload PublicKeyRequest request,
                                      @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                      @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                      @Header(KafkaHeaders.OFFSET) long offset,
                                      Acknowledgment acknowledgment) {

        logger.info("Received public key request: requestId={}, requestedBy={}, topic={}, partition={}, offset={}",
                   request.getRequestId(), request.getRequestedBy(), topic, partition, offset);

        try {
            // Validiere die Anfrage
            publicKeyService.validatePublicKeyRequest(request);

            // Verarbeite die Public Key Anfrage
            PublicKeyResponse response = publicKeyService.processPublicKeyRequest(request);

            // Sende die Antwort zurück
            kafkaTemplate.send("public-key-response", request.getRequestId(), response);

            logger.info("Successfully processed public key request: requestId={}, status={}",
                       request.getRequestId(), response.getStatus());

            // Bestätige die Verarbeitung
            acknowledgment.acknowledge();

        } catch (IllegalArgumentException e) {
            logger.error("Invalid public key request: requestId={}, error={}",
                        request.getRequestId(), e.getMessage());

            // Sende Error-Response
            PublicKeyResponse errorResponse = publicKeyService.createPublicKeyErrorResponse(request, e.getMessage());
            kafkaTemplate.send("public-key-response", request.getRequestId(), errorResponse);

            acknowledgment.acknowledge();

        } catch (Exception e) {
            logger.error("Error processing public key request: requestId={}",
                        request.getRequestId(), e);

            // Sende Error-Response
            PublicKeyResponse errorResponse = publicKeyService.createPublicKeyErrorResponse(
                request, "Internal error processing public key request");
            kafkaTemplate.send("public-key-response", request.getRequestId(), errorResponse);

            acknowledgment.acknowledge();
        }
    }
}
