package de.mhus.nimbus.universe.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

    private String keyId;
    private String secretBase64;
    private int expiresMinutes = 60;

    public String getKeyId() { return keyId; }
    public void setKeyId(String keyId) { this.keyId = keyId; }

    public String getSecretBase64() { return secretBase64; }
    public void setSecretBase64(String secretBase64) { this.secretBase64 = secretBase64; }

    public int getExpiresMinutes() { return expiresMinutes; }
    public void setExpiresMinutes(int expiresMinutes) { this.expiresMinutes = expiresMinutes; }
}

