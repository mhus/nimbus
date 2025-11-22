package de.mhus.nimbus.universe.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "universe.security")
@Data
public class USecurityProperties {

    public static final String JWT_TOKEN_KEY_ID = "universe:jwt-auth-token";
    private int authExpiresMinutes = 60;
    private int refreshExpiresDays = 30;

    private int refreshMaxTotalDays = 365;


}

