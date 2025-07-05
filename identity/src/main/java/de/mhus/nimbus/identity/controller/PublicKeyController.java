package de.mhus.nimbus.identity.controller;

import de.mhus.nimbus.identity.service.PublicKeyService;
import de.mhus.nimbus.shared.avro.PublicKeyRequest;
import de.mhus.nimbus.shared.avro.PublicKeyResponse;
import de.mhus.nimbus.shared.avro.PublicKeyStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

/**
 * REST Controller für Public Key Operationen
 */
@RestController
@RequestMapping("/api/public-key")
public class PublicKeyController {

    private final PublicKeyService publicKeyService;

    public PublicKeyController(PublicKeyService publicKeyService) {
        this.publicKeyService = publicKeyService;
    }

    /**
     * Haupt-Endpunkt für Public Key Abfrage
     */
    @GetMapping
    public ResponseEntity<PublicKeyRestResponse> getPublicKey(@RequestParam(required = false) String requestedBy) {
        try {
            // Erstelle Avro-Request
            PublicKeyRequest request = PublicKeyRequest.newBuilder()
                    .setRequestId(UUID.randomUUID().toString())
                    .setTimestamp(Instant.now())
                    .setRequestedBy(requestedBy != null ? requestedBy : "REST-API")
                    .build();

            // Verarbeite die Anfrage
            PublicKeyResponse response = publicKeyService.processPublicKeyRequest(request);

            // Konvertiere zu REST-Response
            PublicKeyRestResponse restResponse = convertToRestResponse(response);

            return ResponseEntity.ok(restResponse);

        } catch (Exception e) {
            PublicKeyRestResponse errorResponse = new PublicKeyRestResponse(
                    false, null, null, null, null, "Error retrieving public key: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Alternativer Endpunkt für Kompatibilität (behält den ursprünglichen AuthController Endpunkt bei)
     */
    @GetMapping("/info")
    public ResponseEntity<PublicKeyInfoResponse> getPublicKeyInfo() {
        try {
            PublicKeyRequest request = PublicKeyRequest.newBuilder()
                    .setRequestId(UUID.randomUUID().toString())
                    .setTimestamp(Instant.now())
                    .setRequestedBy("REST-API-INFO")
                    .build();

            PublicKeyResponse response = publicKeyService.processPublicKeyRequest(request);

            if (response.getStatus() == PublicKeyStatus.SUCCESS) {
                PublicKeyInfoResponse infoResponse = new PublicKeyInfoResponse(
                        response.getPublicKey(),
                        response.getKeyType(),
                        response.getAlgorithm(),
                        response.getIssuer(),
                        "Public key for JWT token validation",
                        response.getTimestamp().toString()
                );
                return ResponseEntity.ok(infoResponse);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new PublicKeyInfoResponse(null, null, null, null,
                                response.getErrorMessage(), null));
            }

        } catch (Exception e) {
            PublicKeyInfoResponse errorResponse = new PublicKeyInfoResponse(
                    null, null, null, null, "Error retrieving public key info", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Gesundheitscheck für Public Key Service
     */
    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        try {
            // Teste ob der Service funktioniert
            PublicKeyRequest testRequest = PublicKeyRequest.newBuilder()
                    .setRequestId("health-check")
                    .setTimestamp(Instant.now())
                    .setRequestedBy("HEALTH-CHECK")
                    .build();

            PublicKeyResponse response = publicKeyService.processPublicKeyRequest(testRequest);

            boolean healthy = response.getStatus() == PublicKeyStatus.SUCCESS;
            String message = healthy ? "Public Key Service is healthy" : "Public Key Service has issues";

            return ResponseEntity.ok(new HealthResponse(healthy, message, Instant.now().toString()));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new HealthResponse(false, "Public Key Service is unhealthy: " + e.getMessage(),
                            Instant.now().toString()));
        }
    }

    /**
     * Konvertiert PublicKeyResponse zu REST-Response
     */
    private PublicKeyRestResponse convertToRestResponse(PublicKeyResponse response) {
        boolean success = response.getStatus() == PublicKeyStatus.SUCCESS;
        String message = success ? "Public key retrieved successfully" : response.getErrorMessage();

        return new PublicKeyRestResponse(
                success,
                response.getPublicKey(),
                response.getKeyType(),
                response.getAlgorithm(),
                response.getIssuer(),
                message
        );
    }

    // Response DTOs für REST API
    public record PublicKeyRestResponse(
            boolean success,
            String publicKey,
            String keyType,
            String algorithm,
            String issuer,
            String message
    ) {}

    public record PublicKeyInfoResponse(
            String publicKey,
            String keyType,
            String algorithm,
            String issuer,
            String description,
            String timestamp
    ) {}

    public record HealthResponse(
            boolean healthy,
            String message,
            String timestamp
    ) {}
}
