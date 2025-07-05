package de.mhus.nimbus.identity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Konfiguration für JWT-Eigenschaften
 */
@Configuration
@ConfigurationProperties(prefix = "nimbus.identity.jwt")
public class JwtProperties {

    private String privateKeyPath = "classpath:keys/private-key.pem";
    private String publicKeyPath = "classpath:keys/public-key.pem";
    private long expiration = 3600000; // 1 Stunde in Millisekunden
    private String issuer = "nimbus-identity-service";

    public String getPrivateKeyPath() {
        return privateKeyPath;
    }

    public void setPrivateKeyPath(String privateKeyPath) {
        this.privateKeyPath = privateKeyPath;
    }

    public String getPublicKeyPath() {
        return publicKeyPath;
    }

    public void setPublicKeyPath(String publicKeyPath) {
        this.publicKeyPath = publicKeyPath;
    }

    public long getExpiration() {
        return expiration;
    }

    public void setExpiration(long expiration) {
        this.expiration = expiration;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }
}
