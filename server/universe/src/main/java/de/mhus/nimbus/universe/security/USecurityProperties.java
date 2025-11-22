package de.mhus.nimbus.universe.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "universe.security")
@Data
public class USecurityProperties {

    public static final String JWT_AUTH_TOKEN_KEY_ID = "universe:auth-token:";
    public static final String JWT_REFRESH_TOKEN_KEY_ID = "universe:refresh-token";
    private int authExpiresMinutes = 60;
    private int refreshExpiresDays = 30;


}

