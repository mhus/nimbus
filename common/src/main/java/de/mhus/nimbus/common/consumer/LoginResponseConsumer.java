package de.mhus.nimbus.common.consumer;

import de.mhus.nimbus.common.service.SecurityService;
import de.mhus.nimbus.shared.avro.LoginResponse;
import de.mhus.nimbus.shared.avro.PublicKeyResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka Consumer für Login-Responses und Public Key Responses vom Identity Service
 * Wird nur aktiviert wenn Kafka verfügbar ist
 */
@Component
@ConditionalOnProperty(name = "nimbus.security.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class LoginResponseConsumer {

    private static final Logger logger = LoggerFactory.getLogger(LoginResponseConsumer.class);

    private final SecurityService securityService;

    public LoginResponseConsumer(SecurityService securityService) {
        this.securityService = securityService;
    }

    /**
     * Verarbeitet Login-Responses vom Identity Service
     */
    @KafkaListener(
        topics = "login-response",
        groupId = "#{T(java.util.UUID).randomUUID().toString()}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleLoginResponse(@Payload LoginResponse response,
                                   @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                   @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                   @Header(KafkaHeaders.OFFSET) long offset,
                                   Acknowledgment acknowledgment) {

        logger.debug("Received login response: requestId={}, status={}, topic={}, partition={}, offset={}",
                    response.getRequestId(), response.getStatus(), topic, partition, offset);

        try {
            // Delegiere an SecurityService
            securityService.handleLoginResponse(response, topic);

            // Bestätige die Verarbeitung
            acknowledgment.acknowledge();

        } catch (Exception e) {
            logger.error("Error handling login response: requestId={}", response.getRequestId(), e);
            // Bestätige trotzdem, um Endlosschleife zu vermeiden
            acknowledgment.acknowledge();
        }
    }

    /**
     * Verarbeitet Public Key Responses vom Identity Service
     */
    @KafkaListener(
        topics = "public-key-response",
        groupId = "#{T(java.util.UUID).randomUUID().toString()}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePublicKeyResponse(@Payload PublicKeyResponse response,
                                       @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                       @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                       @Header(KafkaHeaders.OFFSET) long offset,
                                       Acknowledgment acknowledgment) {

        logger.debug("Received public key response: requestId={}, status={}, topic={}, partition={}, offset={}",
                    response.getRequestId(), response.getStatus(), topic, partition, offset);

        try {
            // Delegiere an SecurityService
            securityService.handlePublicKeyResponse(response, topic);

            // Bestätige die Verarbeitung
            acknowledgment.acknowledge();

        } catch (Exception e) {
            logger.error("Error handling public key response: requestId={}", response.getRequestId(), e);
            // Bestätige trotzdem, um Endlosschleife zu vermeiden
            acknowledgment.acknowledge();
        }
    }
}
