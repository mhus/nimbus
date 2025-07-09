package de.mhus.nimbus.common.consumer;

import de.mhus.nimbus.common.client.RegistryClient;
import de.mhus.nimbus.shared.avro.PlanetRegistrationResponse;
import de.mhus.nimbus.shared.avro.PlanetUnregistrationResponse;
import de.mhus.nimbus.shared.avro.PlanetLookupResponse;
import de.mhus.nimbus.shared.avro.WorldRegistrationResponse;
import de.mhus.nimbus.shared.avro.WorldUnregistrationResponse;
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
public class RegistryResponseConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistryResponseConsumer.class);

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

        LOGGER.debug("Received planet registration response: requestId={}, status={}, planet={}, topic={}, partition={}, offset={}",
                    response.getRequestId(), response.getStatus(), response.getPlanetName(), topic, partition, offset);

        try {
            // Delegiere an RegistryClient
            registryClient.handlePlanetRegistrationResponse(response);

            // Bestätige die Verarbeitung
            acknowledgment.acknowledge();
            LOGGER.debug("Successfully processed planet registration response: requestId={}", response.getRequestId());

        } catch (Exception e) {
            LOGGER.error("Error handling planet registration response: requestId={}", response.getRequestId(), e);
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

        LOGGER.debug("Received planet unregistration response: requestId={}, status={}, planet={}, topic={}, partition={}, offset={}",
                    response.getRequestId(), response.getStatus(), response.getPlanetName(), topic, partition, offset);

        try {
            // Delegiere an RegistryClient
            registryClient.handlePlanetUnregistrationResponse(response);

            // Bestätige die Verarbeitung
            acknowledgment.acknowledge();
            LOGGER.debug("Successfully processed planet unregistration response: requestId={}", response.getRequestId());

        } catch (Exception e) {
            LOGGER.error("Error handling planet unregistration response: requestId={}", response.getRequestId(), e);
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

        LOGGER.debug("Received planet lookup response: requestId={}, status={}, worlds count={}, topic={}, partition={}, offset={}",
                    response.getRequestId(), response.getStatus(),
                    response.getPlanetWorlds() != null ? response.getPlanetWorlds().size() : 0, topic, partition, offset);

        try {
            // Delegiere an RegistryClient
            registryClient.handlePlanetLookupResponse(response);

            // Bestätige die Verarbeitung
            acknowledgment.acknowledge();
            LOGGER.debug("Successfully processed planet lookup response: requestId={}", response.getRequestId());

        } catch (Exception e) {
            LOGGER.error("Error handling planet lookup response: requestId={}", response.getRequestId(), e);
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

        LOGGER.debug("Received world registration response: requestId={}, status={}, world={}, topic={}, partition={}, offset={}",
                    response.getRequestId(), response.getStatus(), response.getWorldId(), topic, partition, offset);

        try {
            // Delegiere an RegistryClient
            registryClient.handleWorldRegistrationResponse(response);

            // Bestätige die Verarbeitung
            acknowledgment.acknowledge();
            LOGGER.debug("Successfully processed world registration response: requestId={}", response.getRequestId());

        } catch (Exception e) {
            LOGGER.error("Error handling world registration response: requestId={}", response.getRequestId(), e);
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

        LOGGER.debug("Received world unregistration response: requestId={}, status={}, world={}, topic={}, partition={}, offset={}",
                    response.getRequestId(), response.getStatus(), response.getWorldId(), topic, partition, offset);

        try {
            // Delegiere an RegistryClient
            registryClient.handleWorldUnregistrationResponse(response);

            // Bestätige die Verarbeitung
            acknowledgment.acknowledge();
            LOGGER.debug("Successfully processed world unregistration response: requestId={}", response.getRequestId());

        } catch (Exception e) {
            LOGGER.error("Error handling world unregistration response: requestId={}", response.getRequestId(), e);
            // Bestätige trotzdem, um Endlosschleife zu vermeiden
            acknowledgment.acknowledge();
        }
    }
}
