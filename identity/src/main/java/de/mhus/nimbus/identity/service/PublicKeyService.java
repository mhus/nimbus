package de.mhus.nimbus.identity.service;

import de.mhus.nimbus.identity.config.JwtProperties;
import de.mhus.nimbus.shared.avro.PublicKeyRequest;
import de.mhus.nimbus.shared.avro.PublicKeyResponse;
import de.mhus.nimbus.shared.avro.PublicKeyStatus;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Service für Public Key Operationen
 */
@Service
@Slf4j
public class PublicKeyService {

    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    public PublicKeyService(JwtService jwtService, JwtProperties jwtProperties) {
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
    }

    /**
     * Verarbeitet eine Public Key Anfrage
     */
    public PublicKeyResponse processPublicKeyRequest(PublicKeyRequest request) {
        log.info("Processing public key request: requestId={}, requestedBy={}",
                   request.getRequestId(), request.getRequestedBy());

        long currentTimestamp = Instant.now().toEpochMilli();

        try {
            // Hole den öffentlichen Schlüssel
            String publicKeyString = jwtService.getPublicKeyAsString();
            String issuer = jwtProperties.getIssuer();

            log.info("Successfully processed public key request: requestId={}", request.getRequestId());

            return PublicKeyResponse.newBuilder()
                    .setRequestId(request.getRequestId())
                    .setStatus(PublicKeyStatus.SUCCESS)
                    .setPublicKey(publicKeyString)
                    .setKeyType("RSA")
                    .setAlgorithm("RS256")
                    .setIssuer(issuer)
                    .setTimestamp(Instant.ofEpochMilli(currentTimestamp))
                    .setErrorMessage(null)
                    .build();

        } catch (Exception e) {
            log.error("Error processing public key request: {}", request.getRequestId(), e);
            return createPublicKeyErrorResponse(request, e.getMessage(), currentTimestamp);
        }
    }

    /**
     * Validiert eine Public Key Anfrage
     */
    public void validatePublicKeyRequest(PublicKeyRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Public key request cannot be null");
        }

        if (request.getRequestId() == null || request.getRequestId().trim().isEmpty()) {
            throw new IllegalArgumentException("Request ID cannot be null or empty");
        }

        log.debug("Public key request validation passed: {}", request.getRequestId());
    }

    /**
     * Erstellt eine Error-Response für Public Key Fehler
     */
    public PublicKeyResponse createPublicKeyErrorResponse(PublicKeyRequest request, String errorMessage) {
        long currentTimestamp = Instant.now().toEpochMilli();
        return createPublicKeyErrorResponse(request, errorMessage, currentTimestamp);
    }

    /**
     * Erstellt eine Error-Response für Public Key Fehler mit Timestamp
     */
    private PublicKeyResponse createPublicKeyErrorResponse(PublicKeyRequest request, String errorMessage, long timestamp) {
        return PublicKeyResponse.newBuilder()
                .setRequestId(request.getRequestId())
                .setStatus(PublicKeyStatus.ERROR)
                .setPublicKey(null)
                .setKeyType(null)
                .setAlgorithm(null)
                .setIssuer(null)
                .setTimestamp(Instant.ofEpochMilli(timestamp))
                .setErrorMessage(errorMessage)
                .build();
    }
}
