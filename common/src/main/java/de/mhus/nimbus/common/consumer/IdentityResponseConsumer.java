package de.mhus.nimbus.common.consumer;

import de.mhus.nimbus.common.client.IdentityClient;
import de.mhus.nimbus.shared.avro.LoginResponse;
import de.mhus.nimbus.shared.avro.UserLookupResponse;
import de.mhus.nimbus.shared.avro.PlayerCharacterLookupResponse;
import de.mhus.nimbus.shared.avro.PublicKeyResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka Consumer für Identity-Responses
 * Verarbeitet alle Response-Typen vom Identity-Modul und leitet sie an den IdentityClient weiter
 */
@Component
@ConditionalOnProperty(name = "nimbus.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class IdentityResponseConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(IdentityResponseConsumer.class);

    private final IdentityClient identityClient;

    @Autowired
    public IdentityResponseConsumer(IdentityClient identityClient) {
        this.identityClient = identityClient;
    }

    /**
     * Verarbeitet Login-Responses vom Identity-Modul
     */
    @KafkaListener(
        topics = "login-response",
        groupId = "identity-client-#{T(java.util.UUID).randomUUID().toString()}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleLoginResponse(@Payload LoginResponse response,
                                   @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                   @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                   @Header(KafkaHeaders.OFFSET) long offset,
                                   Acknowledgment acknowledgment) {

        LOGGER.debug("Received login response: requestId={}, status={}, topic={}, partition={}, offset={}",
                    response.getRequestId(), response.getStatus(), topic, partition, offset);

        try {
            // Delegiere an IdentityClient
            identityClient.handleLoginResponse(response);

            // Bestätige die Verarbeitung
            acknowledgment.acknowledge();
            LOGGER.debug("Successfully processed login response: requestId={}", response.getRequestId());

        } catch (Exception e) {
            LOGGER.error("Error handling login response: requestId={}", response.getRequestId(), e);
            // Bestätige trotzdem, um Endlosschleife zu vermeiden
            acknowledgment.acknowledge();
        }
    }

    /**
     * Verarbeitet UserLookup-Responses vom Identity-Modul
     */
    @KafkaListener(
        topics = "user-lookup-response",
        groupId = "identity-client-#{T(java.util.UUID).randomUUID().toString()}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleUserLookupResponse(@Payload UserLookupResponse response,
                                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                        @Header(KafkaHeaders.OFFSET) long offset,
                                        Acknowledgment acknowledgment) {

        LOGGER.debug("Received user lookup response: requestId={}, status={}, topic={}, partition={}, offset={}",
                    response.getRequestId(), response.getStatus(), topic, partition, offset);

        try {
            // Delegiere an IdentityClient
            identityClient.handleUserLookupResponse(response);

            // Bestätige die Verarbeitung
            acknowledgment.acknowledge();
            LOGGER.debug("Successfully processed user lookup response: requestId={}", response.getRequestId());

        } catch (Exception e) {
            LOGGER.error("Error handling user lookup response: requestId={}", response.getRequestId(), e);
            // Bestätige trotzdem, um Endlosschleife zu vermeiden
            acknowledgment.acknowledge();
        }
    }

    /**
     * Verarbeitet PlayerCharacterLookup-Responses vom Identity-Modul
     */
    @KafkaListener(
        topics = "identity-character-lookup-response",
        groupId = "identity-client-#{T(java.util.UUID).randomUUID().toString()}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleCharacterLookupResponse(@Payload PlayerCharacterLookupResponse response,
                                             @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                             @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                             @Header(KafkaHeaders.OFFSET) long offset,
                                             Acknowledgment acknowledgment) {

        LOGGER.debug("Received character lookup response: requestId={}, status={}, characters count={}, topic={}, partition={}, offset={}",
                    response.getRequestId(), response.getStatus(), response.getCharacters().size(), topic, partition, offset);

        try {
            // Delegiere an IdentityClient
            identityClient.handleCharacterLookupResponse(response);

            // Bestätige die Verarbeitung
            acknowledgment.acknowledge();
            LOGGER.debug("Successfully processed character lookup response: requestId={}", response.getRequestId());

        } catch (Exception e) {
            LOGGER.error("Error handling character lookup response: requestId={}", response.getRequestId(), e);
            // Bestätige trotzdem, um Endlosschleife zu vermeiden
            acknowledgment.acknowledge();
        }
    }

    /**
     * Verarbeitet PublicKey-Responses vom Identity-Modul
     */
    @KafkaListener(
        topics = "public-key-response",
        groupId = "identity-client-#{T(java.util.UUID).randomUUID().toString()}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePublicKeyResponse(@Payload PublicKeyResponse response,
                                       @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                       @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                       @Header(KafkaHeaders.OFFSET) long offset,
                                       Acknowledgment acknowledgment) {

        LOGGER.debug("Received public key response: requestId={}, status={}, topic={}, partition={}, offset={}",
                    response.getRequestId(), response.getStatus(), topic, partition, offset);

        try {
            // Delegiere an IdentityClient
            identityClient.handlePublicKeyResponse(response);

            // Bestätige die Verarbeitung
            acknowledgment.acknowledge();
            LOGGER.debug("Successfully processed public key response: requestId={}", response.getRequestId());

        } catch (Exception e) {
            LOGGER.error("Error handling public key response: requestId={}", response.getRequestId(), e);
            // Bestätige trotzdem, um Endlosschleife zu vermeiden
            acknowledgment.acknowledge();
        }
    }
}
