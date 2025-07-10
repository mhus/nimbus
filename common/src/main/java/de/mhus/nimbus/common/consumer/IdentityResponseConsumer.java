package de.mhus.nimbus.common.consumer;

import de.mhus.nimbus.common.client.IdentityClient;
import de.mhus.nimbus.shared.avro.LoginResponse;
import de.mhus.nimbus.shared.avro.UserLookupResponse;
import de.mhus.nimbus.shared.avro.PlayerCharacterLookupResponse;
import de.mhus.nimbus.shared.avro.PublicKeyResponse;
import de.mhus.nimbus.shared.avro.AceCreateResponse;
import de.mhus.nimbus.shared.avro.AceLookupResponse;
import de.mhus.nimbus.shared.avro.AceUpdateResponse;
import de.mhus.nimbus.shared.avro.AceDeleteResponse;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class IdentityResponseConsumer {

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

        log.debug("Received login response: requestId={}, status={}, topic={}, partition={}, offset={}",
                    response.getRequestId(), response.getStatus(), topic, partition, offset);

        try {
            // Delegiere an IdentityClient
            boolean handled = identityClient.handleLoginResponse(response);

            if (handled) {
                // Bestätige die Verarbeitung nur wenn die Response zugeordnet werden konnte
                acknowledgment.acknowledge();
                log.debug("Successfully processed login response: requestId={}", response.getRequestId());
            } else {
                log.warn("Could not process login response - message will not be acknowledged: requestId={}", response.getRequestId());
            }

        } catch (Exception e) {
            log.error("Error handling login response: requestId={}", response.getRequestId(), e);
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

        log.debug("Received user lookup response: requestId={}, status={}, topic={}, partition={}, offset={}",
                    response.getRequestId(), response.getStatus(), topic, partition, offset);

        try {
            // Delegiere an IdentityClient
            boolean handled = identityClient.handleUserLookupResponse(response);

            if (handled) {
                // Bestätige die Verarbeitung nur wenn die Response zugeordnet werden konnte
                acknowledgment.acknowledge();
                log.debug("Successfully processed user lookup response: requestId={}", response.getRequestId());
            } else {
                log.warn("Could not process user lookup response - message will not be acknowledged: requestId={}", response.getRequestId());
            }

        } catch (Exception e) {
            log.error("Error handling user lookup response: requestId={}", response.getRequestId(), e);
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

        log.debug("Received character lookup response: requestId={}, status={}, characters count={}, topic={}, partition={}, offset={}",
                    response.getRequestId(), response.getStatus(), response.getCharacters().size(), topic, partition, offset);

        try {
            // Delegiere an IdentityClient
            boolean handled = identityClient.handleCharacterLookupResponse(response);

            if (handled) {
                // Bestätige die Verarbeitung nur wenn die Response zugeordnet werden konnte
                acknowledgment.acknowledge();
                log.debug("Successfully processed character lookup response: requestId={}", response.getRequestId());
            } else {
                log.warn("Could not process character lookup response - message will not be acknowledged: requestId={}", response.getRequestId());
            }

        } catch (Exception e) {
            log.error("Error handling character lookup response: requestId={}", response.getRequestId(), e);
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

        log.debug("Received public key response: requestId={}, status={}, topic={}, partition={}, offset={}",
                    response.getRequestId(), response.getStatus(), topic, partition, offset);

        try {
            // Delegiere an IdentityClient
            boolean handled = identityClient.handlePublicKeyResponse(response);

            if (handled) {
                // Bestätige die Verarbeitung nur wenn die Response zugeordnet werden konnte
                acknowledgment.acknowledge();
                log.debug("Successfully processed public key response: requestId={}", response.getRequestId());
            } else {
                log.warn("Could not process public key response - message will not be acknowledged: requestId={}", response.getRequestId());
            }

        } catch (Exception e) {
            log.error("Error handling public key response: requestId={}", response.getRequestId(), e);
            // Bestätige trotzdem, um Endlosschleife zu vermeiden
            acknowledgment.acknowledge();
        }
    }

    /**
     * Verarbeitet AceCreate-Responses vom Identity-Modul
     */
    @KafkaListener(
        topics = "ace-create-response",
        groupId = "identity-client-#{T(java.util.UUID).randomUUID().toString()}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleAceCreateResponse(@Payload AceCreateResponse response,
                                       @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                       @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                       @Header(KafkaHeaders.OFFSET) long offset,
                                       Acknowledgment acknowledgment) {

        log.debug("Received ACE create response: requestId={}, success={}, topic={}, partition={}, offset={}",
                    response.getRequestId(), response.getSuccess(), topic, partition, offset);

        try {
            // Delegiere an IdentityClient
            boolean handled = identityClient.handleAceCreateResponse(response);

            if (handled) {
                // Bestätige die Verarbeitung nur wenn die Response zugeordnet werden konnte
                acknowledgment.acknowledge();
                log.debug("Successfully processed ACE create response: requestId={}", response.getRequestId());
            } else {
                log.warn("Could not process ACE create response - message will not be acknowledged: requestId={}", response.getRequestId());
            }

        } catch (Exception e) {
            log.error("Error handling ACE create response: requestId={}", response.getRequestId(), e);
            // Bestätige trotzdem, um Endlosschleife zu vermeiden
            acknowledgment.acknowledge();
        }
    }

    /**
     * Verarbeitet AceLookup-Responses vom Identity-Modul
     */
    @KafkaListener(
        topics = "ace-lookup-response",
        groupId = "identity-client-#{T(java.util.UUID).randomUUID().toString()}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleAceLookupResponse(@Payload AceLookupResponse response,
                                       @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                       @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                       @Header(KafkaHeaders.OFFSET) long offset,
                                       Acknowledgment acknowledgment) {

        log.debug("Received ACE lookup response: requestId={}, success={}, topic={}, partition={}, offset={}",
                    response.getRequestId(), response.getSuccess(), topic, partition, offset);

        try {
            // Delegiere an IdentityClient
            boolean handled = identityClient.handleAceLookupResponse(response);

            if (handled) {
                // Bestätige die Verarbeitung nur wenn die Response zugeordnet werden konnte
                acknowledgment.acknowledge();
                log.debug("Successfully processed ACE lookup response: requestId={}", response.getRequestId());
            } else {
                log.warn("Could not process ACE lookup response - message will not be acknowledged: requestId={}", response.getRequestId());
            }

        } catch (Exception e) {
            log.error("Error handling ACE lookup response: requestId={}", response.getRequestId(), e);
            // Bestätige trotzdem, um Endlosschleife zu vermeiden
            acknowledgment.acknowledge();
        }
    }

    /**
     * Verarbeitet AceUpdate-Responses vom Identity-Modul
     */
    @KafkaListener(
        topics = "ace-update-response",
        groupId = "identity-client-#{T(java.util.UUID).randomUUID().toString()}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleAceUpdateResponse(@Payload AceUpdateResponse response,
                                       @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                       @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                       @Header(KafkaHeaders.OFFSET) long offset,
                                       Acknowledgment acknowledgment) {

        log.debug("Received ACE update response: requestId={}, success={}, topic={}, partition={}, offset={}",
                    response.getRequestId(), response.getSuccess(), topic, partition, offset);

        try {
            // Delegiere an IdentityClient
            boolean handled = identityClient.handleAceUpdateResponse(response);

            if (handled) {
                // Bestätige die Verarbeitung nur wenn die Response zugeordnet werden konnte
                acknowledgment.acknowledge();
                log.debug("Successfully processed ACE update response: requestId={}", response.getRequestId());
            } else {
                log.warn("Could not process ACE update response - message will not be acknowledged: requestId={}", response.getRequestId());
            }

        } catch (Exception e) {
            log.error("Error handling ACE update response: requestId={}", response.getRequestId(), e);
            // Bestätige trotzdem, um Endlosschleife zu vermeiden
            acknowledgment.acknowledge();
        }
    }

    /**
     * Verarbeitet AceDelete-Responses vom Identity-Modul
     */
    @KafkaListener(
        topics = "ace-delete-response",
        groupId = "identity-client-#{T(java.util.UUID).randomUUID().toString()}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleAceDeleteResponse(@Payload AceDeleteResponse response,
                                       @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                       @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                       @Header(KafkaHeaders.OFFSET) long offset,
                                       Acknowledgment acknowledgment) {

        log.debug("Received ACE delete response: requestId={}, success={}, topic={}, partition={}, offset={}",
                    response.getRequestId(), response.getSuccess(), topic, partition, offset);

        try {
            // Delegiere an IdentityClient
            boolean handled = identityClient.handleAceDeleteResponse(response);

            if (handled) {
                // Bestätige die Verarbeitung nur wenn die Response zugeordnet werden konnte
                acknowledgment.acknowledge();
                log.debug("Successfully processed ACE delete response: requestId={}", response.getRequestId());
            } else {
                log.warn("Could not process ACE delete response - message will not be acknowledged: requestId={}", response.getRequestId());
            }

        } catch (Exception e) {
            log.error("Error handling ACE delete response: requestId={}", response.getRequestId(), e);
            // Bestätige trotzdem, um Endlosschleife zu vermeiden
            acknowledgment.acknowledge();
        }
    }
}
