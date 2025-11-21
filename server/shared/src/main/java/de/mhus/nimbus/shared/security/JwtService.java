package de.mhus.nimbus.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

/**
 * Service for creating and validating JWT tokens.
 * <p>
 * Uses the KeyService to resolve signing and verification keys by key id.
 * Supports both symmetric (HMAC with SecretKey) and asymmetric (RSA/ECDSA with PublicKey) algorithms.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class JwtService {

    private final KeyService keyService;

    /**
     * Creates a JWT token signed with an ECC private key (secp256r1). No symmetric fallback.
     *
     * @param keyId    the key id in format "owner:id" to locate the signing key
     * @param subject  the subject claim (e.g., user id)
     * @param claims   additional claims to include in the token
     * @param expiresAt expiration time (null for no expiration)
     * @return the signed JWT token string
     * @throws IllegalArgumentException if the key cannot be found
     */
    public String createTokenWithSecretKey(@NonNull String keyId,
                                            @NonNull String subject,
                                            Map<String, Object> claims,
                                            Instant expiresAt) {
        PrivateKey key = keyService.findPrivateKey(KeyType.UNIVERSE, keyId)
                .orElseThrow(() -> new IllegalArgumentException("ECC private key not found: " + keyId));
        if (!"EC".equalsIgnoreCase(key.getAlgorithm())) {
            throw new IllegalArgumentException("Private key for keyId " + keyId + " is not ECC (EC). Found: " + key.getAlgorithm());
        }
        var builder = Jwts.builder()
            .subject(subject)
            .issuedAt(Date.from(Instant.now()));
        if (claims != null && !claims.isEmpty()) builder.claims(claims);
        if (expiresAt != null) builder.expiration(Date.from(expiresAt));
        return builder.signWith(key).compact();
    }

    /**
     * Creates a JWT token signed with a secret key from SyncKeyProvider (HMAC algorithm).
     *
     * @param keyId    the key id in format "owner:id" to locate the signing key
     * @param subject  the subject claim (e.g., user id)
     * @param claims   additional claims to include in the token
     * @param expiresAt expiration time (null for no expiration)
     * @return the signed JWT token string
     * @throws IllegalArgumentException if the key cannot be found
     */
    public String createTokenWithSyncKey(@NonNull String keyId,
                                          @NonNull String subject,
                                          Map<String, Object> claims,
                                          Instant expiresAt) {
        SecretKey key = keyService.findSymetricKey(KeyType.UNIVERSE, keyId)
                .orElseThrow(() -> new IllegalArgumentException("Sync key not found: " + keyId));

        var builder = Jwts.builder()
                .subject(subject)
                .issuedAt(Date.from(Instant.now()));

        if (claims != null && !claims.isEmpty()) {
            builder.claims(claims);
        }

        if (expiresAt != null) {
            builder.expiration(Date.from(expiresAt));
        }

        return builder
                .signWith(key)
                .compact();
    }

    /**
     * Validates a JWT token using a secret key (HMAC algorithm).
     *
     * @param token the JWT token string
     * @param keyId the key id in format "owner:id" to locate the verification key
     * @return optional containing the parsed claims if valid; empty if validation fails
     */
    public Optional<Jws<Claims>> validateTokenWithSecretKey(@NonNull String token, @NonNull String keyId) {
        // ECC Validierung über PublicKey
        return keyService.findPublicKey(KeyType.UNIVERSE, keyId)
                .filter(pk -> "EC".equalsIgnoreCase(pk.getAlgorithm()))
                .flatMap(pk -> parseToken(token, pk));
    }

    /**
     * Validates a JWT token using a sync key (HMAC algorithm).
     *
     * @param token the JWT token string
     * @param keyId the key id in format "owner:id" to locate the verification key
     * @return optional containing the parsed claims if valid; empty if validation fails
     */
    public Optional<Jws<Claims>> validateTokenWithSyncKey(@NonNull String token, @NonNull String keyId) {
        return keyService.findSymetricKey(KeyType.UNIVERSE, keyId)
                .flatMap(key -> parseToken(token, key));
    }

    /**
     * Validates a JWT token using a public key (RSA/ECDSA algorithm).
     *
     * @param token the JWT token string
     * @param keyId the key id in format "owner:id" to locate the verification key
     * @return optional containing the parsed claims if valid; empty if validation fails
     */
    public Optional<Jws<Claims>> validateTokenWithPublicKey(@NonNull String token, @NonNull String keyId) {
        return keyService.findPublicKey(KeyType.UNIVERSE, keyId)
                .flatMap(key -> parseToken(token, key));
    }

    /**
     * Parses and validates a JWT token using the provided key.
     *
     * @param token the JWT token string
     * @param key   the verification key (SecretKey or PublicKey)
     * @return optional containing the parsed claims if valid; empty if validation fails
     */
    private Optional<Jws<Claims>> parseToken(String token, Key key) {
        try {
            JwtParser parser;
            if (key instanceof SecretKey secretKey) {
                parser = Jwts.parser()
                        .verifyWith(secretKey)
                        .build();
            } else if (key instanceof PublicKey publicKey) {
                parser = Jwts.parser()
                        .verifyWith(publicKey)
                        .build();
            } else {
                throw new IllegalArgumentException("Unsupported key type: " + key.getClass());
            }
            Jws<Claims> jws = parser.parseSignedClaims(token);
            return Optional.of(jws);
        } catch (SignatureException e) {
            if (log.isDebugEnabled()) {
                log.debug("JWT signature validation failed", e);
            }
            return Optional.empty();
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("JWT parsing failed", e);
            }
            return Optional.empty();
        }
    }
}
