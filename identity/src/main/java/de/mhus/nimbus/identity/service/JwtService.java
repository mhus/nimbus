package de.mhus.nimbus.identity.service;

import de.mhus.nimbus.identity.config.JwtProperties;
import de.mhus.nimbus.identity.util.KeyUtils;
import de.mhus.nimbus.identity.entity.Ace;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service für JWT Token-Management mit asymmetrischer RSA-Signierung
 */
@Service
public class JwtService {

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    private final JwtProperties jwtProperties;
    private final KeyUtils keyUtils;

    private PrivateKey privateKey;
    private PublicKey publicKey;

    public JwtService(JwtProperties jwtProperties, KeyUtils keyUtils) {
        this.jwtProperties = jwtProperties;
        this.keyUtils = keyUtils;
    }

    @PostConstruct
    public void init() {
        try {
            this.privateKey = keyUtils.loadPrivateKey(jwtProperties.getPrivateKeyPath());
            this.publicKey = keyUtils.loadPublicKey(jwtProperties.getPublicKeyPath());
            logger.info("Successfully loaded RSA key pair for JWT signing");
        } catch (Exception e) {
            logger.error("Failed to load RSA keys for JWT signing", e);
            throw new RuntimeException("Failed to initialize JWT service", e);
        }
    }

    /**
     * Generiert einen JWT Token für einen User mit RSA-Signierung
     */
    public String generateToken(Long userId, String username, String email) {
        return generateToken(userId, username, email, null, null);
    }

    /**
     * Generiert einen JWT Token für einen User mit IdentityCharacter-Namen, ACE-Regeln und RSA-Signierung
     */
    public String generateToken(Long userId, String username, String email, List<String> characterNames, List<String> aceRules) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("email", email);
        claims.put("iss", jwtProperties.getIssuer());

        if (characterNames != null && !characterNames.isEmpty()) {
            claims.put("characterNames", characterNames);
        }

        if (aceRules != null && !aceRules.isEmpty()) {
            claims.put("aceRules", aceRules);
        }

        Instant now = Instant.now();
        Instant expiration = now.plus(jwtProperties.getExpiration(), ChronoUnit.MILLIS);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuer(jwtProperties.getIssuer())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    /**
     * Extrahiert Claims aus einem Token mit öffentlichem Schlüssel
     */
    public Claims extractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Extrahiert den Username aus einem Token
     */
    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    /**
     * Extrahiert die User-ID aus einem Token
     */
    public Long extractUserId(String token) {
        return extractClaims(token).get("userId", Long.class);
    }

    /**
     * Extrahiert den Issuer aus einem Token
     */
    public String extractIssuer(String token) {
        return extractClaims(token).getIssuer();
    }

    /**
     * Überprüft ob ein Token abgelaufen ist
     */
    public boolean isTokenExpired(String token) {
        return extractClaims(token).getExpiration().before(new Date());
    }

    /**
     * Validiert einen Token mit öffentlichem Schlüssel
     */
    public boolean validateToken(String token, String username) {
        try {
            final String extractedUsername = extractUsername(token);
            final String extractedIssuer = extractIssuer(token);

            return (extractedUsername.equals(username) &&
                    !isTokenExpired(token) &&
                    jwtProperties.getIssuer().equals(extractedIssuer));
        } catch (Exception e) {
            logger.warn("Token validation failed", e);
            return false;
        }
    }

    /**
     * Berechnet die Ablaufzeit eines neuen Tokens
     */
    public Instant calculateExpirationTime() {
        return Instant.now().plus(jwtProperties.getExpiration(), ChronoUnit.MILLIS);
    }

    /**
     * Gibt den öffentlichen Schlüssel zurück (für externe Validierung)
     */
    public PublicKey getPublicKey() {
        return publicKey;
    }

    /**
     * Gibt den öffentlichen Schlüssel als Base64-String zurück
     */
    public String getPublicKeyAsString() {
        return java.util.Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    /**
     * Extrahiert die ACE-Regeln aus einem Token
     */
    @SuppressWarnings("unchecked")
    public List<String> extractAceRules(String token) {
        return extractClaims(token).get("aceRules", List.class);
    }

    /**
     * Extrahiert die Character-Namen aus einem Token
     */
    @SuppressWarnings("unchecked")
    public List<String> extractCharacterNames(String token) {
        return extractClaims(token).get("characterNames", List.class);
    }
}
