package de.mhus.nimbus.identity.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.List;

/**
 * Utility class for JWT token creation using RSA private key.
 * Used by the Identity Service to sign JWT tokens.
 */
@Component
@Slf4j
public class JwtTokenUtils {

    private static final String ISSUER = "identity-service";
    private static final long TOKEN_VALIDITY_HOURS = 2;
    private RSAPrivateKey privateKey;

    public JwtTokenUtils() {
        loadPrivateKey();
    }

    /**
     * Loads the RSA private key from the classpath.
     */
    private void loadPrivateKey() {
        try {
            ClassPathResource resource = new ClassPathResource("private.key");
            String keyContent = Files.readString(resource.getFile().toPath(), StandardCharsets.UTF_8);

            // Remove header, footer and whitespace
            keyContent = keyContent
                    .replaceAll("-----BEGIN PRIVATE KEY-----", "")
                    .replaceAll("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] keyBytes = Base64.getDecoder().decode(keyContent);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            this.privateKey = (RSAPrivateKey) keyFactory.generatePrivate(spec);

            log.info("Private key loaded successfully for JWT signing");
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            log.error("Failed to load private key: {}", e.getMessage());
            throw new RuntimeException("Could not load private key for JWT signing", e);
        }
    }

    /**
     * Creates a JWT token for the given user ID and roles.
     * @param userId the user identifier
     * @param roles list of user roles
     * @return JWT token string
     */
    public String createToken(String userId, List<String> roles) {
        Algorithm algorithm = Algorithm.RSA256(null, privateKey);
        Date now = new Date();
        Date expiresAt = new Date(now.getTime() + TOKEN_VALIDITY_HOURS * 60 * 60 * 1000);

        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(userId)
                .withIssuedAt(now)
                .withExpiresAt(expiresAt)
                .withClaim("roles", roles)
                .sign(algorithm);
    }

    /**
     * Extracts the expiration time from a JWT token (for response creation).
     * @param token the JWT token
     * @return expiration time as Unix timestamp
     */
    public Long getExpirationTime(String token) {
        return JWT.decode(token).getExpiresAt().getTime() / 1000;
    }

    /**
     * Extracts the issued time from a JWT token (for response creation).
     * @param token the JWT token
     * @return issued time as Unix timestamp
     */
    public Long getIssuedTime(String token) {
        return JWT.decode(token).getIssuedAt().getTime() / 1000;
    }
}
