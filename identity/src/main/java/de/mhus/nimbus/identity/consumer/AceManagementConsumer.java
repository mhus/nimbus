package de.mhus.nimbus.identity.consumer;

import de.mhus.nimbus.identity.entity.Ace;
import de.mhus.nimbus.identity.service.AceService;
import de.mhus.nimbus.shared.avro.*;
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

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Kafka Consumer für ACE-Management-Nachrichten
 */
@Component
@Slf4j
public class AceManagementConsumer {

    private final AceService aceService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public AceManagementConsumer(AceService aceService, KafkaTemplate<String, Object> kafkaTemplate) {
        this.aceService = aceService;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Verarbeitet ACE-Erstellungsanfragen
     */
    @KafkaListener(topics = "ace-create-request", groupId = "identity-service")
    public void handleAceCreateRequest(@Payload AceCreateRequest request,
                                      @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                      @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                      @Header(KafkaHeaders.OFFSET) long offset,
                                      Acknowledgment acknowledgment) {

        log.info("Received ACE create request: requestId={}, rule={}, userId={}, topic={}, partition={}, offset={}",
                   request.getRequestId(), request.getRule(), request.getUserId(), topic, partition, offset);

        AceCreateResponse.Builder responseBuilder = AceCreateResponse.newBuilder()
                .setRequestId(request.getRequestId())
                .setTimestamp(Instant.now());

        try {
            Ace ace;
            if (request.getOrderValue() != null) {
                ace = aceService.createAceWithOrder(
                    request.getRule(),
                    request.getUserId(),
                    request.getOrderValue(),
                    request.getDescription()
                );
            } else {
                ace = aceService.createAce(
                    request.getRule(),
                    request.getUserId(),
                    request.getDescription()
                );
            }

            // Erfolgreiche Response erstellen
            AceCreateResponse response = responseBuilder
                    .setSuccess(true)
                    .setAceId(ace.getId())
                    .setRule(ace.getRule())
                    .setOrderValue(ace.getOrderValue())
                    .build();

            // Antwort über Kafka senden
            kafkaTemplate.send("ace-create-response", request.getRequestId(), response);

            log.info("Successfully created ACE with ID: {} for user: {}", ace.getId(), request.getUserId());
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing ACE create request: {}", e.getMessage(), e);

            // Fehler-Response erstellen
            AceCreateResponse response = responseBuilder
                    .setSuccess(false)
                    .setErrorMessage(e.getMessage())
                    .build();

            // Fehler-Antwort über Kafka senden
            kafkaTemplate.send("ace-create-response", request.getRequestId(), response);
            acknowledgment.acknowledge();
        }
    }

    /**
     * Verarbeitet ACE-Lookup-Anfragen
     */
    @KafkaListener(topics = "ace-lookup-request", groupId = "identity-service")
    public void handleAceLookupRequest(@Payload AceLookupRequest request,
                                      @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                      @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                      @Header(KafkaHeaders.OFFSET) long offset,
                                      Acknowledgment acknowledgment) {

        log.info("Received ACE lookup request: requestId={}, aceId={}, userId={}, topic={}, partition={}, offset={}",
                   request.getRequestId(), request.getAceId(), request.getUserId(), topic, partition, offset);

        AceLookupResponse.Builder responseBuilder = AceLookupResponse.newBuilder()
                .setRequestId(request.getRequestId())
                .setTimestamp(Instant.now());

        try {
            List<Ace> aces;

            if (request.getAceId() != null) {
                // Suche nach spezifischer ACE-ID
                aces = aceService.getAceById(request.getAceId())
                        .map(List::of)
                        .orElse(List.of());
            } else if (request.getUserId() != null) {
                // Suche nach Benutzer-ID
                if (request.getActiveOnly()) {
                    aces = aceService.getActiveAcesByUserId(request.getUserId());
                } else {
                    aces = aceService.getAcesByUserId(request.getUserId());
                }
            } else if (request.getRulePattern() != null) {
                // Suche nach Regel-Muster
                aces = aceService.searchAcesByRule(request.getRulePattern());
                if (request.getActiveOnly()) {
                    aces = aces.stream().filter(Ace::getActive).collect(Collectors.toList());
                }
            } else {
                aces = List.of();
            }

            // ACEs zu AceInfo konvertieren
            List<AceInfo> aceInfos = aces.stream()
                    .map(this::convertToAceInfo)
                    .collect(Collectors.toList());

            // Erfolgreiche Response erstellen
            AceLookupResponse response = responseBuilder
                    .setSuccess(true)
                    .setAces(aceInfos)
                    .setTotalCount((long) aceInfos.size())
                    .build();

            // Antwort über Kafka senden
            kafkaTemplate.send("ace-lookup-response", request.getRequestId(), response);

            log.info("Successfully found {} ACEs for lookup request", aceInfos.size());
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing ACE lookup request: {}", e.getMessage(), e);

            // Fehler-Response erstellen
            AceLookupResponse response = responseBuilder
                    .setSuccess(false)
                    .setErrorMessage(e.getMessage())
                    .setTotalCount(0L)
                    .build();

            // Fehler-Antwort über Kafka senden
            kafkaTemplate.send("ace-lookup-response", request.getRequestId(), response);
            acknowledgment.acknowledge();
        }
    }

    /**
     * Verarbeitet ACE-Update-Anfragen
     */
    @KafkaListener(topics = "ace-update-request", groupId = "identity-service")
    public void handleAceUpdateRequest(@Payload AceUpdateRequest request,
                                      @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                      @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                      @Header(KafkaHeaders.OFFSET) long offset,
                                      Acknowledgment acknowledgment) {

        log.info("Received ACE update request: requestId={}, aceId={}, topic={}, partition={}, offset={}",
                   request.getRequestId(), request.getAceId(), topic, partition, offset);

        AceUpdateResponse.Builder responseBuilder = AceUpdateResponse.newBuilder()
                .setRequestId(request.getRequestId())
                .setTimestamp(Instant.now());

        try {
            Ace ace = aceService.updateAce(
                request.getAceId(),
                request.getRule(),
                request.getOrderValue(),
                request.getDescription(),
                request.getActive()
            );

            // Erfolgreiche Response erstellen
            AceUpdateResponse response = responseBuilder
                    .setSuccess(true)
                    .setAceInfo(convertToAceInfo(ace))
                    .build();

            // Antwort über Kafka senden
            kafkaTemplate.send("ace-update-response", request.getRequestId(), response);

            log.info("Successfully updated ACE with ID: {}", ace.getId());
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing ACE update request: {}", e.getMessage(), e);

            // Fehler-Response erstellen
            AceUpdateResponse response = responseBuilder
                    .setSuccess(false)
                    .setErrorMessage(e.getMessage())
                    .build();

            // Fehler-Antwort über Kafka senden
            kafkaTemplate.send("ace-update-response", request.getRequestId(), response);
            acknowledgment.acknowledge();
        }
    }

    /**
     * Verarbeitet ACE-Löschanfragen
     */
    @KafkaListener(topics = "ace-delete-request", groupId = "identity-service")
    public void handleAceDeleteRequest(@Payload AceDeleteRequest request,
                                      @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                      @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                      @Header(KafkaHeaders.OFFSET) long offset,
                                      Acknowledgment acknowledgment) {

        log.info("Received ACE delete request: requestId={}, aceId={}, userId={}, topic={}, partition={}, offset={}",
                   request.getRequestId(), request.getAceId(), request.getUserId(), topic, partition, offset);

        AceDeleteResponse.Builder responseBuilder = AceDeleteResponse.newBuilder()
                .setRequestId(request.getRequestId())
                .setTimestamp(Instant.now());

        try {
            long deletedCount;

            if (request.getAceId() != null) {
                // Lösche spezifische ACE
                aceService.deleteAce(request.getAceId());
                deletedCount = 1;
            } else if (request.getUserId() != null) {
                // Lösche alle ACEs für einen Benutzer
                long countBefore = aceService.countAcesByUserId(request.getUserId());
                aceService.deleteAllAcesForUser(request.getUserId());
                deletedCount = countBefore;
            } else {
                deletedCount = 0;
            }

            // Erfolgreiche Response erstellen
            AceDeleteResponse response = responseBuilder
                    .setSuccess(true)
                    .setDeletedCount(deletedCount)
                    .build();

            // Antwort über Kafka senden
            kafkaTemplate.send("ace-delete-response", request.getRequestId(), response);

            log.info("Successfully deleted {} ACE(s)", deletedCount);
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing ACE delete request: {}", e.getMessage(), e);

            // Fehler-Response erstellen
            AceDeleteResponse response = responseBuilder
                    .setSuccess(false)
                    .setDeletedCount(0L)
                    .setErrorMessage(e.getMessage())
                    .build();

            // Fehler-Antwort über Kafka senden
            kafkaTemplate.send("ace-delete-response", request.getRequestId(), response);
            acknowledgment.acknowledge();
        }
    }

    /**
     * Konvertiert eine Ace-Entity zu AceInfo für Avro-Responses
     */
    private AceInfo convertToAceInfo(Ace ace) {
        return AceInfo.newBuilder()
                .setAceId(ace.getId())
                .setRule(ace.getRule())
                .setOrderValue(ace.getOrderValue())
                .setDescription(ace.getDescription())
                .setActive(ace.getActive())
                .setCreatedAt(ace.getCreatedAt())
                .setUpdatedAt(ace.getUpdatedAt())
                .build();
    }
}
