package de.mhus.nimbus.identity.consumer;

import de.mhus.nimbus.identity.service.LoginService;
import de.mhus.nimbus.shared.avro.LoginRequest;
import de.mhus.nimbus.shared.avro.LoginResponse;
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
 * Kafka Consumer für Login-Anfragen
 */
@Component
@Slf4j
public class LoginConsumer {

    private final LoginService loginService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public LoginConsumer(LoginService loginService, KafkaTemplate<String, Object> kafkaTemplate) {
        this.loginService = loginService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "login-request", groupId = "identity-service")
    public void handleLoginRequest(@Payload LoginRequest request,
                                  @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                  @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                  @Header(KafkaHeaders.OFFSET) long offset,
                                  Acknowledgment acknowledgment) {

        log.info("Received login request: requestId={}, username={}, topic={}, partition={}, offset={}",
                   request.getRequestId(), request.getUsername(), topic, partition, offset);

        try {
            // Validiere die Anfrage
            loginService.validateLoginRequest(request);

            // Verarbeite die Login-Anfrage
            LoginResponse response = loginService.processLoginRequest(request);

            // Sende die Antwort zurück
            kafkaTemplate.send("login-response", request.getRequestId(), response);

            log.info("Successfully processed login request: requestId={}, status={}",
                       request.getRequestId(), response.getStatus());

            // Bestätige die Verarbeitung
            acknowledgment.acknowledge();

        } catch (IllegalArgumentException e) {
            log.error("Invalid login request: requestId={}, error={}",
                        request.getRequestId(), e.getMessage());

            // Sende Error-Response
            LoginResponse errorResponse = loginService.createLoginErrorResponse(request, e.getMessage());
            kafkaTemplate.send("login-response", request.getRequestId(), errorResponse);

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing login request: requestId={}",
                        request.getRequestId(), e);

            // Sende Error-Response
            LoginResponse errorResponse = loginService.createLoginErrorResponse(
                request, "Internal error processing login request");
            kafkaTemplate.send("login-response", request.getRequestId(), errorResponse);

            acknowledgment.acknowledge();
        }
    }
}
