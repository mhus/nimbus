package de.mhus.nimbus.common.consumer;

import de.mhus.nimbus.common.client.RegistryClient;
import de.mhus.nimbus.shared.avro.PlanetRegistrationResponse;
import de.mhus.nimbus.shared.avro.PlanetUnregistrationResponse;
import de.mhus.nimbus.shared.avro.PlanetLookupResponse;
import de.mhus.nimbus.shared.avro.WorldRegistrationResponse;
import de.mhus.nimbus.shared.avro.WorldUnregistrationResponse;
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
 * Kafka Consumer für Registry-Responses
 * Verarbeitet alle Response-Typen vom Registry-Modul und leitet sie an den RegistryClient weiter
 */
@Component
@ConditionalOnProperty(name = "nimbus.kafka.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class RegistryResponseConsumer {

    private final RegistryClient registryClient;

    @Autowired
    public RegistryResponseConsumer(RegistryClient registryClient) {
        this.registryClient = registryClient;
    }

    /**
     * Verarbeitet PlanetRegistration-Responses vom Registry-Modul
     */
    @KafkaListener(
        topics = "planet-registration-response",
        groupId = "registry-client-#{T(java.util.UUID).randomUUID().toString()}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePlanetRegistrationResponse(@Payload PlanetRegistrationResponse response,
                                                @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                                @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                                @Header(KafkaHeaders.OFFSET) long offset,
                                                Acknowledgment acknowledgment) {

        log.debug("Received planet registration response: requestId={}, status={}, planet={}, topic={}, partition={}, offset={}",
                    response.getRequestId(), response.getStatus(), response.getPlanetName(), topic, partition, offset);

        try {
            // Delegiere an RegistryClient
            boolean handled = registryClient.handlePlanetRegistrationResponse(response);

            if (handled) {
                // Bestätige die Verarbeitung nur wenn die Response zugeordnet werden konnte
                acknowledgment.acknowledge();
                log.debug("Successfully processed planet registration response: requestId={}", response.getRequestId());
            } else {
                log.warn("Could not process planet registration response - message will not be acknowledged: requestId={}", response.getRequestId());
            }

        } catch (Exception e) {
            log.error("Error handling planet registration response: requestId={}", response.getRequestId(), e);
            // Bestätige trotzdem, um Endlosschleife zu vermeiden
            acknowledgment.acknowledge();
        }
    }

    /**
     * Verarbeitet PlanetUnregistration-Responses vom Registry-Modul
     */
    @KafkaListener(
        topics = "planet-unregistration-response",
        groupId = "registry-client-#{T(java.util.UUID).randomUUID().toString()}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePlanetUnregistrationResponse(@Payload PlanetUnregistrationResponse response,
                                                  @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                                  @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                                  @Header(KafkaHeaders.OFFSET) long offset,
                                                  Acknowledgment acknowledgment) {

        log.debug("Received planet unregistration response: requestId={}, status={}, planet={}, topic={}, partition={}, offset={}",
                    response.getRequestId(), response.getStatus(), response.getPlanetName(), topic, partition, offset);

        try {
            // Delegiere an RegistryClient
            boolean handled = registryClient.handlePlanetUnregistrationResponse(response);

            if (handled) {
                // Bestätige die Verarbeitung nur wenn die Response zugeordnet werden konnte
                acknowledgment.acknowledge();
                log.debug("Successfully processed planet unregistration response: requestId={}", response.getRequestId());
            } else {
                log.warn("Could not process planet unregistration response - message will not be acknowledged: requestId={}", response.getRequestId());
            }

        } catch (Exception e) {
            log.error("Error handling planet unregistration response: requestId={}", response.getRequestId(), e);
            // Bestätige trotzdem, um Endlosschleife zu vermeiden
            acknowledgment.acknowledge();
        }
    }

    /**
     * Verarbeitet PlanetLookup-Responses vom Registry-Modul
     */
    @KafkaListener(
        topics = "planet-lookup-response",
        groupId = "registry-client-#{T(java.util.UUID).randomUUID().toString()}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePlanetLookupResponse(@Payload PlanetLookupResponse response,
                                          @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                          @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                          @Header(KafkaHeaders.OFFSET) long offset,
                                          Acknowledgment acknowledgment) {

        log.debug("Received planet lookup response: requestId={}, status={}, worlds count={}, topic={}, partition={}, offset={}",
                    response.getRequestId(), response.getStatus(),
                    response.getPlanetWorlds() != null ? response.getPlanetWorlds().size() : 0, topic, partition, offset);

        try {
            // Delegiere an RegistryClient
            boolean handled = registryClient.handlePlanetLookupResponse(response);

            if (handled) {
                // Bestätige die Verarbeitung nur wenn die Response zugeordnet werden konnte
                acknowledgment.acknowledge();
                log.debug("Successfully processed planet lookup response: requestId={}", response.getRequestId());
            } else {
                log.warn("Could not process planet lookup response - message will not be acknowledged: requestId={}", response.getRequestId());
            }

        } catch (Exception e) {
            log.error("Error handling planet lookup response: requestId={}", response.getRequestId(), e);
            // Bestätige trotzdem, um Endlosschleife zu vermeiden
            acknowledgment.acknowledge();
        }
    }

    /**
     * Verarbeitet WorldRegistration-Responses vom Registry-Modul
     */
    @KafkaListener(
        topics = "world-registration-response",
        groupId = "registry-client-#{T(java.util.UUID).randomUUID().toString()}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleWorldRegistrationResponse(@Payload WorldRegistrationResponse response,
                                               @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                               @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                               @Header(KafkaHeaders.OFFSET) long offset,
                                               Acknowledgment acknowledgment) {

        log.debug("Received world registration response: requestId={}, status={}, world={}, topic={}, partition={}, offset={}",
                    response.getRequestId(), response.getStatus(), response.getWorldId(), topic, partition, offset);

        try {
            // Delegiere an RegistryClient
            boolean handled = registryClient.handleWorldRegistrationResponse(response);

            if (handled) {
                // Bestätige die Verarbeitung nur wenn die Response zugeordnet werden konnte
                acknowledgment.acknowledge();
                log.debug("Successfully processed world registration response: requestId={}", response.getRequestId());
            } else {
                log.warn("Could not process world registration response - message will not be acknowledged: requestId={}", response.getRequestId());
            }

        } catch (Exception e) {
            log.error("Error handling world registration response: requestId={}", response.getRequestId(), e);
            // Bestätige trotzdem, um Endlosschleife zu vermeiden
            acknowledgment.acknowledge();
        }
    }

    /**
     * Verarbeitet WorldUnregistration-Responses vom Registry-Modul
     */
    @KafkaListener(
        topics = "world-unregistration-response",
        groupId = "registry-client-#{T(java.util.UUID).randomUUID().toString()}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleWorldUnregistrationResponse(@Payload WorldUnregistrationResponse response,
                                                 @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                                 @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                                 @Header(KafkaHeaders.OFFSET) long offset,
                                                 Acknowledgment acknowledgment) {

        log.debug("Received world unregistration response: requestId={}, status={}, world={}, topic={}, partition={}, offset={}",
                    response.getRequestId(), response.getStatus(), response.getWorldId(), topic, partition, offset);

        try {
            // Delegiere an RegistryClient
            boolean handled = registryClient.handleWorldUnregistrationResponse(response);

            if (handled) {
                // Bestätige die Verarbeitung nur wenn die Response zugeordnet werden konnte
                acknowledgment.acknowledge();
                log.debug("Successfully processed world unregistration response: requestId={}", response.getRequestId());
            } else {
                log.warn("Could not process world unregistration response - message will not be acknowledged: requestId={}", response.getRequestId());
            }

        } catch (Exception e) {
            log.error("Error handling world unregistration response: requestId={}", response.getRequestId(), e);
            // Bestätige trotzdem, um Endlosschleife zu vermeiden
            acknowledgment.acknowledge();
        }
    }
}
